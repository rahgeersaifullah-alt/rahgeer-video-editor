@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal val Bg = Color(0xFF0D0B1E)
internal val Panel = Color(0xFF1A1635)
internal val Accent = Color(0xFF7C4DFF)
internal val Gold = Color(0xFFFFC107)
internal val TextMain = Color(0xFFFFFFFF)
internal val TextDim = Color(0xFFB8B3D6)

internal val speedNames = listOf("0.5x", "0.75x", "1x", "1.5x", "2x", "3x")
internal val speedValues = listOf(0.5f, 0.75f, 1f, 1.5f, 2f, 3f)
internal val cropNames = listOf("اصل", "1:1", "16:9", "9:16", "4:3")
internal val cropRatios = listOf<Float?>(null, 1f, 16f / 9f, 9f / 16f, 4f / 3f)
internal val filterNames = listOf("اصل", "بلیک اینڈ وائٹ", "گرم", "ٹھنڈا", "شوخ", "مدھم", "الٹے رنگ")
internal val lookNames = listOf("کوئی نہیں", "سینما", "پرانی فلم", "ڈرامائی", "خواب جیسا")
internal val aspectNames = listOf("اصل", "16:9", "9:16", "1:1", "4:5", "4:3")
internal val aspectRatios = listOf<Float?>(null, 16f / 9f, 9f / 16f, 1f, 0.8f, 4f / 3f)
internal val pitchNames = listOf("بھاری", "عام", "باریک", "بچوں جیسی")
internal val pitchValues = listOf(0.8f, 1f, 1.3f, 1.6f)
internal val textPosNames = listOf("اوپر", "درمیان", "نیچے")
internal val textPosY = listOf(0.8f, 0f, -0.8f)
internal val templateNames = listOf(
    "السلام علیکم",
    "عید مبارک",
    "جمعہ مبارک",
    "شکریہ",
    "سبسکرائب کریں",
    "لائک اور شیئر کریں",
    "راہگیر ویڈیو ایڈیٹر"
)
internal val colorNames = listOf("سفید", "سنہری", "سرخ", "سبز", "نیلا", "کالا")
internal val colorValues = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF5252.toInt(),
    0xFF69F0AE.toInt(),
    0xFF40C4FF.toInt(),
    0xFF000000.toInt()
)
internal val cornerNames = listOf("اوپر دائیں", "اوپر بائیں", "نیچے دائیں", "نیچے بائیں", "درمیان")
internal val cornerX = listOf(0.75f, -0.75f, 0.75f, -0.75f, 0f)
internal val cornerY = listOf(0.7f, 0.7f, -0.7f, -0.7f, 0f)
internal val shapeSymbols = listOf("●", "■", "▲", "★", "♥", "➤", "✔", "✖")
internal val frameNames = listOf("کوئی نہیں", "سفید", "سنہری", "کالا", "سرخ")
internal val frameColors = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFC107.toInt(),
    0xFF000000.toInt(),
    0xFFFF5252.toInt()
)
internal val emojis = listOf(
    "😍", "😂", "🔥", "❤️", "👍", "⭐", "🎉", "😎",
    "💯", "🌹", "🕌", "🌙", "✨", "🎵", "📍", "🥳"
)
internal val eqNames = listOf("بند", "بیس بوسٹ", "ٹریبل بوسٹ", "بیس + ٹریبل")
internal val gateNames = listOf("بند", "ہلکا", "درمیانہ", "تیز")
internal val fadeNames = listOf("بند", "0.5 سیکنڈ", "1 سیکنڈ", "2 سیکنڈ")
internal val fadeMs = listOf(0, 500, 1000, 2000)
internal val transitionNames = listOf("کوئی نہیں", "سیاہ فیڈ", "سفید فیڈ", "تیز فیڈ")
internal val motionNames = listOf("کوئی نہیں", "زوم اِن", "زوم آؤٹ", "ہلکا ہلنا", "دھڑکن")
internal val animNames = listOf("بند", "فیڈ اِن", "اوپر سلائیڈ", "چمکنا", "اُبھرنا")
internal val drawWidths = listOf(8f, 16f, 28f)
internal val drawWidthNames = listOf("باریک", "درمیانہ", "موٹا")

internal const val FONT_BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl/"

internal class FontDef(val name: String, val file: String, val url: String)

internal val fontDefs = listOf(
    FontDef("نوٹو نستعلیق", "NotoNastaliqUrdu.ttf", FONT_BASE + "notonastaliqurdu/NotoNastaliqUrdu%5Bwght%5D.ttf"),
    FontDef("گلزار نستعلیق", "Gulzar.ttf", FONT_BASE + "gulzar/Gulzar-Regular.ttf"),
    FontDef("نوٹو نسخ", "NotoNaskhArabic.ttf", FONT_BASE + "notonaskharabic/NotoNaskhArabic%5Bwght%5D.ttf"),
    FontDef("امیری", "Amiri.ttf", FONT_BASE + "amiri/Amiri-Regular.ttf"),
    FontDef("لطیف", "Lateef.ttf", FONT_BASE + "lateef/Lateef-Regular.ttf"),
    FontDef("شہرزاد", "Scheherazade.ttf", FONT_BASE + "scheherazadenew/ScheherazadeNew-Regular.ttf")
)

internal val fontCache = HashMap<String, Typeface>()

internal fun getTypeface(context: Context, idx: Int): Typeface? {
    if (idx <= 0 || idx > fontDefs.size) return null
    val def = fontDefs[idx - 1]
    val cached = fontCache[def.file]
    if (cached != null) return cached
    val f = File(File(context.filesDir, "fonts"), def.file)
    if (!f.exists() || f.length() < 1000L) return null
    return try {
        val tf = Typeface.createFromFile(f)
        fontCache[def.file] = tf
        tf
    } catch (e: Exception) {
        null
    }
}

internal fun downloadFont(context: Context, def: FontDef): Boolean {
    return try {
        val dir = File(context.filesDir, "fonts")
        dir.mkdirs()
        val tmp = File(dir, def.file + ".tmp")
        val target = File(dir, def.file)
        val conn = URL(def.url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        if (conn.responseCode != 200) return false
        conn.inputStream.use { input ->
            tmp.outputStream().use { out -> input.copyTo(out) }
        }
        if (tmp.length() < 1000L) {
            tmp.delete()
            return false
        }
        target.delete()
        tmp.renameTo(target)
    } catch (e: Exception) {
        false
    }
}

internal class FontSpan(private val tf: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = tf
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = tf
    }
}

internal data class DrawStroke(val color: Int, val width: Float, val points: List<Offset>)

internal data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val thumbs: List<ImageBitmap>,
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
    val fontIdx: Int = 0,
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

internal data class Tool(
    val icon: String,
    val label: String,
    val ready: Boolean = false,
    val hint: String = ""
)

internal data class Category(val icon: String, val label: String, val tools: List<Tool>)

internal val categories = listOf(
    Category("✂️", "ایڈٹ", listOf(
        Tool("✂️", "کاٹیں", true, "کلپ پر ٹیپ کریں، پھر سنہری کناروں کو کھینچیں"),
        Tool("🔪", "تقسیم", true),
        Tool("⬅️", "بائیں حذف", true),
        Tool("➡️", "دائیں حذف", true),
        Tool("🗑️", "حذف", true),
        Tool("📑", "نقل بنائیں", true),
        Tool("⏫", "پہلے لائیں", true),
        Tool("⏬", "بعد لے جائیں", true),
        Tool("🔲", "کراپ", true),
        Tool("🔄", "گھمائیں", true),
        Tool("⚡", "رفتار", true),
        Tool("📐", "سائز / ریشو", true),
        Tool("🎚️", "شفافیت / بلینڈ", true),
        Tool("⏪", "الٹا چلائیں"),
        Tool("❄️", "فریز فریم"),
        Tool("🪟", "اسپلٹ اسکرین"),
        Tool("🖼️", "پکچر اِن پکچر"),
        Tool("🔑", "کی فریم"),
        Tool("📹", "اسٹیبلائزر")
    )),
    Category("🎵", "آڈیو", listOf(
        Tool("🎵", "موسیقی", true),
        Tool("🔉", "آواز کی سطح", true),
        Tool("📈", "فیڈ اِن / آؤٹ", true),
        Tool("🔇", "شور ہٹائیں", true),
        Tool("🎧", "ایکولائزر", true),
        Tool("🗣️", "وائس چینجر", true),
        Tool("🔕", "خاموش کریں", true),
        Tool("🎼", "آڈیو الگ کریں", true),
        Tool("🔊", "ساؤنڈ ایفیکٹس"),
        Tool("🎤", "وائس اوور")
    )),
    Category("🔤", "ٹیکسٹ", listOf(
        Tool("🔤", "ٹیکسٹ شامل کریں", true),
        Tool("🖋️", "اردو فونٹس", true),
        Tool("🎨", "ٹیکسٹ رنگ", true),
        Tool("✨", "ٹیکسٹ اینیمیشن", true),
        Tool("📋", "ٹیکسٹ ٹیمپلیٹس", true),
        Tool("💬", "خودکار کیپشنز"),
        Tool("📄", "سب ٹائٹل فائل"),
        Tool("🌐", "ترجمہ")
    )),
    Category("🪄", "ایفیکٹس", listOf(
        Tool("🎨", "فلٹرز", true),
        Tool("🪄", "ایفیکٹس", true),
        Tool("🌈", "رنگ درست کریں", true),
        Tool("☀️", "روشنی / کنٹراسٹ", true),
        Tool("🔀", "ٹرانزیشنز", true),
        Tool("💨", "موشن ایفیکٹ", true),
        Tool("🎞️", "فلم گرین", true),
        Tool("🖼️", "فریمز اور بارڈر", true),
        Tool("🌫️", "بلر"),
        Tool("🏞️", "بیک گراؤنڈ تبدیلی"),
        Tool("🟩", "گرین اسکرین")
    )),
    Category("😍", "اسٹیکر", listOf(
        Tool("😍", "اسٹیکرز", true),
        Tool("🙂", "ایموجی", true),
        Tool("🔷", "شکلیں", true),
        Tool("✏️", "ڈرائنگ", true),
        Tool("📷", "تصویر اوورلے", true),
        Tool("🏷️", "واٹر مارک", true),
        Tool("🎬", "GIF")
    )),
    Category("📤", "ایکسپورٹ", listOf(
        Tool("⚙️", "ایکسپورٹ سیٹنگ", true),
        Tool("🟢", "واٹس ایپ", true),
        Tool("▶️", "یوٹیوب", true),
        Tool("🎵", "ٹک ٹاک", true),
        Tool("🔵", "فیس بک", true),
        Tool("📸", "انسٹاگرام", true),
        Tool("⋯", "مزید شیئر", true),
        Tool("💾", "پراجیکٹ محفوظ کریں")
    ))
)


internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

