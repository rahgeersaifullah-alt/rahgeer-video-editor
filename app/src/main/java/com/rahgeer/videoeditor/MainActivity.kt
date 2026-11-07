package com.rahgeer.videoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
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

private data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumb: ImageBitmap?,
    val startFrac: Float = 0f,
    val endFrac: Float = 1f
) {
    val trimmedMs: Long get() = ((endFrac - startFrac) * durationMs).toLong()
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
        Tool("🔪", "تقسیم"),
        Tool("🔲", "کراپ"),
        Tool("🔄", "گھمائیں"),
        Tool("⚡", "رفتار"),
        Tool("⏪", "الٹا چلائیں"),
        Tool("❄️", "فریز فریم"),
        Tool("📑", "نقل بنائیں", true),
        Tool("🗑️", "حذف", true),
        Tool("📐", "سائز / ریشو"),
        Tool("🪟", "اسپلٹ اسکرین"),
        Tool("🖼️", "پکچر اِن پکچر"),
        Tool("🔑", "کی فریم"),
        Tool("📹", "اسٹیبلائزر"),
        Tool("🎚️", "شفافیت / بلینڈ")
    )),
    Category("🎵", "آڈیو", listOf(
        Tool("🎵", "موسیقی"),
        Tool("🔊", "ساؤنڈ ایفیکٹس"),
        Tool("🎤", "وائس اوور"),
        Tool("🔉", "آواز کی سطح"),
        Tool("📈", "فیڈ اِن / آؤٹ"),
        Tool("🔇", "شور ہٹائیں"),
        Tool("🎼", "آڈیو الگ کریں"),
        Tool("🗣️", "وائس چینجر"),
        Tool("🔕", "خاموش کریں"),
        Tool("🎧", "ایکولائزر")
    )),
    Category("🔤", "ٹیکسٹ", listOf(
        Tool("🔤", "ٹیکسٹ شامل کریں"),
        Tool("💬", "خودکار کیپشنز"),
        Tool("🖋️", "اردو فونٹس"),
        Tool("✨", "ٹیکسٹ اینیمیشن"),
        Tool("📄", "سب ٹائٹل فائل"),
        Tool("🎨", "رنگ اور سایہ"),
        Tool("📋", "ٹیکسٹ ٹیمپلیٹس"),
        Tool("🌐", "ترجمہ")
    )),
    Category("🪄", "ایفیکٹس", listOf(
        Tool("🎨", "فلٹرز"),
        Tool("🪄", "ایفیکٹس"),
        Tool("🔀", "ٹرانزیشنز"),
        Tool("🌈", "رنگ درست کریں"),
        Tool("🌫️", "بلر"),
        Tool("💨", "موشن ایفیکٹ"),
        Tool("🏞️", "بیک گراؤنڈ تبدیلی"),
        Tool("🟩", "گرین اسکرین"),
        Tool("🖼️", "فریمز اور بارڈر"),
        Tool("🎞️", "فلم گرین"),
        Tool("☀️", "روشنی / کنٹراسٹ")
    )),
    Category("😍", "اسٹیکر", listOf(
        Tool("😍", "اسٹیکرز"),
        Tool("🙂", "ایموجی"),
        Tool("🔷", "شکلیں"),
        Tool("✏️", "ڈرائنگ"),
        Tool("🎬", "GIF"),
        Tool("📷", "تصویر اوورلے"),
        Tool("🏷️", "واٹر مارک")
    )),
    Category("📤", "ایکسپورٹ", listOf(
        Tool("💾", "پراجیکٹ محفوظ کریں"),
        Tool("🟢", "واٹس ایپ"),
        Tool("▶️", "یوٹیوب"),
        Tool("🎵", "ٹک ٹاک"),
        Tool("🔵", "فیس بک"),
        Tool("📸", "انسٹاگرام"),
        Tool("⋯", "مزید شیئر")
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

private fun saveToGallery(context: Context, file: File): Boolean {
    if (Build.VERSION.SDK_INT < 29) return false
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Rahgeer")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun ToolButton(tool: Tool, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
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
    var category by remember { mutableIntStateOf(0) }
    var resolution by remember { mutableStateOf("1080p") }
    var fps by remember { mutableStateOf("30") }
    var quality by remember { mutableStateOf("اعلیٰ") }

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

    fun selectClip(index: Int) {
        if (index !in clips.indices) {
            selected = -1
            exoPlayer.clearMediaItems()
            return
        }
        selected = index
        val c = clips[index]
        exoPlayer.setMediaItem(MediaItem.fromUri(c.uri))
        exoPlayer.prepare()
        exoPlayer.seekTo((c.startFrac * c.durationMs).toLong())
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

    fun onToolClick(tool: Tool) {
        when (tool.label) {
            "کاٹیں" -> toast(tool.hint)
            "حذف" -> deleteSelected()
            "نقل بنائیں" -> duplicateSelected()
            else -> toast("${tool.label}: یہ سہولت جلد شامل ہو گی")
        }
    }

    fun startExport() {
        if (clips.isEmpty()) {
            toast("پہلے ویڈیو چنیں")
            return
        }
        exoPlayer.pause()

        val first = clips[0]
        val portrait = first.height > first.width
        val shortSide = when (resolution) {
            "720p" -> 720
            "4K" -> 2160
            else -> 1080
        }
        val longSide = shortSide * 16 / 9
        val outW = if (portrait) shortSide else longSide
        val outH = if (portrait) longSide else shortSide

        val presentation = Presentation.createForWidthAndHeight(
            outW,
            outH,
            Presentation.LAYOUT_SCALE_TO_FIT
        )
        val effects = Effects(emptyList(), listOf(presentation))

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
            EditedMediaItem.Builder(mediaItem).setEffects(effects).build()
        }
        val project = Composition.Builder(listOf(EditedMediaItemSequence(items))).build()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val outFile = File(dir, "rahgeer_${System.currentTimeMillis()}.mp4")

        exportProgress = 0f
        exportMessage = ""

        val t = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    exportProgress = 100f
                    exportState = 2
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { saveToGallery(context, outFile) }
                        exportMessage = if (ok) {
                            "ویڈیو گیلری میں محفوظ ہو گئی (Movies/Rahgeer)"
                        } else {
                            "ویڈیو اس جگہ محفوظ ہوئی:\n${outFile.absolutePath}"
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
                val totalMs = clips.sumOf { it.trimmedMs }
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
                                formatTime(item.trimmedMs),
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
                    "سب ویڈیوز ترتیب سے جُڑ کر ایک ویڈیو بنیں گی۔ ریزولوشن ابھی کام کرتا ہے، فریم ریٹ اور کوالٹی اگلے مرحلے میں فعال ہوں گے۔",
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
                    ToolButton(tool) { onToolClick(tool) }
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
        val totalMs = clips.sumOf { it.trimmedMs }
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
                            val endAcc = acc + item.trimmedMs
                            acc = endAcc
                            val st = if (done || elapsedMs >= endAcc) {
                                2
                            } else if (elapsedMs >= startAcc && p > 0f) {
                                1
                            } else {
                                0
                            }
                            val frac = if (item.trimmedMs > 0) {
                                ((elapsedMs - startAcc) / item.trimmedMs).coerceIn(0f, 1f)
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
                                        "ویڈیو ${i + 1} • ${formatTime(item.trimmedMs)}",
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
