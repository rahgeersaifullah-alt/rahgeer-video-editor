@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@Composable
internal fun ColumnScope.TopBar(ed: EditorCtx) {
    with(ed) {
        // اوپر کی پٹی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "راہگیر ویڈیو ایڈیٹر",
                color = TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
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
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun ColumnScope.PreviewArea(ed: EditorCtx) {
    with(ed) {
        // بڑی ویڈیو اسکرین
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 140.dp)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (clips.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", fontSize = 44.sp, color = TextMain)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { pickVideos() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text(if (loading) "لوڈ ہو رہی ہیں…" else "ویڈیوز چنیں", color = TextMain)
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun ColumnScope.TransportBar(ed: EditorCtx) {
    with(ed) {
        // چلانے کی پٹی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "↩️",
                    fontSize = 20.sp,
                    color = if (undoStack.isNotEmpty()) TextMain else Color(0x55FFFFFF),
                    modifier = Modifier.clickable { undo() }.padding(8.dp)
                )
                Text(
                    "↪️",
                    fontSize = 20.sp,
                    color = if (redoStack.isNotEmpty()) TextMain else Color(0x55FFFFFF),
                    modifier = Modifier.clickable { redo() }.padding(8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "⏮",
                    fontSize = 24.sp,
                    color = TextMain,
                    modifier = Modifier
                        .clickable {
                            if (selected in clips.indices) {
                                seekGlobal(startOf(selected))
                                syncScroll()
                            }
                        }
                        .padding(6.dp)
                )
                Text(
                    if (isPlaying) "⏸️" else "▶️",
                    fontSize = 30.sp,
                    color = TextMain,
                    modifier = Modifier
                        .clickable {
                            if (clips.isNotEmpty()) {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                        seekGlobal(0L)
                                        syncScroll()
                                    }
                                    exoPlayer.play()
                                }
                            }
                        }
                        .padding(6.dp)
                )
                Text(
                    "⏭",
                    fontSize = 24.sp,
                    color = TextMain,
                    modifier = Modifier
                        .clickable {
                            if (selected in clips.indices) {
                                seekGlobal(startOf(selected) + clips[selected].trimmedMs - 1L)
                                syncScroll()
                            }
                        }
                        .padding(6.dp)
                )
            }
            Text(
                "${formatTime(playheadMs)} / ${formatTime(totalTrimmed())}",
                color = TextDim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
internal fun ColumnScope.TimelineArea(ed: EditorCtx) {
    with(ed) {
        // ٹائم لائن
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(Color(0xFF111026))
            ) {
                val halfW = maxWidth / 2
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(halfW))
                    clips.forEachIndexed { index, item ->
                        ClipBlock(
                            item = item,
                            selected = index == selected,
                            widthDp = (item.trimmedMs / 1000f * dpPerSec).dp,
                            index = index,
                            onClick = {
                                seekGlobal(startOf(index))
                                syncScroll()
                            },
                            onTrimStart = { trimEdge(index, it, true) },
                            onTrimEnd = { trimEdge(index, it, false) },
                            onTrimDone = { rebuild(playheadMs) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .width(52.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Panel)
                            .clickable { pickVideos() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➕", fontSize = 20.sp, color = TextMain)
                    }
                    Spacer(Modifier.width(halfW))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
internal fun ColumnScope.CutBar(ed: EditorCtx) {
    with(ed) {
        // کٹنگ کی فوری پٹی
        if (clips.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111026))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "کاٹنے کا طریقہ: ٹائم لائن کو کھینچ کر سفید لکیر کٹ کی جگہ پر لائیں، پھر ✂️ دبائیں",
                        color = TextDim,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "🔍➖",
                        fontSize = 14.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2450))
                            .clickable { zoom(0.7f) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        "🔍➕",
                        fontSize = 14.sp,
                        color = TextMain,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2450))
                            .clickable { zoom(1.4f) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CutButton("✂️", "یہاں سے کاٹیں", true, Modifier.weight(1.5f)) { splitAtPlayhead() }
                    CutButton("⬅️", "بائیں حذف", false, Modifier.weight(1f)) { trimAtPlayhead(true) }
                    CutButton("➡️", "دائیں حذف", false, Modifier.weight(1f)) { trimAtPlayhead(false) }
                    CutButton("🗑️", "کلپ حذف", false, Modifier.weight(1f)) { deleteSelected() }
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.ToolBar(ed: EditorCtx) {
    with(ed) {
        // نیچے کی ٹول بار: ایک ہی قطار، انگلی سے دائیں بائیں سرکائیں
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14112B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .height(66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF241F4A)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(cat.icon, fontSize = 16.sp, color = Gold)
                    Text(cat.label, fontSize = 9.sp, color = Gold, maxLines = 1)
                }
                cat.tools.forEach { tool ->
                    SmallTool(tool, isActive(tool)) { onToolClick(tool) }
                }
            }
        }
    }
}

