@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop

@Composable
internal fun DrawDialog(ed: EditorCtx) {
    with(ed) {
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
                    val firstThumb = drawClip.thumbs.firstOrNull()
                    if (firstThumb != null) {
                        Image(
                            bitmap = firstThumb,
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
    }
}

@Composable
internal fun ExportDialog(ed: EditorCtx) {
    with(ed) {
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
}

