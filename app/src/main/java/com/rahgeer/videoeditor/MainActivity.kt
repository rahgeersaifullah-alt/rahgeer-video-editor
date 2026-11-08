package com.rahgeer.videoeditor

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF0D0B1E)
private val Panel = Color(0xFF1A1635)
private val Accent = Color(0xFF7C4DFF)
private val Gold = Color(0xFFFFC107)
private val TextMain = Color(0xFFFFFFFF)
private val TextDim = Color(0xFFB8B3D6)

private val speedNames = listOf("0.5x", "0.75x", "1x", "1.5x", "2x", "3x")
private val speedValues = listOf(0.5f, 0.75f, 1f, 1.5f, 2f, 3f)
private val cropNames = listOf("اصل", "1:1", "16:9", "9:16", "4:3")
private val cropRatios = listOf<Float?>(null, 1f, 16f / 9f, 9f / 16f, 4f / 3f)
private val filterNames = listOf("اصل", "بلیک اینڈ وائٹ", "گرم", "ٹھنڈا", "شوخ", "مدھم", "الٹے رنگ")
private val lookNames = listOf("کوئی نہیں", "سینما", "پرانی فلم", "ڈرامائی", "خواب جیسا")
private val aspectNames = listOf("اصل", "16:9", "9:16", "1:1", "4:5", "4:3")
private val aspectRatios = listOf<Float?>(null, 16f / 9f, 9f / 16f, 1f, 0.8f, 4f / 3f)
private val pitchNames = listOf("بھاری", "عام", "باریک", "بچوں جیسی")
private val pitchValues = listOf(0.8f, 1f, 1.3f, 1.6f)
private val textPosNames = listOf("اوپر", "درمیان", "نیچے")
private val textPosY = listOf(0.8f, 0f, -0.8f)
private val templateNames = listOf(
    "السلام علیکم",
    "عید مبارک",
    "جمعہ مبارک",
    "شکریہ",
    "سبسکرائب کریں",
    "لائک اور شیئر کریں",
    "راہگیر ویڈیو ایڈیٹر"
)
private val colorNames = listOf("سفید", "سنہری", "سرخ", "سبز", "نیلا", "کالا")
private val colorValues = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF5252.toInt(),
    0xFF69F0AE.toInt(),
    0xFF40C4FF.toInt(),
    0xFF000000.toInt()
)
private val cornerNames = listOf("اوپر دائیں", "اوپر بائیں", "نیچے دائیں", "نیچے بائیں", "درمیان")
private val cornerX = listOf(0.75f, -0.75f, 0.75f, -0.75f, 0f)
private val cornerY = listOf(0.7f, 0.7f, -0.7f, -0.7f, 0f)
private val shapeSymbols = listOf("●", "■", "▲", "★", "♥", "➤", "✔", "✖")
private val frameNames = listOf("کوئی نہیں", "سفید", "سنہری", "کالا", "سرخ")
private val frameColors = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFC107.toInt(),
    0xFF000000.toInt(),
    0xFFFF5252.toInt()
)
private val emojis = listOf(
    "😍", "😂", "🔥", "❤️", "👍", "⭐", "🎉", "😎",
    "💯", "🌹", "🕌", "🌙", "✨", "🎵", "📍", "🥳"
)
private val eqNames = listOf("بند", "بیس بوسٹ", "ٹریبل بوسٹ", "بیس + ٹریبل")
private val gateNames = listOf("بند", "ہلکا", "درمیانہ", "تیز")
private val fadeNames = listOf("بند", "0.5 سیکنڈ", "1 سیکنڈ", "2 سیکنڈ")
private val fadeMs = listOf(0, 500, 1000, 2000)
private val transitionNames = listOf("کوئی نہیں", "سیاہ فیڈ", "سفید فیڈ", "تیز فیڈ")
private val motionNames = listOf("کوئی نہیں", "زوم اِن", "زوم آؤٹ", "ہلکا ہلنا", "دھڑکن")
private val animNames = listOf("بند", "فیڈ اِن", "اوپر سلائیڈ", "چمکنا", "اُبھرنا")
private val drawWidths = listOf(8f, 16f, 28f)
private val drawWidthNames = listOf("باریک", "درمیانہ", "موٹا")

private data class DrawStroke(val color: Int, val width: Float, val points: List<Offset>)

private data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumb: ImageBitmap?,
    val startFrac: Float = 0f,
    val endFrac: Float = 1f,
    val speed: Float = 1f,
    val rotation: Int = 0,
    val cropIdx: Int = 0,
    val filterIdx: Int = 0,
    val lookIdx: Int = 0,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val pitchIdx: Int = 1,
    val volume: Float = 1f,
    val eqIdx: Int = 0,
    val gateIdx: Int = 0,
    val fadeInIdx: Int = 0,
    val fadeOutIdx: Int = 0,
    val fadeWhite: Boolean = false,
    val opacity: Float = 1f,
    val motionIdx: Int = 0,
    val grain: Boolean = false,
    val text: String = "",
    val textPos: Int = 2,
    val textColorIdx: Int = 0,
    val textAnimIdx: Int = 0,
    val sticker: String = "",
    val stickerPos: Int = 0,
    val shape: String = "",
    val shapeColorIdx: Int = 1,
    val shapePos: Int = 1,
    val frameIdx: Int = 0,
    val drawing: Bitmap? = null,
    val overlayUri: Uri? = null,
    val imagePos: Int = 3
) {
    val trimmedMs: Long get() = ((endFrac - startFrac) * durationMs).toLong()
    val outDur: Long get() = (trimmedMs / speed).toLong()
}

private data class Tool(
    val icon: String,
    val label: String,
    val ready: Boolean = false,
    val hint: String = ""
)

private data class Category(val icon: String, val label: String, val tools: List<Tool>)

private val categories = listOf(
    Category("✂️", "ایڈٹ", listOf(
        Tool("✂️", "کاٹیں", true, "سلائیڈر کے دونوں سروں کو کھینچ کر حصہ چنیں"),
        Tool("🔪", "تقسیم", true),
        Tool("🔲", "کراپ", true),
        Tool("🔄", "گھمائیں", true),
        Tool("⚡", "رفتار", true),
        Tool("⏪", "الٹا چلائیں"),
        Tool("❄️", "فریز فریم"),
        Tool("📑", "نقل بنائیں", true),
        Tool("🗑️", "حذف", true),
        Tool("📐", "سائز / ریشو", true),
        Tool("🪟", "اسپلٹ اسکرین"),
        Tool("🖼️", "پکچر اِن پکچر"),
        Tool("🔑", "کی فریم"),
        Tool("📹", "اسٹیبلائزر"),
        Tool("🎚️", "شفافیت / بلینڈ", true)
    )),
    Category("🎵", "آڈیو", listOf(
        Tool("🎵", "موسیقی", true),
        Tool("🔊", "ساؤنڈ ایفیکٹس"),
        Tool("🎤", "وائس اوور"),
        Tool("🔉", "آواز کی سطح", true),
        Tool("📈", "فیڈ اِن / آؤٹ", true),
        Tool("🔇", "شور ہٹائیں", true),
        Tool("🎼", "آڈیو الگ کریں", true),
        Tool("🗣️", "وائس چینجر", true),
        Tool("🔕", "خاموش کریں", true),
        Tool("🎧", "ایکولائزر", true)
    )),
    Category("🔤", "ٹیکسٹ", listOf(
        Tool("🔤", "ٹیکسٹ شامل کریں", true),
        Tool("💬", "خودکار کیپشنز"),
        Tool("🖋️", "اردو فونٹس"),
        Tool("✨", "ٹیکسٹ اینیمیشن", true),
        Tool("📄", "سب ٹائٹل فائل"),
        Tool("🎨", "ٹیکسٹ رنگ", true),
        Tool("📋", "ٹیکسٹ ٹیمپلیٹس", true),
        Tool("🌐", "ترجمہ")
    )),
    Category("🪄", "ایفیکٹس", listOf(
        Tool("🎨", "فلٹرز", true),
        Tool("🪄", "ایفیکٹس", true),
        Tool("🔀", "ٹرانزیشنز", true),
        Tool("🌈", "رنگ درست کریں", true),
        Tool("🌫️", "بلر"),
        Tool("💨", "موشن ایفیکٹ", true),
        Tool("🏞️", "بیک گراؤنڈ تبدیلی"),
        Tool("🟩", "گرین اسکرین"),
        Tool("🖼️", "فریمز اور بارڈر", true),
        Tool("🎞️", "فلم گرین", true),
        Tool("☀️", "روشنی / کنٹراسٹ", true)
    )),
    Category("😍", "اسٹیکر", listOf(
        Tool("😍", "اسٹیکرز", true),
        Tool("🙂", "ایموجی", true),
        Tool("🔷", "شکلیں", true),
        Tool("✏️", "ڈرائنگ", true),
        Tool("🎬", "GIF"),
        Tool("📷", "تصویر اوورلے", true),
        Tool("🏷️", "واٹر مارک", true)
    )),
    Category("📤", "ایکسپورٹ", listOf(
        Tool("💾", "پراجیکٹ محفوظ کریں"),
        Tool("🟢", "واٹس ایپ", true),
        Tool("▶️", "یوٹیوب", true),
        Tool("🎵", "ٹک ٹاک", true),
        Tool("🔵", "فیس بک", true),
        Tool("📸", "انسٹاگرام", true),
        Tool("⋯", "مزید شیئر", true)
    ))
)

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

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private class TimeRef {
    private var first = Long.MIN_VALUE

    fun local(t: Long): Long {
        if (first == Long.MIN_VALUE) first = t
        return t - first
    }
}

private class AudioFxProcessor(
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

private fun loadClip(context: Context, uri: Uri): Clip? {
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
        val frame = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        val thumb = frame?.let {
            val tw = 240
            val th = (tw * it.height / it.width.toFloat()).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(it, tw, th, true).asImageBitmap()
        }
        if (dur <= 0L) null else Clip(uri, dur, w, h, thumb)
    } catch (e: Exception) {
        null
    } finally {
        r.release()
    }
}

private fun audioDurationMs(context: Context, uri: Uri): Long {
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

private fun saveToGallery(context: Context, file: File, audio: Boolean): Uri? {
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

private fun loadBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
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

private fun effectiveSize(c: Clip): Pair<Int, Int> {
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

private fun geometryEffects(c: Clip): List<Effect> {
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

private fun colorEffects(c: Clip): List<Effect> {
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

private fun motionEffect(mode: Int, durMs: Long): MatrixTransformation {
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

private fun fadeMatrixEffect(
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

private fun makeSpan(text: String, colorInt: Int, sizePx: Int): SpannableString {
    val s = SpannableString(text)
    s.setSpan(ForegroundColorSpan(colorInt), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    s.setSpan(AbsoluteSizeSpan(sizePx), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return s
}

private fun makeText(text: String, colorInt: Int, sizePx: Int, x: Float, y: Float): TextOverlay {
    val settings = OverlaySettings.Builder()
        .setBackgroundFrameAnchor(x, y)
        .setOverlayFrameAnchor(0f, 0f)
        .build()
    return TextOverlay.createStaticTextOverlay(makeSpan(text, colorInt, sizePx), settings)
}

private fun animatedText(
    text: String,
    colorInt: Int,
    sizePx: Int,
    baseY: Float,
    mode: Int
): TextOverlay {
    val span = makeSpan(text, colorInt, sizePx)
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

private fun overlayEffects(context: Context, c: Clip, watermark: Boolean): List<Effect> {
    val overlays = ArrayList<TextureOverlay>()
    val (ew, eh) = effectiveSize(c)
    val base = minOf(ew, eh).coerceAtLeast(240)

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
            overlays.add(makeText(c.text, colorValues[c.textColorIdx], base / 12, 0f, textPosY[c.textPos]))
        } else {
            overlays.add(
                animatedText(c.text, colorValues[c.textColorIdx], base / 12, textPosY[c.textPos], c.textAnimIdx)
            )
        }
    }
    if (c.sticker.isNotEmpty()) {
        overlays.add(makeText(c.sticker, 0xFFFFFFFF.toInt(), base / 5, cornerX[c.stickerPos], cornerY[c.stickerPos]))
    }
    if (c.shape.isNotEmpty()) {
        overlays.add(makeText(c.shape, colorValues[c.shapeColorIdx], base / 5, cornerX[c.shapePos], cornerY[c.shapePos]))
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
        overlays.add(makeText("راہگیر ویڈیو ایڈیٹر", 0xCCFFFFFF.toInt(), (base / 24).coerceAtLeast(14), 0.5f, -0.92f))
    }
    return if (overlays.isEmpty()) {
        emptyList()
    } else {
        listOf(OverlayEffect(ImmutableList.copyOf(overlays)))
    }
}

private fun exportVideoEffects(
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

private fun audioProcessors(c: Clip): List<AudioProcessor> {
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

private fun outputSize(first: Clip, resolution: String, aspectIdx: Int): Pair<Int, Int> {
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

@Composable
private fun ToolButton(tool: Tool, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Accent else Panel)
            .border(
                1.dp,
                if (tool.ready) Gold else Color(0x33FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tool.icon, fontSize = 26.sp, color = TextMain)
        Spacer(Modifier.height(4.dp))
        Text(
            tool.label,
            color = TextMain,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (tool.ready) {
            Text("⭐", color = Gold, fontSize = 11.sp)
        } else {
            Text("جلد", color = Gold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = TextDim, fontSize = 13.sp, modifier = Modifier.width(80.dp))
        options.forEach { opt ->
            val on = opt == selected
            Text(
                opt,
                color = if (on) Color.Black else TextMain,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Gold else Panel)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, opt ->
            val on = i == selectedIndex
            Text(
                opt,
                color = if (on) Color.Black else TextMain,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Gold else Color(0xFF2A2450))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    Column {
        Text(label, color = TextDim, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onFinish,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Accent)
        )
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = TextMain,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Panel)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun StageRow(state: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            when (state) {
                2 -> "✅"
                1 -> "⏳"
                else -> "⚪"
            },
            fontSize = 16.sp,
            color = TextMain
        )
        Text(
            text,
            color = if (state == 0) TextDim else TextMain,
            fontSize = 14.sp,
            fontWeight = if (state == 1) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val clips = remember { mutableStateListOf<Clip>() }
    var selected by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var category by remember { mutableIntStateOf(0) }
    var activeTool by remember { mutableStateOf("") }
    var resolution by remember { mutableStateOf("1080p") }
    var fps by remember { mutableStateOf("30") }
    var quality by remember { mutableStateOf("اعلیٰ") }
    var aspectIdx by remember { mutableIntStateOf(0) }
    var transitionIdx by remember { mutableIntStateOf(0) }
    var muteAll by remember { mutableStateOf(false) }
    var watermark by remember { mutableStateOf(false) }
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var lastExportUri by remember { mutableStateOf<Uri?>(null) }
    var lastExportIsAudio by remember { mutableStateOf(false) }

    var exportState by remember { mutableIntStateOf(0) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportMessage by remember { mutableStateOf("") }
    var transformer by remember { mutableStateOf<Transformer?>(null) }

    var drawing by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var strokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var drawColorIdx by remember { mutableIntStateOf(2) }
    var drawWidthIdx by remember { mutableIntStateOf(1) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
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
            position = exoPlayer.currentPosition
            delay(250)
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

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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

    fun selectClip(index: Int) {
        if (index !in clips.indices) {
            selected = -1
            exoPlayer.clearMediaItems()
            return
        }
        selected = index
        val c = clips[index]
        applyPreview(c)
        exoPlayer.setMediaItem(MediaItem.fromUri(c.uri))
        exoPlayer.prepare()
        exoPlayer.seekTo((c.startFrac * c.durationMs).toLong())
    }

    fun updateClip(c: Clip) {
        if (selected in clips.indices) {
            clips[selected] = c
            applyPreview(c)
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                loading = true
                val loaded = withContext(Dispatchers.IO) {
                    uris.mapNotNull { loadClip(context, it) }
                }
                val wasEmpty = clips.isEmpty()
                clips.addAll(loaded)
                if (wasEmpty && clips.isNotEmpty()) selectClip(0)
                if (loaded.size < uris.size) toast("کچھ ویڈیوز لوڈ نہیں ہو سکیں")
                loading = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selected in clips.indices) {
            clips[selected] = clips[selected].copy(overlayUri = uri)
        }
    }

    val musicPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            musicUri = uri
            toast("موسیقی منتخب ہو گئی")
        }
    }

    val pickVideos = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    fun deleteSelected() {
        if (selected !in clips.indices) {
            toast("پہلے ویڈیو چنیں")
            return
        }
        clips.removeAt(selected)
        selectClip(if (clips.isEmpty()) -1 else minOf(selected, clips.lastIndex))
    }

    fun duplicateSelected() {
        if (selected !in clips.indices) {
            toast("پہلے ویڈیو چنیں")
            return
        }
        clips.add(selected + 1, clips[selected])
        selectClip(selected + 1)
    }

    fun moveSelected(delta: Int) {
        val target = selected + delta
        if (selected !in clips.indices || target !in clips.indices) return
        val c = clips.removeAt(selected)
        clips.add(target, c)
        selected = target
    }

    fun splitSelected() {
        if (selected !in clips.indices) {
            toast("پہلے ویڈیو چنیں")
            return
        }
        val c = clips[selected]
        val frac = exoPlayer.currentPosition.toFloat() / c.durationMs
        if (frac <= c.startFrac + 0.02f || frac >= c.endFrac - 0.02f) {
            toast("پلے ہیڈ سلائیڈر کو کٹ کی جگہ پر لائیں، پھر تقسیم دبائیں")
            return
        }
        clips[selected] = c.copy(endFrac = frac)
        clips.add(selected + 1, c.copy(startFrac = frac))
        toast("ویڈیو دو حصوں میں تقسیم ہو گئی")
    }

    fun commitDrawing() {
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

    fun shareTo(pkg: String?) {
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

    fun startExport(audioOnly: Boolean = false) {
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
            val mediaItem = MediaItem.Builder()
                .setUri(c.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs((c.startFrac * c.durationMs).toLong())
                        .setEndPositionMs((c.endFrac * c.durationMs).toLong())
                        .build()
                )
                .build()
            val builder = EditedMediaItem.Builder(mediaItem)
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

    fun onToolClick(tool: Tool) {
        val needsClip = selected in clips.indices
        when (tool.label) {
            "کاٹیں" -> toast(tool.hint)
            "حذف" -> deleteSelected()
            "نقل بنائیں" -> duplicateSelected()
            "تقسیم" -> splitSelected()
            "کراپ", "گھمائیں", "رفتار", "فلٹرز", "ایفیکٹس", "رنگ درست کریں",
            "روشنی / کنٹراسٹ", "فریمز اور بارڈر", "ٹیکسٹ شامل کریں", "ٹیکسٹ رنگ",
            "ٹیکسٹ ٹیمپلیٹس", "ٹیکسٹ اینیمیشن", "اسٹیکرز", "ایموجی", "شکلیں",
            "تصویر اوورلے", "وائس چینجر", "آواز کی سطح", "ایکولائزر", "شور ہٹائیں",
            "فیڈ اِن / آؤٹ", "موشن ایفیکٹ", "فلم گرین", "شفافیت / بلینڈ" -> {
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
            "سائز / ریشو", "موسیقی", "ٹرانزیشنز" -> activeTool = tool.label
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

    fun isActive(tool: Tool): Boolean = when (tool.label) {
        "واٹر مارک" -> watermark
        "خاموش کریں" -> muteAll
        else -> activeTool == tool.label
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {

        // اوپر کی پٹی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "راہگیر ویڈیو ایڈیٹر",
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "⚙️",
                    fontSize = 22.sp,
                    color = TextMain,
                    modifier = Modifier.clickable { toast("ترتیبات: جلد شامل ہوں گی") }
                )
                Text(
                    "📤 ایکسپورٹ",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gold)
                        .clickable { if (exportState == 0) startExport() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // درمیانی حصہ
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selected !in clips.indices) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Panel),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎬", fontSize = 48.sp, color = TextMain)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { pickVideos() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("ویڈیوز چنیں", color = TextMain)
                    }
                }
            } else {
                val sel = clips[selected]

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⏮",
                        fontSize = 28.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clickable { exoPlayer.seekTo((sel.startFrac * sel.durationMs).toLong()) }
                            .padding(8.dp)
                    )
                    Text(
                        if (isPlaying) "⏸️" else "▶️",
                        fontSize = 36.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clickable { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                            .padding(8.dp)
                    )
                    Text(
                        "⏭",
                        fontSize = 28.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clickable { exoPlayer.seekTo((sel.endFrac * sel.durationMs).toLong()) }
                            .padding(8.dp)
                    )
                    Text(
                        "➕",
                        fontSize = 26.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clickable { pickVideos() }
                            .padding(8.dp)
                    )
                }

                Text(
                    "پلے ہیڈ: ${formatTime(position)} / ${formatTime(sel.durationMs)}",
                    color = TextDim,
                    fontSize = 12.sp
                )
                Slider(
                    value = (position.toFloat() / sel.durationMs).coerceIn(0f, 1f),
                    onValueChange = {
                        val target = (it * sel.durationMs).toLong()
                        exoPlayer.seekTo(target)
                        position = target
                    },
                    colors = SliderDefaults.colors(thumbColor = TextMain, activeTrackColor = TextDim)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "آغاز: ${formatTime((sel.startFrac * sel.durationMs).toLong())}",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                    Text(
                        "اختتام: ${formatTime((sel.endFrac * sel.durationMs).toLong())}",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }

                RangeSlider(
                    value = sel.startFrac..sel.endFrac,
                    onValueChange = {
                        clips[selected] = sel.copy(
                            startFrac = it.start,
                            endFrac = it.endInclusive
                        )
                        exoPlayer.seekTo((it.start * sel.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Accent
                    )
                )
            }

            // ٹائم لائن
            if (clips.isNotEmpty() || loading) {
                val totalMs = clips.sumOf { it.outDur }
                Text(
                    if (loading) {
                        "ویڈیوز لوڈ ہو رہی ہیں…"
                    } else {
                        "ٹائم لائن: ${clips.size} ویڈیوز • کل دورانیہ ${formatTime(totalMs)}"
                    },
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (clips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clips.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Panel)
                                .border(
                                    2.dp,
                                    if (index == selected) Gold else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectClip(index) }
                        ) {
                            if (item.thumb != null) {
                                Image(
                                    bitmap = item.thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                "${index + 1}",
                                color = TextMain,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp)
                            )
                            Text(
                                formatTime(item.outDur),
                                color = TextMain,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Panel)
                            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { pickVideos() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("➕", fontSize = 20.sp, color = TextMain)
                        Text("مزید ویڈیوز", fontSize = 11.sp, color = TextDim)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("⏫ پہلے لائیں") { moveSelected(-1) }
                    SmallAction("⏬ بعد میں لے جائیں") { moveSelected(1) }
                    SmallAction("🗑️ حذف") { deleteSelected() }
                }
            }

            val current = categories[category]

            Text(
                current.label,
                color = Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // ٹول پینل
            val cur = clips.getOrNull(selected)
            if (activeTool.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Panel)
                        .border(1.dp, Color(0x80FFC107), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activeTool, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "✖",
                            color = TextMain,
                            fontSize = 18.sp,
                            modifier = Modifier.clickable { activeTool = "" }.padding(4.dp)
                        )
                    }

                    when (activeTool) {
                        "رفتار" -> if (cur != null) {
                            ChipRow(speedNames, speedValues.indexOf(cur.speed).coerceAtLeast(0)) {
                                updateClip(cur.copy(speed = speedValues[it]))
                            }
                            Text(
                                "رفتار کا اثر ایکسپورٹ شدہ ویڈیو میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "گھمائیں" -> if (cur != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { updateClip(cur.copy(rotation = (cur.rotation + 90) % 360)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Text("↻ 90° گھمائیں", color = TextMain)
                                }
                                Text("موجودہ: ${cur.rotation}°", color = TextDim, fontSize = 13.sp)
                            }
                        }

                        "کراپ" -> if (cur != null) {
                            ChipRow(cropNames, cur.cropIdx) { updateClip(cur.copy(cropIdx = it)) }
                        }

                        "فلٹرز" -> if (cur != null) {
                            ChipRow(filterNames, cur.filterIdx) { updateClip(cur.copy(filterIdx = it)) }
                        }

                        "ایفیکٹس" -> if (cur != null) {
                            ChipRow(lookNames, cur.lookIdx) { updateClip(cur.copy(lookIdx = it)) }
                        }

                        "فریمز اور بارڈر" -> if (cur != null) {
                            ChipRow(frameNames, cur.frameIdx) { clips[selected] = cur.copy(frameIdx = it) }
                            Text(
                                "فریم ایکسپورٹ شدہ ویڈیو میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "رنگ درست کریں", "روشنی / کنٹراسٹ" -> if (cur != null) {
                            LabeledSlider(
                                "روشنی",
                                cur.brightness,
                                -0.5f..0.5f,
                                { clips[selected] = cur.copy(brightness = it) },
                                { applyPreview(clips[selected]) }
                            )
                            LabeledSlider(
                                "کنٹراسٹ",
                                cur.contrast,
                                -0.5f..0.5f,
                                { clips[selected] = cur.copy(contrast = it) },
                                { applyPreview(clips[selected]) }
                            )
                            LabeledSlider(
                                "رنگوں کی شدت",
                                cur.saturation,
                                -100f..100f,
                                { clips[selected] = cur.copy(saturation = it) },
                                { applyPreview(clips[selected]) }
                            )
                            SmallAction("↺ ری سیٹ") {
                                updateClip(cur.copy(brightness = 0f, contrast = 0f, saturation = 0f))
                            }
                        }

                        "شفافیت / بلینڈ" -> if (cur != null) {
                            LabeledSlider(
                                "شفافیت: ${(cur.opacity * 100).toInt()}%",
                                cur.opacity,
                                0.1f..1f,
                                { clips[selected] = cur.copy(opacity = it) },
                                {}
                            )
                            Text("پس منظر", color = TextDim, fontSize = 12.sp)
                            ChipRow(listOf("سیاہ", "سفید"), if (cur.fadeWhite) 1 else 0) {
                                clips[selected] = cur.copy(fadeWhite = it == 1)
                            }
                            Text(
                                "اثر ایکسپورٹ شدہ ویڈیو میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "آواز کی سطح" -> if (cur != null) {
                            LabeledSlider(
                                "آواز: ${(cur.volume * 100).toInt()}%",
                                cur.volume,
                                0f..2f,
                                { clips[selected] = cur.copy(volume = it) },
                                {}
                            )
                            SmallAction("↺ ری سیٹ") { clips[selected] = cur.copy(volume = 1f) }
                            Text(
                                "آواز کا اثر ایکسپورٹ شدہ ویڈیو میں سنائی دے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "ایکولائزر" -> if (cur != null) {
                            ChipRow(eqNames, cur.eqIdx) { clips[selected] = cur.copy(eqIdx = it) }
                            Text(
                                "اثر ایکسپورٹ شدہ ویڈیو میں سنائی دے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "شور ہٹائیں" -> if (cur != null) {
                            ChipRow(gateNames, cur.gateIdx) { clips[selected] = cur.copy(gateIdx = it) }
                            Text(
                                "خاموش وقفوں کا ہلکا شور کم کرتا ہے، اثر ایکسپورٹ میں سنائی دے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "فیڈ اِن / آؤٹ" -> if (cur != null) {
                            Text("فیڈ اِن (شروع میں)", color = TextDim, fontSize = 12.sp)
                            ChipRow(fadeNames, cur.fadeInIdx) { clips[selected] = cur.copy(fadeInIdx = it) }
                            Text("فیڈ آؤٹ (آخر میں)", color = TextDim, fontSize = 12.sp)
                            ChipRow(fadeNames, cur.fadeOutIdx) { clips[selected] = cur.copy(fadeOutIdx = it) }
                            Text("ویڈیو کا رنگ", color = TextDim, fontSize = 12.sp)
                            ChipRow(listOf("سیاہ", "سفید"), if (cur.fadeWhite) 1 else 0) {
                                clips[selected] = cur.copy(fadeWhite = it == 1)
                            }
                            Text(
                                "ویڈیو اور آواز دونوں پر اثر، ایکسپورٹ میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "ٹرانزیشنز" -> {
                            ChipRow(transitionNames, transitionIdx) { transitionIdx = it }
                            Text(
                                "ہر ویڈیو کے شروع اور آخر میں فیڈ لگے گا، ایکسپورٹ میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "موشن ایفیکٹ" -> if (cur != null) {
                            ChipRow(motionNames, cur.motionIdx) { clips[selected] = cur.copy(motionIdx = it) }
                            Text(
                                "حرکت کا اثر ایکسپورٹ شدہ ویڈیو میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "فلم گرین" -> if (cur != null) {
                            ChipRow(listOf("بند", "آن"), if (cur.grain) 1 else 0) {
                                clips[selected] = cur.copy(grain = it == 1)
                            }
                            Text(
                                "گرین 1080p تک کی ویڈیو پر لگتا ہے، ایکسپورٹ میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "ٹیکسٹ اینیمیشن" -> if (cur != null) {
                            ChipRow(animNames, cur.textAnimIdx) { clips[selected] = cur.copy(textAnimIdx = it) }
                            Text(
                                "پہلے ٹیکسٹ شامل کریں میں متن لکھیں، پھر یہاں انداز چنیں",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "ٹیکسٹ ٹیمپلیٹس" -> if (cur != null) {
                            ChipRow(templateNames, templateNames.indexOf(cur.text)) {
                                clips[selected] = cur.copy(
                                    text = templateNames[it],
                                    textColorIdx = 1,
                                    textPos = 1
                                )
                            }
                            Text(
                                "متن بدلنا ہو تو ٹیکسٹ شامل کریں کھولیں",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "ٹیکسٹ شامل کریں", "ٹیکسٹ رنگ" -> if (cur != null) {
                            OutlinedTextField(
                                value = cur.text,
                                onValueChange = { clips[selected] = cur.copy(text = it) },
                                label = { Text("یہاں متن لکھیں", color = TextDim) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold,
                                    unfocusedBorderColor = TextDim,
                                    cursorColor = Gold,
                                    focusedTextColor = TextMain,
                                    unfocusedTextColor = TextMain
                                )
                            )
                            Text("جگہ", color = TextDim, fontSize = 12.sp)
                            ChipRow(textPosNames, cur.textPos) { clips[selected] = cur.copy(textPos = it) }
                            Text("رنگ", color = TextDim, fontSize = 12.sp)
                            ChipRow(colorNames, cur.textColorIdx) { clips[selected] = cur.copy(textColorIdx = it) }
                            Text(
                                "متن ایکسپورٹ شدہ ویڈیو میں نظر آئے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "اسٹیکرز", "ایموجی" -> if (cur != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                emojis.forEach { e ->
                                    Text(
                                        e,
                                        fontSize = 28.sp,
                                        color = TextMain,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (cur.sticker == e) Accent else Color(0xFF2A2450))
                                            .clickable { clips[selected] = cur.copy(sticker = e) }
                                            .padding(8.dp)
                                    )
                                }
                            }
                            Text("جگہ", color = TextDim, fontSize = 12.sp)
                            ChipRow(cornerNames, cur.stickerPos) { clips[selected] = cur.copy(stickerPos = it) }
                            SmallAction("🗑️ اسٹیکر ہٹائیں") { clips[selected] = cur.copy(sticker = "") }
                        }

                        "شکلیں" -> if (cur != null) {
                            ChipRow(shapeSymbols, shapeSymbols.indexOf(cur.shape)) {
                                clips[selected] = cur.copy(shape = shapeSymbols[it])
                            }
                            Text("رنگ", color = TextDim, fontSize = 12.sp)
                            ChipRow(colorNames, cur.shapeColorIdx) { clips[selected] = cur.copy(shapeColorIdx = it) }
                            Text("جگہ", color = TextDim, fontSize = 12.sp)
                            ChipRow(cornerNames, cur.shapePos) { clips[selected] = cur.copy(shapePos = it) }
                            SmallAction("🗑️ شکل ہٹائیں") { clips[selected] = cur.copy(shape = "") }
                        }

                        "تصویر اوورلے" -> if (cur != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Text("🖼️ تصویر چنیں", color = TextMain)
                                }
                                if (cur.overlayUri != null) {
                                    SmallAction("🗑️ ہٹائیں") { clips[selected] = cur.copy(overlayUri = null) }
                                }
                            }
                            Text("جگہ", color = TextDim, fontSize = 12.sp)
                            ChipRow(cornerNames, cur.imagePos) { clips[selected] = cur.copy(imagePos = it) }
                        }

                        "وائس چینجر" -> if (cur != null) {
                            ChipRow(pitchNames, cur.pitchIdx) { clips[selected] = cur.copy(pitchIdx = it) }
                            Text(
                                "آواز کا اثر ایکسپورٹ شدہ ویڈیو میں سنائی دے گا",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "سائز / ریشو" -> {
                            ChipRow(aspectNames, aspectIdx) { aspectIdx = it }
                            Text(
                                "یہ ایکسپورٹ شدہ ویڈیو کا سائز ہے",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        "موسیقی" -> {
                            Text(
                                if (musicUri != null) "✅ موسیقی منتخب ہے" else "ابھی کوئی موسیقی نہیں چنی",
                                color = TextMain,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { musicPicker.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Text("🎵 گانا / آڈیو چنیں", color = TextMain)
                                }
                                if (musicUri != null) {
                                    SmallAction("🗑️ ہٹائیں") { musicUri = null }
                                }
                            }
                        }
                    }
                }
            }

            if (category == categories.size - 1) {
                SettingRow("ریزولوشن", listOf("720p", "1080p", "4K"), resolution) { resolution = it }
                SettingRow("فریم ریٹ", listOf("24", "30", "60"), fps) { fps = it }
                SettingRow("کوالٹی", listOf("کم", "درمیانہ", "اعلیٰ"), quality) { quality = it }
                Button(
                    onClick = { startExport() },
                    enabled = clips.isNotEmpty() && exportState == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (clips.size > 1) "${clips.size} ویڈیوز جوڑ کر ایکسپورٹ کریں"
                        else "ویڈیو ایکسپورٹ کریں",
                        color = TextMain
                    )
                }
                Text(
                    "سب ویڈیوز ترتیب سے جُڑ کر ایک ویڈیو بنیں گی۔ ریزولوشن کام کرتا ہے، فریم ریٹ اور کوالٹی اگلے مرحلے میں فعال ہوں گے۔ شیئر کے بٹن ایکسپورٹ مکمل ہونے کے بعد کام کرتے ہیں۔",
                    color = TextDim,
                    fontSize = 11.sp
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                current.tools.forEach { tool ->
                    ToolButton(tool, isActive(tool)) { onToolClick(tool) }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // نیچے کی پٹی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEachIndexed { index, cat ->
                Column(
                    modifier = Modifier
                        .clickable { category = index }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(cat.icon, fontSize = 22.sp, color = TextMain)
                    Text(
                        cat.label,
                        color = if (index == category) Gold else TextDim,
                        fontSize = 11.sp,
                        fontWeight = if (index == category) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // ڈرائنگ کی ونڈو
    val drawClip = clips.getOrNull(selected)
    if (drawing && drawClip != null) {
        val (dw, dh) = effectiveSize(drawClip)
        val ratio = if (dw > 0 && dh > 0) dw.toFloat() / dh else 16f / 9f
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Panel)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("✏️ ڈرائنگ", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .onSizeChanged { boxSize = it }
                ) {
                    if (drawClip.thumb != null) {
                        Image(
                            bitmap = drawClip.thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { o -> strokePoints = listOf(o) },
                                    onDrag = { change, _ -> strokePoints = strokePoints + change.position },
                                    onDragEnd = {
                                        if (strokePoints.isNotEmpty()) {
                                            strokes.add(
                                                DrawStroke(
                                                    colorValues[drawColorIdx],
                                                    drawWidths[drawWidthIdx],
                                                    strokePoints
                                                )
                                            )
                                        }
                                        strokePoints = emptyList()
                                    }
                                )
                            }
                    ) {
                        strokes.forEach { s ->
                            for (i in 1 until s.points.size) {
                                drawLine(Color(s.color), s.points[i - 1], s.points[i], s.width, StrokeCap.Round)
                            }
                        }
                        for (i in 1 until strokePoints.size) {
                            drawLine(
                                Color(colorValues[drawColorIdx]),
                                strokePoints[i - 1],
                                strokePoints[i],
                                drawWidths[drawWidthIdx],
                                StrokeCap.Round
                            )
                        }
                    }
                }

                ChipRow(colorNames, drawColorIdx) { drawColorIdx = it }
                ChipRow(drawWidthNames, drawWidthIdx) { drawWidthIdx = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallAction("↩️ واپس") {
                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                    }
                    SmallAction("🗑️ صاف") { strokes.clear() }
                    SmallAction("❌ ہٹائیں") {
                        val c = clips.getOrNull(selected)
                        if (c != null) clips[selected] = c.copy(drawing = null)
                        strokes.clear()
                        drawing = false
                    }
                }

                Button(
                    onClick = { commitDrawing() },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ہو گیا", color = TextMain)
                }
            }
        }
    }

    // ایکسپورٹ کی پائپ لائن
    if (exportState != 0) {
        val done = exportState == 2
        val failed = exportState == 3
        val p = exportProgress
        val totalMs = clips.sumOf { it.outDur }
        val elapsedMs = totalMs * p / 100f

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Panel)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when {
                        failed -> "ایکسپورٹ ناکام"
                        done -> "ایکسپورٹ مکمل ہو گیا 🎉"
                        else -> "ایکسپورٹ جاری ہے…"
                    },
                    color = TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!failed) {
                    Text(
                        "${p.toInt()}%",
                        color = Gold,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { p / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Gold,
                        trackColor = Color(0x33FFFFFF)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StageRow(if (p > 0f || done) 2 else 1, "تیاری")
                        StageRow(
                            if (done || p >= 95f) 2 else if (p > 0f) 1 else 0,
                            "ویڈیوز پروسیس ہو رہی ہیں"
                        )
                        StageRow(
                            if (done) 2 else if (p >= 95f) 1 else 0,
                            "فائل تیار ہو رہی ہے"
                        )
                        StageRow(if (done) 2 else 0, "مکمل")
                    }

                    Text(
                        "ویڈیوز کی ترتیب",
                        color = TextDim,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var acc = 0L
                        clips.forEachIndexed { i, item ->
                            val startAcc = acc
                            val endAcc = acc + item.outDur
                            acc = endAcc
                            val st = if (done || elapsedMs >= endAcc) {
                                2
                            } else if (elapsedMs >= startAcc && p > 0f) {
                                1
                            } else {
                                0
                            }
                            val frac = if (item.outDur > 0) {
                                ((elapsedMs - startAcc) / item.outDur).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (st) {
                                            2 -> "✅"
                                            1 -> "⏳"
                                            else -> "⚪"
                                        },
                                        fontSize = 14.sp,
                                        color = TextMain
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "ویڈیو ${i + 1} • ${formatTime(item.outDur)}",
                                        color = if (st == 0) TextDim else TextMain,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (st == 1) {
                                        Text("${(frac * 100).toInt()}%", color = Gold, fontSize = 13.sp)
                                    } else if (st == 2) {
                                        Text("100%", color = TextDim, fontSize = 13.sp)
                                    }
                                }
                                if (st == 1) {
                                    LinearProgressIndicator(
                                        progress = { frac },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = Accent,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                }
                            }
                        }
                    }
                }

                if (exportMessage.isNotEmpty()) {
                    Text(
                        exportMessage,
                        color = TextMain,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (exportState == 1) {
                    Button(
                        onClick = {
                            transformer?.cancel()
                            exportState = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                    ) {
                        Text("منسوخ کریں", color = TextMain)
                    }
                } else {
                    if (done && lastExportUri != null) {
                        Button(
                            onClick = { shareTo(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text("📤 شیئر کریں", color = Color.Black)
                        }
                    }
                    Button(
                        onClick = { exportState = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("ٹھیک ہے", color = TextMain)
                    }
                }
            }
        }
    }
}
