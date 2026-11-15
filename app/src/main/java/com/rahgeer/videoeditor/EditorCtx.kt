@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class EditorCtx(val context: Context, val scope: CoroutineScope) {
    var density: Float = 1f

    val clips = mutableStateListOf<Clip>()
    var selected by mutableIntStateOf(-1)
    var loading by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var playheadMs by mutableLongStateOf(0L)
    var dpPerSec by mutableFloatStateOf(60f)
    var lastProgrammatic by mutableIntStateOf(0)
    val scrollState = ScrollState(0)

    var activeTool by mutableStateOf("")
    var resolution by mutableStateOf("1080p")
    var fps by mutableStateOf("30")
    var quality by mutableStateOf("اعلیٰ")
    var aspectIdx by mutableIntStateOf(0)
    var transitionIdx by mutableIntStateOf(0)
    var muteAll by mutableStateOf(false)
    var watermark by mutableStateOf(false)
    var musicUri by mutableStateOf<Uri?>(null)
    var lastExportUri by mutableStateOf<Uri?>(null)
    var lastExportIsAudio by mutableStateOf(false)

    var exportState by mutableIntStateOf(0)
    var exportProgress by mutableFloatStateOf(0f)
    var exportMessage by mutableStateOf("")
    var transformer by mutableStateOf<Transformer?>(null)

    var fontsVersion by mutableIntStateOf(0)
    var fontMsg by mutableStateOf("")
    var downloadingFonts by mutableStateOf(false)

    var drawing by mutableStateOf(false)
    val strokes = mutableStateListOf<DrawStroke>()
    var strokePoints by mutableStateOf<List<Offset>>(emptyList())
    var drawColorIdx by mutableIntStateOf(2)
    var drawWidthIdx by mutableIntStateOf(1)
    var boxSize by mutableStateOf(IntSize.Zero)

    val undoStack = mutableStateListOf<List<Clip>>()
    val redoStack = mutableStateListOf<List<Clip>>()
    var lastSnapshot by mutableStateOf<List<Clip>>(emptyList())
    var restoring by mutableStateOf(false)

    lateinit var picker: ActivityResultLauncher<PickVisualMediaRequest>
    lateinit var imagePicker: ActivityResultLauncher<PickVisualMediaRequest>
    lateinit var musicPicker: ActivityResultLauncher<String>

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun pxPerMs(): Float = dpPerSec * density / 1000f

    fun startOf(index: Int): Long {
        var acc = 0L
        for (i in 0 until minOf(index, clips.size)) acc += clips[i].trimmedMs
        return acc
    }

    fun totalTrimmed(): Long {
        var acc = 0L
        for (c in clips) acc += c.trimmedMs
        return acc
    }

    fun locate(g: Long): Pair<Int, Long> {
        var acc = 0L
        for (i in clips.indices) {
            val d = clips[i].trimmedMs
            if (g < acc + d || i == clips.lastIndex) {
                return Pair(i, (g - acc).coerceIn(0L, d))
            }
            acc += d
        }
        return Pair(0, 0L)
    }

    fun applyPreview(c: Clip?) {
        try {
            val effects: List<Effect> = if (c == null) {
                emptyList()
            } else {
                geometryEffects(c) + colorEffects(c)
            }
            exoPlayer.setVideoEffects(effects)
        } catch (e: Exception) {
            // پریویو کا اثر نہ لگ سکا، ایکسپورٹ پر کوئی اثر نہیں
        }
    }

    fun syncScroll() {
        scope.launch {
            withFrameNanos { }
            val px = (playheadMs * pxPerMs()).toInt()
            lastProgrammatic = px
            scrollState.scrollTo(px)
        }
    }

    fun seekGlobal(g: Long) {
        if (clips.isEmpty()) return
        val gg = g.coerceIn(0L, totalTrimmed())
        val (i, local) = locate(gg)
        exoPlayer.seekTo(i, local)
        playheadMs = gg
        if (i != selected) {
            selected = i
            applyPreview(clips[i])
            exoPlayer.setPlaybackSpeed(clips[i].speed)
        }
    }

    fun rebuild(globalMs: Long) {
        if (clips.isEmpty()) {
            exoPlayer.clearMediaItems()
            selected = -1
            playheadMs = 0L
            syncScroll()
            return
        }
        val g = globalMs.coerceIn(0L, totalTrimmed())
        val (i, local) = locate(g)
        exoPlayer.setMediaItems(clips.map { mediaItemFor(it) }, i, local)
        exoPlayer.prepare()
        selected = i
        applyPreview(clips[i])
        exoPlayer.setPlaybackSpeed(clips[i].speed)
        playheadMs = g
        syncScroll()
    }

    fun updateClip(c: Clip) {
        if (selected in clips.indices) {
            clips[selected] = c
            applyPreview(c)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) {
            toast("واپس کرنے کے لیے کچھ نہیں")
            return
        }
        val prev = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(lastSnapshot)
        restoring = true
        lastSnapshot = prev
        clips.clear()
        clips.addAll(prev)
        rebuild(playheadMs)
    }

    fun redo() {
        if (redoStack.isEmpty()) {
            toast("آگے کرنے کے لیے کچھ نہیں")
            return
        }
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(lastSnapshot)
        restoring = true
        lastSnapshot = next
        clips.clear()
        clips.addAll(next)
        rebuild(playheadMs)
    }

    fun pickVideos() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
}
