package com.rahgeer.videoeditor

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        EditorScreen()
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var range by remember { mutableStateOf(0f..1f) }
    var exporting by remember { mutableStateOf(false) }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && player.duration > 0) {
                    durationMs = player.duration
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            durationMs = 0L
            range = 0f..1f
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }
    }

    fun exportVideo() {
        val uri = videoUri ?: return
        if (durationMs <= 0L) return
        val startMs = (range.start * durationMs).toLong()
        val endMs = (range.endInclusive * durationMs).toLong()
        if (endMs - startMs < 500) {
            Toast.makeText(context, "منتخب حصہ بہت چھوٹا ہے", Toast.LENGTH_SHORT).show()
            return
        }

        val clipped = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val outFile = File(dir, "rahgeer_${System.currentTimeMillis()}.mp4")

        exporting = true
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    exporting = false
                    Toast.makeText(context, "ایکسپورٹ مکمل ہو گیا:\n${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    exporting = false
                    Toast.makeText(context, "ایکسپورٹ میں مسئلہ پیش آیا", Toast.LENGTH_LONG).show()
                }
            })
            .build()
        transformer.start(EditedMediaItem.Builder(clipped).build(), outFile.absolutePath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("راہگیر ویڈیو ایڈیٹر", style = MaterialTheme.typography.headlineMedium)

        Button(
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ویڈیو چنیں")
        }

        if (videoUri != null) {
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("آغاز: ${formatTime((range.start * durationMs).toLong())}")
                Text("اختتام: ${formatTime((range.endInclusive * durationMs).toLong())}")
            }

            RangeSlider(
                value = range,
                onValueChange = {
                    range = it
                    player.seekTo((it.start * durationMs).toLong())
                },
                enabled = durationMs > 0L
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { player.play() }) { Text("چلائیں") }
                OutlinedButton(onClick = { player.pause() }) { Text("روکیں") }
            }

            Button(
                onClick = { exportVideo() },
                enabled = !exporting && durationMs > 0L,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (exporting) "ایکسپورٹ ہو رہا ہے…" else "ایکسپورٹ کریں")
            }
        }
    }
}
