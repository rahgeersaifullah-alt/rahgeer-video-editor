@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

internal class TimeRef {
    private var first = Long.MIN_VALUE

    fun local(t: Long): Long {
        if (first == Long.MIN_VALUE) first = t
        return t - first
    }
}

internal class AudioFxProcessor(
    private val gain: Float,
    private val eqMode: Int,
    private val gateLevel: Int,
    private val fadeInMs: Int,
    private val fadeOutMs: Int,
    private val totalMs: Long
) : BaseAudioProcessor() {
    private var channels = 2
    private var sampleRate = 44100
    private var lpLow = FloatArray(2)
    private var lpHigh = FloatArray(2)
    private var env = 0f
    private var frames = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        lpLow = FloatArray(channels)
        lpHigh = FloatArray(channels)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        val aLow = 1f - exp(-2.0 * Math.PI * 200.0 / sampleRate).toFloat()
        val aHigh = 1f - exp(-2.0 * Math.PI * 3000.0 / sampleRate).toFloat()
        val envA = 1f - exp(-1.0 / (sampleRate * 0.02)).toFloat()
        val gate = when (gateLevel) {
            1 -> 0.01f
            2 -> 0.02f
            3 -> 0.04f
            else -> 0f
        } * 32768f
        val samples = FloatArray(channels)

        while (inputBuffer.remaining() >= 2 * channels) {
            val tMs = frames * 1000f / sampleRate
            var fade = 1f
            if (fadeInMs > 0 && tMs < fadeInMs) {
                fade = tMs / fadeInMs
            }
            if (fadeOutMs > 0) {
                val left = totalMs - tMs
                if (left < fadeOutMs) {
                    val f = (left / fadeOutMs).coerceIn(0f, 1f)
                    if (f < fade) fade = f
                }
            }
            var peak = 0f
            for (ch in 0 until channels) {
                val x0 = inputBuffer.short.toFloat()
                var x = x0
                if (eqMode == 1 || eqMode == 3) {
                    lpLow[ch] += aLow * (x0 - lpLow[ch])
                    x += (if (eqMode == 3) 0.7f else 1.2f) * lpLow[ch]
                }
                if (eqMode == 2 || eqMode == 3) {
                    lpHigh[ch] += aHigh * (x0 - lpHigh[ch])
                    x += (if (eqMode == 3) 0.6f else 1.0f) * (x0 - lpHigh[ch])
                }
                samples[ch] = x
                val a = if (x < 0f) -x else x
                if (a > peak) peak = a
            }
            env += envA * (peak - env)
            var g = gain * fade
            if (gate > 0f && env < gate) g *= 0.15f
            for (ch in 0 until channels) {
                val v = (samples[ch] * g).toInt().coerceIn(-32768, 32767)
                out.putShort(v.toShort())
            }
            frames++
        }
        while (inputBuffer.hasRemaining()) {
            inputBuffer.get()
        }
        out.flip()
    }
}

internal fun loadClip(context: Context, uri: Uri): Clip? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0
        val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        if (rot == 90 || rot == 270) {
            val t = w
            w = h
            h = t
        }
        val thumbs = ArrayList<ImageBitmap>()
        for (k in 0 until 5) {
            val tUs = dur * 1000L * (2 * k + 1) / 10L
            val fr = r.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
            val tw = 160
            val th = (tw * fr.height / fr.width.toFloat()).toInt().coerceAtLeast(1)
            thumbs.add(Bitmap.createScaledBitmap(fr, tw, th, true).asImageBitmap())
        }
        if (dur <= 0L) null else Clip(uri, dur, w, h, thumbs)
    } catch (e: Exception) {
        null
    } finally {
        r.release()
    }
}

internal fun audioDurationMs(context: Context, uri: Uri): Long {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    } finally {
        r.release()
    }
}

internal fun saveToGallery(context: Context, file: File, audio: Boolean): Uri? {
    if (Build.VERSION.SDK_INT < 29) return null
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (audio) "audio/mp4" else "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                if (audio) "Music/Rahgeer" else "Movies/Rahgeer"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (audio) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri
    } catch (e: Exception) {
        null
    }
}

internal fun loadBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    return try {
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val s = maxSide.toFloat() / maxOf(original.width, original.height)
        Bitmap.createScaledBitmap(
            original,
            (original.width * s).toInt().coerceAtLeast(1),
            (original.height * s).toInt().coerceAtLeast(1),
            true
        )
    } catch (e: Exception) {
        null
    }
}

internal fun mediaItemFor(c: Clip): MediaItem {
    return MediaItem.Builder()
        .setUri(c.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs((c.startFrac * c.durationMs).toLong())
                .setEndPositionMs((c.endFrac * c.durationMs).toLong())
                .build()
        )
        .build()
}

internal fun effectiveSize(c: Clip): Pair<Int, Int> {
    var w = c.width
    var h = c.height
    val ratio = cropRatios[c.cropIdx]
    if (ratio != null && w > 0 && h > 0) {
        val src = w.toFloat() / h
        if (ratio < src) {
            w = (h * ratio).toInt()
        } else if (ratio > src) {
            h = (w / ratio).toInt()
        }
    }
    if (c.rotation == 90 || c.rotation == 270) {
        val t = w
        w = h
        h = t
    }
    return Pair(w, h)
}

internal fun geometryEffects(c: Clip): List<Effect> {
    val list = mutableListOf<Effect>()
    val ratio = cropRatios[c.cropIdx]
    if (ratio != null && c.width > 0 && c.height > 0) {
        val src = c.width.toFloat() / c.height
        if (ratio < src) {
            val f = ratio / src
            list.add(Crop(-f, f, -1f, 1f))
        } else if (ratio > src) {
            val f = src / ratio
            list.add(Crop(-1f, 1f, -f, f))
        }
    }
    if (c.rotation != 0) {
        list.add(
            ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(c.rotation.toFloat())
                .build()
        )
    }
    return list
}

internal fun colorEffects(c: Clip): List<Effect> {
    val list = mutableListOf<Effect>()
    when (c.filterIdx) {
        1 -> list.add(RgbFilter.createGrayscaleFilter())
        2 -> list.add(RgbAdjustment.Builder().setRedScale(1.15f).setBlueScale(0.88f).build())
        3 -> list.add(RgbAdjustment.Builder().setBlueScale(1.15f).setRedScale(0.88f).build())
        4 -> {
            list.add(HslAdjustment.Builder().adjustSaturation(40f).build())
            list.add(Contrast(0.15f))
        }
        5 -> {
            list.add(Brightness(0.08f))
            list.add(Contrast(-0.2f))
            list.add(HslAdjustment.Builder().adjustSaturation(-25f).build())
        }
        6 -> list.add(RgbFilter.createInvertedFilter())
    }
    when (c.lookIdx) {
        1 -> {
            list.add(Contrast(0.2f))
            list.add(HslAdjustment.Builder().adjustSaturation(-15f).build())
            list.add(RgbAdjustment.Builder().setRedScale(1.05f).setBlueScale(1.08f).build())
        }
        2 -> {
            list.add(HslAdjustment.Builder().adjustSaturation(-45f).build())
            list.add(
                RgbAdjustment.Builder()
                    .setRedScale(1.12f)
                    .setGreenScale(1.02f)
                    .setBlueScale(0.85f)
                    .build()
            )
            list.add(Brightness(0.05f))
        }
        3 -> {
            list.add(Contrast(0.35f))
            list.add(HslAdjustment.Builder().adjustSaturation(30f).build())
        }
        4 -> {
            list.add(Brightness(0.12f))
            list.add(Contrast(-0.15f))
            list.add(HslAdjustment.Builder().adjustSaturation(15f).build())
        }
    }
    if (c.brightness != 0f) list.add(Brightness(c.brightness))
    if (c.contrast != 0f) list.add(Contrast(c.contrast))
    if (c.saturation != 0f) list.add(HslAdjustment.Builder().adjustSaturation(c.saturation).build())
    return list
}

internal fun motionEffect(mode: Int, durMs: Long): MatrixTransformation {
    val ref = TimeRef()
    return MatrixTransformation { presentationTimeUs ->
        val t = ref.local(presentationTimeUs) / 1000f
        val prog = if (durMs > 0) (t / durMs).coerceIn(0f, 1f) else 0f
        val m = Matrix()
        when (mode) {
            1 -> {
                val s = 1f + 0.2f * prog
                m.postScale(s, s)
            }
            2 -> {
                val s = 1.2f - 0.2f * prog
                m.postScale(s, s)
            }
            3 -> {
                m.postScale(1.08f, 1.08f)
                m.postTranslate(0.02f * sin(t / 90f), 0.02f * cos(t / 110f))
            }
            4 -> {
                val s = 1f + 0.05f * sin(t / 127f)
                m.postScale(s, s)
            }
        }
        m
    }
}

internal fun fadeMatrixEffect(
    fadeInMs: Int,
    fadeOutMs: Int,
    white: Boolean,
    opacity: Float,
    durMs: Long
): RgbMatrix {
    val ref = TimeRef()
    return object : RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
            val tMs = ref.local(presentationTimeUs) / 1000f
            var f = opacity
            if (fadeInMs > 0 && tMs < fadeInMs) {
                f *= (tMs / fadeInMs).coerceIn(0f, 1f)
            }
            if (fadeOutMs > 0) {
                val left = durMs - tMs
                if (left < fadeOutMs) {
                    f *= (left / fadeOutMs).coerceIn(0f, 1f)
                }
            }
            val off = if (white) 1f - f else 0f
            return floatArrayOf(
                f, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, f, 0f,
                off, off, off, 1f
            )
        }
    }
}

internal fun makeSpan(text: String, colorInt: Int, sizePx: Int, tf: Typeface?): SpannableString {
    val s = SpannableString(text)
    s.setSpan(ForegroundColorSpan(colorInt), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    s.setSpan(AbsoluteSizeSpan(sizePx), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    if (tf != null) {
        s.setSpan(FontSpan(tf), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else {
        s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return s
}

internal fun makeText(
    text: String,
    colorInt: Int,
    sizePx: Int,
    x: Float,
    y: Float,
    tf: Typeface?
): TextOverlay {
    val settings = OverlaySettings.Builder()
        .setBackgroundFrameAnchor(x, y)
        .setOverlayFrameAnchor(0f, 0f)
        .build()
    return TextOverlay.createStaticTextOverlay(makeSpan(text, colorInt, sizePx, tf), settings)
}

internal fun animatedText(
    text: String,
    colorInt: Int,
    sizePx: Int,
    baseY: Float,
    mode: Int,
    tf: Typeface?
): TextOverlay {
    val span = makeSpan(text, colorInt, sizePx, tf)
    val ref = TimeRef()
    return object : TextOverlay() {
        override fun getText(presentationTimeUs: Long): SpannableString = span

        override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
            val t = ref.local(presentationTimeUs) / 1_000_000f
            val p = t.coerceIn(0f, 1f)
            val b = OverlaySettings.Builder().setOverlayFrameAnchor(0f, 0f)
            when (mode) {
                1 -> b.setBackgroundFrameAnchor(0f, baseY).setAlphaScale(p)
                2 -> b.setBackgroundFrameAnchor(0f, baseY - 0.4f * (1f - p)).setAlphaScale(p)
                3 -> b.setBackgroundFrameAnchor(0f, baseY)
                    .setAlphaScale(if ((t * 2f).toInt() % 2 == 0) 1f else 0.25f)
                else -> {
                    val s = if (p < 0.6f) p / 0.6f * 1.2f else 1.2f - (p - 0.6f) / 0.4f * 0.2f
                    b.setBackgroundFrameAnchor(0f, baseY).setScale(s, s)
                }
            }
            return b.build()
        }
    }
}

internal fun overlayEffects(context: Context, c: Clip, watermark: Boolean): List<Effect> {
    val overlays = ArrayList<TextureOverlay>()
    val (ew, eh) = effectiveSize(c)
    val base = minOf(ew, eh).coerceAtLeast(240)
    val tf = getTypeface(context, c.fontIdx)

    if (c.frameIdx > 0 && ew > 0 && eh > 0 && ew.toLong() * eh <= 4096L * 2304L) {
        val bmp = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = base * 0.04f
        paint.color = frameColors[c.frameIdx - 1]
        val half = paint.strokeWidth / 2f
        canvas.drawRect(half, half, ew - half, eh - half, paint)
        overlays.add(BitmapOverlay.createStaticBitmapOverlay(bmp))
    }
    val drawn = c.drawing
    if (drawn != null) {
        overlays.add(BitmapOverlay.createStaticBitmapOverlay(drawn))
    }
    if (c.grain && ew > 0 && eh > 0 && ew.toLong() * eh <= 2_400_000L) {
        val frames = List(3) { k ->
            val px = IntArray(ew * eh)
            val rnd = Random(k + 7)
            for (i in px.indices) {
                val g = rnd.nextInt(256)
                px[i] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
            val b = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
            b.setPixels(px, 0, ew, 0, 0, ew, eh)
            b
        }
        val grainSettings = OverlaySettings.Builder().setAlphaScale(0.14f).build()
        overlays.add(object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap =
                frames[((presentationTimeUs / 66_666L) % 3L).toInt().coerceAtLeast(0)]

            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = grainSettings
        })
    }
    if (c.text.isNotBlank()) {
        if (c.textAnimIdx == 0) {
            overlays.add(
                makeText(c.text, colorValues[c.textColorIdx], base / 12, 0f, textPosY[c.textPos], tf)
            )
        } else {
            overlays.add(
                animatedText(c.text, colorValues[c.textColorIdx], base / 12, textPosY[c.textPos], c.textAnimIdx, tf)
            )
        }
    }
    if (c.sticker.isNotEmpty()) {
        overlays.add(makeText(c.sticker, 0xFFFFFFFF.toInt(), base / 5, cornerX[c.stickerPos], cornerY[c.stickerPos], null))
    }
    if (c.shape.isNotEmpty()) {
        overlays.add(makeText(c.shape, colorValues[c.shapeColorIdx], base / 5, cornerX[c.shapePos], cornerY[c.shapePos], null))
    }
    val imageUri = c.overlayUri
    if (imageUri != null) {
        val bmp = loadBitmap(context, imageUri, base / 3)
        if (bmp != null) {
            val settings = OverlaySettings.Builder()
                .setBackgroundFrameAnchor(cornerX[c.imagePos], cornerY[c.imagePos])
                .setOverlayFrameAnchor(0f, 0f)
                .build()
            overlays.add(BitmapOverlay.createStaticBitmapOverlay(bmp, settings))
        }
    }
    if (watermark) {
        overlays.add(
            makeText("راہگیر ویڈیو ایڈیٹر", 0xCCFFFFFF.toInt(), (base / 24).coerceAtLeast(14), 0.5f, -0.92f, tf)
        )
    }
    return if (overlays.isEmpty()) {
        emptyList()
    } else {
        listOf(OverlayEffect(ImmutableList.copyOf(overlays)))
    }
}

internal fun exportVideoEffects(
    context: Context,
    c: Clip,
    presentation: Presentation,
    watermark: Boolean,
    transitionIdx: Int
): List<Effect> {
    val list = mutableListOf<Effect>()
    list.addAll(geometryEffects(c))
    if (c.motionIdx != 0) list.add(motionEffect(c.motionIdx, c.trimmedMs))
    list.addAll(colorEffects(c))
    list.addAll(overlayEffects(context, c, watermark))

    val trMs = when (transitionIdx) {
        1, 2 -> 600
        3 -> 300
        else -> 0
    }
    val inMs = maxOf(fadeMs[c.fadeInIdx], trMs)
    val outMs = maxOf(fadeMs[c.fadeOutIdx], trMs)
    val white = c.fadeWhite || transitionIdx == 2
    if (inMs > 0 || outMs > 0 || c.opacity < 1f) {
        list.add(fadeMatrixEffect(inMs, outMs, white, c.opacity, c.trimmedMs))
    }

    if (c.speed != 1f) list.add(SpeedChangeEffect(c.speed))
    list.add(presentation)
    return list
}

internal fun audioProcessors(c: Clip): List<AudioProcessor> {
    val list = mutableListOf<AudioProcessor>()
    if (c.volume != 1f || c.eqIdx != 0 || c.gateIdx != 0 || c.fadeInIdx != 0 || c.fadeOutIdx != 0) {
        list.add(
            AudioFxProcessor(
                c.volume,
                c.eqIdx,
                c.gateIdx,
                fadeMs[c.fadeInIdx],
                fadeMs[c.fadeOutIdx],
                c.trimmedMs
            )
        )
    }
    val pitch = pitchValues[c.pitchIdx]
    if (c.speed != 1f || pitch != 1f) {
        val sonic = SonicAudioProcessor()
        sonic.setSpeed(c.speed)
        sonic.setPitch(pitch)
        list.add(sonic)
    }
    return list
}

internal fun outputSize(first: Clip, resolution: String, aspectIdx: Int): Pair<Int, Int> {
    val shortSide = when (resolution) {
        "720p" -> 720
        "4K" -> 2160
        else -> 1080
    }
    val ratio = aspectRatios[aspectIdx]
        ?: if (first.height > first.width) 9f / 16f else 16f / 9f
    val w: Int
    val h: Int
    if (ratio >= 1f) {
        h = shortSide
        w = (shortSide * ratio).toInt()
    } else {
        w = shortSide
        h = (shortSide / ratio).toInt()
    }
    return Pair(w - w % 2, h - h % 2)
}

