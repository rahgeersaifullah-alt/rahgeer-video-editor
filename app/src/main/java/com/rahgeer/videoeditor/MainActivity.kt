@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Bg)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                        EditorScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val ed = remember { EditorCtx(context, scope) }
    ed.density = density

    ed.picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> ed.onVideosPicked(uris) }
    ed.imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> ed.onImagePicked(uri) }
    ed.musicPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> ed.onMusicPicked(uri) }

    EditorEffects(ed)

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        TopBar(ed)
        PreviewArea(ed)
        TransportBar(ed)
        TimelineArea(ed)
        CutBar(ed)
        ToolPanel(ed)
        ToolBar(ed)
    }

    DrawDialog(ed)
    ExportDialog(ed)
}

@Composable
internal fun EditorEffects(ed: EditorCtx) {
    with(ed) {
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val i = exoPlayer.currentMediaItemIndex
                if (i in clips.indices) {
                    selected = i
                    applyPreview(clips[i])
                    exoPlayer.setPlaybackSpeed(clips[i].speed)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val i = exoPlayer.currentMediaItemIndex
            if (isPlaying && i in clips.indices) {
                val g = startOf(i) + exoPlayer.currentPosition
                playheadMs = g
                val px = (g * pxPerMs()).toInt()
                lastProgrammatic = px
                scrollState.scrollTo(px)
            }
            delay(40)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }.collect { v ->
            if (clips.isNotEmpty() && abs(v - lastProgrammatic) > 2) {
                if (exoPlayer.isPlaying) exoPlayer.pause()
                seekGlobal((v / pxPerMs()).toLong())
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { clips.toList() }.collectLatest { snap ->
            delay(350)
            if (restoring) {
                restoring = false
            } else if (snap != lastSnapshot) {
                undoStack.add(lastSnapshot)
                if (undoStack.size > 30) undoStack.removeAt(0)
                redoStack.clear()
                lastSnapshot = snap
            }
        }
    }

    LaunchedEffect(exportState) {
        val t = transformer
        while (exportState == 1 && t != null) {
            val holder = ProgressHolder()
            if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                exportProgress = holder.progress.toFloat()
            }
            delay(300)
        }
    }
    }
}
