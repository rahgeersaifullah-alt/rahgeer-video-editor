@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun EditorCtx.deleteSelected() {
    if (selected !in clips.indices) {
        toast("پہلے ویڈیو چنیں")
        return
    }
    val start = startOf(selected)
    clips.removeAt(selected)
    rebuild(start)
}

internal fun EditorCtx.duplicateSelected() {
    if (selected !in clips.indices) {
        toast("پہلے ویڈیو چنیں")
        return
    }
    clips.add(selected + 1, clips[selected])
    rebuild(startOf(selected + 1))
}

internal fun EditorCtx.moveSelected(delta: Int) {
    val target = selected + delta
    if (selected !in clips.indices || target !in clips.indices) {
        toast("کلپ مزید اس طرف نہیں جا سکتا")
        return
    }
    val c = clips.removeAt(selected)
    clips.add(target, c)
    rebuild(startOf(target))
}

internal fun EditorCtx.splitAtPlayhead() {
    if (clips.isEmpty()) {
        toast("پہلے ویڈیو چنیں")
        return
    }
    val (i, local) = locate(playheadMs)
    val c = clips[i]
    if (local < 200L || local > c.trimmedMs - 200L) {
        toast("سفید لکیر کو کلپ کے اندر لائیں (کناروں سے ذرا ہٹ کر)")
        return
    }
    val frac = c.startFrac + local.toFloat() / c.durationMs
    clips[i] = c.copy(endFrac = frac)
    clips.add(i + 1, c.copy(startFrac = frac))
    rebuild(playheadMs)
    toast("کلپ دو حصوں میں تقسیم ہو گیا")
}

internal fun EditorCtx.trimAtPlayhead(left: Boolean) {
    if (clips.isEmpty()) {
        toast("پہلے ویڈیو چنیں")
        return
    }
    val (i, local) = locate(playheadMs)
    val c = clips[i]
    if (local < 200L || local > c.trimmedMs - 200L) {
        toast("سفید لکیر کو کلپ کے اندر لائیں (کناروں سے ذرا ہٹ کر)")
        return
    }
    val frac = c.startFrac + local.toFloat() / c.durationMs
    if (left) {
        clips[i] = c.copy(startFrac = frac)
        rebuild(startOf(i))
    } else {
        clips[i] = c.copy(endFrac = frac)
        rebuild(startOf(i) + clips[i].trimmedMs - 1L)
    }
}

internal fun EditorCtx.trimEdge(index: Int, dragPx: Float, isStart: Boolean) {
    if (index !in clips.indices) return
    val c = clips[index]
    val df = (dragPx / pxPerMs()) / c.durationMs
    val minF = 300f / c.durationMs
    clips[index] = if (isStart) {
        val upper = (c.endFrac - minF).coerceAtLeast(0f)
        c.copy(startFrac = (c.startFrac + df).coerceIn(0f, upper))
    } else {
        val lower = (c.startFrac + minF).coerceAtMost(1f)
        c.copy(endFrac = (c.endFrac + df).coerceIn(lower, 1f))
    }
}

internal fun EditorCtx.zoom(factor: Float) {
    dpPerSec = (dpPerSec * factor).coerceIn(15f, 240f)
    syncScroll()
}

internal fun EditorCtx.downloadFonts() {
    if (downloadingFonts) return
    downloadingFonts = true
    scope.launch {
        var ok = 0
        fontDefs.forEachIndexed { i, d ->
            fontMsg = "ڈاؤن لوڈ ${i + 1}/${fontDefs.size}: ${d.name}"
            val done = withContext(Dispatchers.IO) {
                getTypeface(context, i + 1) != null || downloadFont(context, d)
            }
            if (done) ok++
            fontsVersion++
        }
        fontMsg = if (ok == fontDefs.size) {
            "✅ تمام فونٹس تیار ہیں"
        } else {
            "$ok/${fontDefs.size} فونٹس ملے، انٹرنیٹ چیک کریں"
        }
        downloadingFonts = false
    }
}

internal fun EditorCtx.commitDrawing() {
    val c = clips.getOrNull(selected)
    if (c != null && strokes.isNotEmpty() && boxSize.width > 0 && boxSize.height > 0) {
        val (ew, eh) = effectiveSize(c)
        if (ew > 0 && eh > 0 && ew.toLong() * eh <= 4096L * 2304L) {
            val bmp = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
            val cv = android.graphics.Canvas(bmp)
            val sx = ew.toFloat() / boxSize.width
            val sy = eh.toFloat() / boxSize.height
            val paint = android.graphics.Paint()
            paint.isAntiAlias = true
            paint.strokeCap = android.graphics.Paint.Cap.ROUND
            strokes.forEach { s ->
                paint.color = s.color
                paint.strokeWidth = s.width * sx
                if (s.points.size == 1) {
                    cv.drawPoint(s.points[0].x * sx, s.points[0].y * sy, paint)
                }
                for (i in 1 until s.points.size) {
                    cv.drawLine(
                        s.points[i - 1].x * sx,
                        s.points[i - 1].y * sy,
                        s.points[i].x * sx,
                        s.points[i].y * sy,
                        paint
                    )
                }
            }
            clips[selected] = c.copy(drawing = bmp)
            toast("ڈرائنگ محفوظ ہو گئی، ایکسپورٹ میں نظر آئے گی")
        }
    }
    drawing = false
}

internal fun EditorCtx.shareTo(pkg: String?) {
    val uri = lastExportUri
    if (uri == null) {
        toast("پہلے ویڈیو ایکسپورٹ کریں")
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (lastExportIsAudio) "audio/mp4" else "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (pkg != null) setPackage(pkg)
    }
    try {
        if (pkg == null) {
            context.startActivity(Intent.createChooser(intent, "شیئر کریں"))
        } else {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        toast("یہ ایپ فون میں نہیں ملی")
    }
}

internal fun EditorCtx.startExport(audioOnly: Boolean = false) {
    if (clips.isEmpty()) {
        toast("پہلے ویڈیو چنیں")
        return
    }
    exoPlayer.pause()

    val (outW, outH) = outputSize(clips[0], resolution, aspectIdx)
    val presentation = Presentation.createForWidthAndHeight(
        outW,
        outH,
        Presentation.LAYOUT_SCALE_TO_FIT
    )

    val items = clips.map { c ->
        val builder = EditedMediaItem.Builder(mediaItemFor(c))
        if (audioOnly) {
            builder.setRemoveVideo(true)
            builder.setEffects(Effects(audioProcessors(c), emptyList()))
        } else {
            if (muteAll) builder.setRemoveAudio(true)
            builder.setEffects(
                Effects(
                    audioProcessors(c),
                    exportVideoEffects(context, c, presentation, watermark, transitionIdx)
                )
            )
        }
        builder.build()
    }

    val sequences = mutableListOf(EditedMediaItemSequence(items))
    val music = musicUri
    if (!audioOnly && music != null) {
        val totalOut = clips.sumOf { it.outDur }
        val musicDur = audioDurationMs(context, music)
        val endMs = if (musicDur > 0) minOf(musicDur, totalOut) else totalOut
        val musicItem = MediaItem.Builder()
            .setUri(music)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        sequences.add(EditedMediaItemSequence(listOf(EditedMediaItem.Builder(musicItem).build())))
    }
    val project = Composition.Builder(sequences).build()

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
    val ext = if (audioOnly) "m4a" else "mp4"
    val outFile = File(dir, "rahgeer_${System.currentTimeMillis()}.$ext")

    exportProgress = 0f
    exportMessage = ""
    lastExportIsAudio = audioOnly

    val tb = Transformer.Builder(context)
    if (audioOnly) tb.setAudioMimeType(MimeTypes.AUDIO_AAC)
    val t = tb
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                exportProgress = 100f
                exportState = 2
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        saveToGallery(context, outFile, audioOnly)
                    }
                    lastExportUri = saved
                    exportMessage = if (saved != null) {
                        if (audioOnly) "آڈیو محفوظ ہو گئی (Music/Rahgeer)"
                        else "ویڈیو گیلری میں محفوظ ہو گئی (Movies/Rahgeer)"
                    } else {
                        "فائل اس جگہ محفوظ ہوئی:\n${outFile.absolutePath}"
                    }
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                exportState = 3
                exportMessage = "ایکسپورٹ میں مسئلہ پیش آیا"
            }
        })
        .build()
    transformer = t
    exportState = 1
    t.start(project, outFile.absolutePath)
}

internal fun EditorCtx.onToolClick(tool: Tool) {
    val needsClip = selected in clips.indices
    when (tool.label) {
        "کاٹیں" -> toast(tool.hint)
        "تقسیم" -> splitAtPlayhead()
        "بائیں حذف" -> trimAtPlayhead(true)
        "دائیں حذف" -> trimAtPlayhead(false)
        "حذف" -> deleteSelected()
        "نقل بنائیں" -> duplicateSelected()
        "پہلے لائیں" -> moveSelected(-1)
        "بعد لے جائیں" -> moveSelected(1)
        "کراپ", "گھمائیں", "رفتار", "فلٹرز", "ایفیکٹس", "رنگ درست کریں",
        "روشنی / کنٹراسٹ", "فریمز اور بارڈر", "ٹیکسٹ شامل کریں", "ٹیکسٹ رنگ",
        "اردو فونٹس", "ٹیکسٹ ٹیمپلیٹس", "ٹیکسٹ اینیمیشن", "اسٹیکرز", "ایموجی",
        "شکلیں", "تصویر اوورلے", "وائس چینجر", "آواز کی سطح", "ایکولائزر",
        "شور ہٹائیں", "فیڈ اِن / آؤٹ", "موشن ایفیکٹ", "فلم گرین",
        "شفافیت / بلینڈ" -> {
            if (needsClip) activeTool = tool.label else toast("پہلے ویڈیو چنیں")
        }
        "ڈرائنگ" -> {
            if (needsClip) {
                strokes.clear()
                strokePoints = emptyList()
                drawing = true
            } else {
                toast("پہلے ویڈیو چنیں")
            }
        }
        "سائز / ریشو", "موسیقی", "ٹرانزیشنز", "ایکسپورٹ سیٹنگ" -> activeTool = tool.label
        "واٹر مارک" -> {
            watermark = !watermark
            toast(if (watermark) "واٹر مارک آن" else "واٹر مارک بند")
        }
        "خاموش کریں" -> {
            muteAll = !muteAll
            toast(if (muteAll) "ویڈیوز کی آواز بند" else "ویڈیوز کی آواز بحال")
        }
        "آڈیو الگ کریں" -> if (exportState == 0) startExport(true)
        "واٹس ایپ" -> shareTo("com.whatsapp")
        "یوٹیوب" -> shareTo("com.google.android.youtube")
        "ٹک ٹاک" -> shareTo("com.zhiliaoapp.musically")
        "فیس بک" -> shareTo("com.facebook.katana")
        "انسٹاگرام" -> shareTo("com.instagram.android")
        "مزید شیئر" -> shareTo(null)
        else -> toast("${tool.label}: یہ سہولت جلد شامل ہو گی")
    }
}

internal fun EditorCtx.isActive(tool: Tool): Boolean = when (tool.label) {
    "واٹر مارک" -> watermark
    "خاموش کریں" -> muteAll
    else -> activeTool == tool.label
}

internal fun EditorCtx.onVideosPicked(uris: List<Uri>) {
    if (uris.isEmpty()) return
    scope.launch {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            uris.mapNotNull { loadClip(context, it) }
        }
        clips.addAll(loaded)
        rebuild(playheadMs)
        if (loaded.size < uris.size) toast("کچھ ویڈیوز لوڈ نہیں ہو سکیں")
        loading = false
    }
}

internal fun EditorCtx.onImagePicked(uri: Uri?) {
    if (uri != null && selected in clips.indices) {
        clips[selected] = clips[selected].copy(overlayUri = uri)
    }
}

internal fun EditorCtx.onMusicPicked(uri: Uri?) {
    if (uri != null) {
        musicUri = uri
        toast("موسیقی منتخب ہو گئی")
    }
}

