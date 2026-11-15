@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableApi::class)
package com.rahgeer.videoeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop

@Composable
internal fun SmallTool(tool: Tool, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(66.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Accent else Color.Transparent)
            .clickable { onClick() }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            tool.icon,
            fontSize = 20.sp,
            color = if (tool.ready) TextMain else Color(0x66FFFFFF)
        )
        Text(
            tool.label,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = if (tool.ready) TextMain else TextDim,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (tool.ready) {
            Text("⭐", fontSize = 8.sp, color = Gold)
        }
    }
}

@Composable
internal fun ClipBlock(
    item: Clip,
    selected: Boolean,
    widthDp: Dp,
    index: Int,
    onClick: () -> Unit,
    onTrimStart: (Float) -> Unit,
    onTrimEnd: (Float) -> Unit,
    onTrimDone: () -> Unit
) {
    val startCb by rememberUpdatedState(onTrimStart)
    val endCb by rememberUpdatedState(onTrimEnd)
    val doneCb by rememberUpdatedState(onTrimDone)
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(
                if (selected) 2.dp else 0.5.dp,
                if (selected) Gold else Color(0x55FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        val n = maxOf(1, (widthDp.value / 56f).toInt())
        if (item.thumbs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                for (k in 0 until n) {
                    val frac = item.startFrac + (k + 0.5f) / n * (item.endFrac - item.startFrac)
                    val idx = (frac * item.thumbs.size).toInt().coerceIn(0, item.thumbs.size - 1)
                    Image(
                        bitmap = item.thumbs[idx],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        Text(
            "${index + 1}",
            color = TextMain,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(2.dp)
                .background(Color(0xAA000000), RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp)
        )
        Text(
            formatTime(item.trimmedMs),
            color = TextMain,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(2.dp)
                .background(Color(0xAA000000), RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(Gold)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { doneCb() },
                            onDragCancel = { doneCb() }
                        ) { change, dragAmount ->
                            change.consume()
                            startCb(dragAmount)
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(Gold)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { doneCb() },
                            onDragCancel = { doneCb() }
                        ) { change, dragAmount ->
                            change.consume()
                            endCb(dragAmount)
                        }
                    }
            )
        }
    }
}

@Composable
internal fun SettingRow(
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
                    .background(if (on) Gold else Color(0xFF2A2450))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
internal fun ChipRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
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
internal fun LabeledSlider(
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
internal fun SmallAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = TextMain,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2A2450))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
internal fun StageRow(state: Int, text: String) {
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

@Composable
internal fun CutButton(
    icon: String,
    label: String,
    primary: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primary) Accent else Color(0xFF2A2450))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, color = TextMain)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = TextMain, maxLines = 1)
    }
}

