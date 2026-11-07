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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val Bg = Color(0xFF0D0B1E)
private val Panel = Color(0xFF1A1635)
private val Accent = Color(0xFF7C4DFF)
private val Gold = Color(0xFFFFC107)
private val TextMain = Color(0xFFFFFFFF)
private val TextDim = Color(0xFFB8B3D6)

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
        Tool("📑", "نقل بنائیں"),
        Tool("🗑️", "حذف"),
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
        Tool("🖼️", "گیلری میں محفوظ کریں"),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var range by remember { mutableStateOf(0f..1f) }
    var exporting by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var category by remember { mutableIntStateOf(0) }
    var resolution by remember { mutableStateOf("1080p") }
    var fps by remember { mutableStateOf("30") }
    var quality by remember { mutableStateOf("اعلیٰ") }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }

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

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            durationMs = 0L
            range = 0f..1f
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }

    val pickVideo = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    fun onToolClick(tool: Tool) {
        val message = if (tool.ready) tool.hint else "${tool.label}: یہ سہولت جلد شامل ہو گی"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(
                        context,
                        "ایکسپورٹ مکمل ہو گیا:\n${outFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
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
            Text(
                "⚙️",
                fontSize = 22.sp,
                color = TextMain,
                modifier = Modifier.clickable {
                    Toast.makeText(context, "ترتیبات: جلد شامل ہوں گی", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // درمیانی حصہ
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (videoUri == null) {
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
                        onClick = { pickVideo() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("ویڈیو چنیں", color = TextMain)
                    }
                }
            } else {
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
                            .clickable { exoPlayer.seekTo((range.start * durationMs).toLong()) }
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
                            .clickable { exoPlayer.seekTo((range.endInclusive * durationMs).toLong()) }
                            .padding(8.dp)
                    )
                    Text(
                        "➕",
                        fontSize = 26.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clickable { pickVideo() }
                            .padding(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "آغاز: ${formatTime((range.start * durationMs).toLong())}",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                    Text(
                        "اختتام: ${formatTime((range.endInclusive * durationMs).toLong())}",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                }

                RangeSlider(
                    value = range,
                    onValueChange = {
                        range = it
                        exoPlayer.seekTo((it.start * durationMs).toLong())
                    },
                    enabled = durationMs > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Accent
                    )
                )
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
                    onClick = { exportVideo() },
                    enabled = videoUri != null && !exporting && durationMs > 0L,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (exporting) "ایکسپورٹ ہو رہا ہے…" else "ویڈیو ایکسپورٹ کریں",
                        color = TextMain
                    )
                }
                Text(
                    "ریزولوشن، فریم ریٹ اور کوالٹی کی ترتیب اگلے مرحلے میں فعال ہو گی",
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
}
