@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import android.graphics.Typeface
import android.widget.TextView
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch

@Composable
internal fun ColumnScope.ToolPanel(ed: EditorCtx) {
    with(ed) {
    val fv = fontsVersion
    val fontLabels = listOf("ڈیفالٹ") + fontDefs.mapIndexed { i, d ->
        d.name + if (getTypeface(context, i + 1) != null) "" else " ⬇"
    }
    val cur = clips.getOrNull(selected)

        // ٹول پینل
        if (activeTool.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .background(Panel)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(activeTool, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                            exoPlayer.setPlaybackSpeed(speedValues[it])
                        }
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
                        Text("فریم ایکسپورٹ شدہ ویڈیو میں نظر آئے گا", color = TextDim, fontSize = 11.sp)
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
                        ChipRow(listOf("سیاہ پس منظر", "سفید پس منظر"), if (cur.fadeWhite) 1 else 0) {
                            clips[selected] = cur.copy(fadeWhite = it == 1)
                        }
                        Text("اثر ایکسپورٹ شدہ ویڈیو میں نظر آئے گا", color = TextDim, fontSize = 11.sp)
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
                    }

                    "ایکولائزر" -> if (cur != null) {
                        ChipRow(eqNames, cur.eqIdx) { clips[selected] = cur.copy(eqIdx = it) }
                    }

                    "شور ہٹائیں" -> if (cur != null) {
                        ChipRow(gateNames, cur.gateIdx) { clips[selected] = cur.copy(gateIdx = it) }
                        Text("خاموش وقفوں کا ہلکا شور کم کرتا ہے", color = TextDim, fontSize = 11.sp)
                    }

                    "فیڈ اِن / آؤٹ" -> if (cur != null) {
                        Text("فیڈ اِن (شروع میں)", color = TextDim, fontSize = 12.sp)
                        ChipRow(fadeNames, cur.fadeInIdx) { clips[selected] = cur.copy(fadeInIdx = it) }
                        Text("فیڈ آؤٹ (آخر میں)", color = TextDim, fontSize = 12.sp)
                        ChipRow(fadeNames, cur.fadeOutIdx) { clips[selected] = cur.copy(fadeOutIdx = it) }
                        ChipRow(listOf("سیاہ", "سفید"), if (cur.fadeWhite) 1 else 0) {
                            clips[selected] = cur.copy(fadeWhite = it == 1)
                        }
                    }

                    "ٹرانزیشنز" -> {
                        ChipRow(transitionNames, transitionIdx) { transitionIdx = it }
                        Text("ہر ویڈیو کے شروع اور آخر میں فیڈ لگے گا", color = TextDim, fontSize = 11.sp)
                    }

                    "موشن ایفیکٹ" -> if (cur != null) {
                        ChipRow(motionNames, cur.motionIdx) { clips[selected] = cur.copy(motionIdx = it) }
                    }

                    "فلم گرین" -> if (cur != null) {
                        ChipRow(listOf("بند", "آن"), if (cur.grain) 1 else 0) {
                            clips[selected] = cur.copy(grain = it == 1)
                        }
                        Text("گرین 1080p تک کی ویڈیو پر لگتا ہے", color = TextDim, fontSize = 11.sp)
                    }

                    "ٹیکسٹ اینیمیشن" -> if (cur != null) {
                        ChipRow(animNames, cur.textAnimIdx) { clips[selected] = cur.copy(textAnimIdx = it) }
                        Text("پہلے ٹیکسٹ شامل کریں میں متن لکھیں", color = TextDim, fontSize = 11.sp)
                    }

                    "ٹیکسٹ ٹیمپلیٹس" -> if (cur != null) {
                        ChipRow(templateNames, templateNames.indexOf(cur.text)) {
                            clips[selected] = cur.copy(
                                text = templateNames[it],
                                textColorIdx = 1,
                                textPos = 1
                            )
                        }
                    }

                    "ٹیکسٹ شامل کریں", "ٹیکسٹ رنگ", "اردو فونٹس" -> if (cur != null) {
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
                        Text("فونٹ", color = TextDim, fontSize = 12.sp)
                        ChipRow(fontLabels, cur.fontIdx) { idx ->
                            if (idx == 0 || getTypeface(context, idx) != null) {
                                clips[selected] = cur.copy(fontIdx = idx)
                            } else {
                                toast("پہلے فونٹس ڈاؤن لوڈ کریں")
                            }
                        }
                        AndroidView(
                            factory = { ctx -> TextView(ctx) },
                            update = { tv ->
                                val refresh = fv
                                tv.text = if (cur.text.isNotBlank()) cur.text else "راہگیر ویڈیو ایڈیٹر"
                                tv.textSize = 24f
                                tv.setTextColor(colorValues[cur.textColorIdx])
                                tv.typeface = getTypeface(context, cur.fontIdx) ?: Typeface.DEFAULT
                                tv.setPadding(0, 8, 0, 8)
                                if (refresh < 0) tv.text = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { downloadFonts() },
                                enabled = !downloadingFonts,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("⬇️ اردو فونٹس ڈاؤن لوڈ", color = TextMain, fontSize = 12.sp)
                            }
                            Text(fontMsg, color = TextDim, fontSize = 11.sp)
                        }
                        Text("جگہ", color = TextDim, fontSize = 12.sp)
                        ChipRow(textPosNames, cur.textPos) { clips[selected] = cur.copy(textPos = it) }
                        Text("رنگ", color = TextDim, fontSize = 12.sp)
                        ChipRow(colorNames, cur.textColorIdx) { clips[selected] = cur.copy(textColorIdx = it) }
                        Text("متن ایکسپورٹ شدہ ویڈیو میں نظر آئے گا", color = TextDim, fontSize = 11.sp)
                    }

                    "اسٹیکرز", "ایموجی" -> if (cur != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emojis.forEach { e ->
                                Text(
                                    e,
                                    fontSize = 26.sp,
                                    color = TextMain,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (cur.sticker == e) Accent else Color(0xFF2A2450))
                                        .clickable { clips[selected] = cur.copy(sticker = e) }
                                        .padding(8.dp)
                                )
                            }
                        }
                        ChipRow(cornerNames, cur.stickerPos) { clips[selected] = cur.copy(stickerPos = it) }
                        SmallAction("🗑️ اسٹیکر ہٹائیں") { clips[selected] = cur.copy(sticker = "") }
                    }

                    "شکلیں" -> if (cur != null) {
                        ChipRow(shapeSymbols, shapeSymbols.indexOf(cur.shape)) {
                            clips[selected] = cur.copy(shape = shapeSymbols[it])
                        }
                        ChipRow(colorNames, cur.shapeColorIdx) { clips[selected] = cur.copy(shapeColorIdx = it) }
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
                        ChipRow(cornerNames, cur.imagePos) { clips[selected] = cur.copy(imagePos = it) }
                    }

                    "وائس چینجر" -> if (cur != null) {
                        ChipRow(pitchNames, cur.pitchIdx) { clips[selected] = cur.copy(pitchIdx = it) }
                    }

                    "سائز / ریشو" -> {
                        ChipRow(aspectNames, aspectIdx) { aspectIdx = it }
                        Text("یہ ایکسپورٹ شدہ ویڈیو کا سائز ہے", color = TextDim, fontSize = 11.sp)
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

                    "ایکسپورٹ سیٹنگ" -> {
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
                            "ریزولوشن کام کرتا ہے، فریم ریٹ اور کوالٹی اگلے مرحلے میں فعال ہوں گے۔ شیئر کے بٹن ایکسپورٹ کے بعد کام کرتے ہیں۔",
                            color = TextDim,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

    }
}
