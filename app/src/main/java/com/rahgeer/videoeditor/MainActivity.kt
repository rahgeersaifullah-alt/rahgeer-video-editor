package com.rahgeer.videoeditor

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
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
private val aspectNames = listOf("اصل", "16:9", "9:16", "1:1", "4:5", "4:3")
private val aspectRatios = listOf<Float?>(null, 16f / 9f, 9f / 16f, 1f, 0.8f, 4f / 3f)
private val pitchNames = listOf("بھاری", "عام", "باریک", "بچوں جیسی")
private val pitchValues = listOf(0.8f, 1f, 1.3f, 1.6f)
private val textPosNames = listOf("اوپر", "درمیان", "نیچے")
private val textPosY = listOf(0.8f, 0f, -0.8f)
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
private val emojis = listOf(
    "😍", "😂", "🔥", "❤️", "👍", "⭐", "🎉", "😎",
    "💯", "🌹", "🕌", "🌙", "✨", "🎵", "📍", "🥳"
)

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
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val pitchIdx: Int = 1,
    val text: String = "",
    val textPos: Int = 2,
    val textColorIdx: Int = 0,
    val sticker: String = "",
    val stickerPos: Int = 0,
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
        Tool("🎚️", "شفافیت / بلینڈ")
    )),
    Category("🎵", "آڈیو", listOf(
        Tool("🎵", "موسیقی", true),
        Tool("🔊", "ساؤنڈ ایفیکٹس"),
        Tool("🎤", "وائس اوور"),
        Tool("🔉", "آواز کی سطح"),
        Tool("📈", "فیڈ اِن / آؤٹ"),
        Tool("🔇", "شور ہٹائیں"),
        Tool("🎼", "آڈیو الگ کریں", true),
        Tool("🗣️", "وائس چینجر", true),
        Tool("🔕", "خاموش کریں", true),
        Tool("🎧", "ایکولائزر")
    )),
    Category("🔤", "ٹیکسٹ", listOf(
        Tool("🔤", "ٹیکسٹ شامل کریں", true),
        Tool("💬", "خودکار کیپشنز"),
        Tool("🖋️", "اردو فونٹس"),
        Tool("✨", "ٹیکسٹ اینیمیشن"),
        Tool("📄", "سب ٹائٹل فائل"),
        Tool("🎨", "ٹیکسٹ رنگ", true),
        Tool("📋", "ٹیکسٹ ٹیمپلیٹس"),
        Tool("🌐", "ترجمہ")
    )),
    Category("🪄", "ایفیکٹس", listOf(
        Tool("🎨", "فلٹرز", true),
        Tool("🪄", "ایفیکٹس"),
        Tool("🔀", "ٹرانزیشنز"),
        Tool("🌈", "رنگ درست کریں", true),
        Tool("🌫️", "بلر"),
        Tool("💨", "موشن ایفیکٹ"),
        Tool("🏞️", "بیک گراؤنڈ تبدیلی"),
        Tool("🟩", "گرین اسکرین"),
        Tool("🖼️", "فریمز اور بارڈر"),
        Tool("🎞️", "فلم گرین"),
        Tool("☀️", "روشنی / کنٹراسٹ", true)
    )),
    Category("😍", "اسٹیکر", listOf(
        Tool("😍", "اسٹیکرز", true),
        Tool("🙂", "ایموجی", true),
        Tool("🔷", "شکلیں"),
        Tool("✏️", "ڈرائنگ"),
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
    if (c.brightness != 0f) list.add(Brightness(c.brightness))
    if (c.contrast != 0f) list.add(Contrast(c.contrast))
    if (c.saturation != 0f) list.add(HslAdjustment.Builder().adjustSaturation(c.saturation).build())
    return list
}

private fun makeText(text: String, colorInt: Int, sizePx: Int, x: Float, y: Float): TextOverlay {
    val s = SpannableString(text)
    s.setSpan(ForegroundColorSpan(colorInt), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    s.setSpan(AbsoluteSizeSpan(sizePx), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    s.setSpan(StyleSpan(Typeface.BOLD), 0, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val settings = OverlaySettings.Builder()
        .setBackgroundFrameAnchor(x, y)
        .setOverlayFrameAnchor(0f, 0f)
        .build()
    return TextOverlay.createStaticTextOverlay(s, settings)
}

private fun overlayEffects(context: Context, c: Clip, watermark: Boolean): List<Effect> {
    val overlays = ArrayList<TextureOverlay>()
    val base = minOf(c.width, c.height).coerceAtLeast(240)

    if (c.text.isNotBlank()) {
        overlays.add(makeText(c.text, colorValues[c.textColorIdx], base / 12, 0f, textPosY[c.textPos]))
    }
    if (c.sticker.isNotEmpty()) {
        overlays.add(makeText(c.sticker, 0xFFFFFFFF.toInt(), base / 5, cornerX[c.stickerPos], cornerY[c.stickerPos]))
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
    watermark: Boolean
): List<Effect> {
    val list = mutableListOf<Effect>()
    list.addAll(geometryEffects(c))
    list.addAll(colorEffects(c))
    list.addAll(overlayEffects(context, c, watermark))
    if (c.speed != 1f) list.add(SpeedChangeEffect(c.speed))
    list.add(presentation)
    return list
}

private fun audioProcessors(c: Clip): List<AudioProcessor> {
    val pitch = pitchValues[c.pitchIdx]
    if (c.speed == 1f && pitch == 1f) return emptyList()
    val sonic = SonicAudioProcessor()
    sonic.setSpeed(c.speed)
    sonic.setPitch(pitch)
    return listOf(sonic)
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
        if (!tool.ready) {
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
    var muteAll by remember { mutableStateOf(false) }
    var watermark by remember { mutableStateOf(false) }
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var lastExportUri by remember { mutableStateOf<Uri?>(null) }
    var lastExportIsAudio by remember { mutableStateOf(false) }

    var exportState by remember { mutableIntStateOf(0) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportMessage by remember { mutableStateOf("") }
    var transformer by remember { mutableStateOf<Transformer?>(null) }

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
                        exportVideoEffects(context, c, presentation, watermark)
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
            "کراپ", "گھمائیں", "رفتار", "فلٹرز", "رنگ درست کریں", "روشنی / کنٹراسٹ",
            "ٹیکسٹ شامل کریں", "ٹیکسٹ رنگ", "اسٹیکرز", "ایموجی", "تصویر اوورلے",
            "وائس چینجر" -> {
                if (needsClip) activeTool = tool.label else toast("پہلے ویڈیو چنیں")
            }
            "سائز / ریشو", "موسیقی" -> activeTool = tool.label
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
