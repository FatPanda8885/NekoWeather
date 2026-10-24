package com.neko.weather.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neko.weather.data.AppTimeZone
import com.neko.weather.data.TimeZoneSection
import com.neko.weather.data.formatUtcOffset
import com.neko.weather.ui.MainUiState
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.Text
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ClockSize = 148.dp

/**
 * 时区二级页：搜索 + 每秒跳动的模拟时钟 + 「跟随系统」置顶 + 按 Region 分组的
 * 可折叠时区列表（参照 animeko 时区设置页）。空偏好代表跟随系统。
 */
@Composable
internal fun TimeZoneSettingsPage(
    state: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val prefId = state.displaySettings.timeZoneId
    val systemZone = remember { ZoneId.systemDefault() }
    val sections = remember { AppTimeZone.catalog(systemZoneId = systemZone.id) }
    var query by rememberSaveable { mutableStateOf("") }
    var collapsedRegions by rememberSaveable(stateSaver = CollapsedRegionsSaver) { mutableStateOf(emptySet<String>()) }
    val visibleSections = AppTimeZone.filterSections(sections, query)
    val selectedZone = prefId.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    val displayZone = selectedZone ?: systemZone
    val select: (String) -> Unit = { zoneId ->
        viewModel.updateDisplaySettings(state.displaySettings.copy(timeZoneId = zoneId))
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("搜索时区、城市或 UTC 偏移") },
            shape = CircleShape,
        )
        Text(
            text = if (selectedZone == null) {
                "当前：跟随系统（${systemZone.id} · ${AppTimeZone.offsetLabel(systemZone)}）"
            } else {
                "当前：${selectedZone.id} · ${AppTimeZone.offsetLabel(selectedZone)}"
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ClockArea(displayZone, Modifier.fillMaxWidth().padding(vertical = 20.dp))
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item(key = "follow-system") {
                TimeZoneRow(
                    title = "跟随系统",
                    description = "${systemZone.id} · ${AppTimeZone.offsetLabel(systemZone)}",
                    offsetLabel = AppTimeZone.offsetLabel(systemZone),
                    selected = selectedZone == null,
                    onClick = { select("") },
                    showDivider = true,
                )
            }
            visibleSections.forEach { section ->
                val expanded = section.region !in collapsedRegions || query.isNotBlank()
                item(key = "header:${section.region}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                collapsedRegions = if (section.region in collapsedRegions) {
                                    collapsedRegions - section.region
                                } else {
                                    collapsedRegions + section.region
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${section.region}（${section.options.size}）",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "折叠" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (expanded) {
                    items(section.options.size, key = { section.options[it].zoneId }) { index ->
                        val option = section.options[index]
                        TimeZoneRow(
                            title = option.city.ifBlank { option.zoneId },
                            description = option.zoneId,
                            offsetLabel = option.offsetLabel,
                            selected = selectedZone?.id == option.zoneId,
                            onClick = { select(option.zoneId) },
                            showDivider = index != section.options.lastIndex,
                        )
                    }
                }
            }
            if (query.isNotBlank() && visibleSections.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        text = "没有匹配「$query」的时区",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeZoneRow(
    title: String,
    description: String,
    offsetLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = offsetLabel,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDivider) HorizontalDivider(Modifier.padding(start = 16.dp))
    }
}

/** 时钟区：模拟表盘 + 数字时间与星期。 */
@Composable
private fun ClockArea(zone: ZoneId, modifier: Modifier = Modifier) {
    val time by rememberTickingTime(zone)
    val date = remember(time) { LocalDate.now(zone) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnalogClock(time = time, size = ClockSize)
        Spacer(Modifier.size(10.dp))
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        val dateText = "%04d-%02d-%02d · ".format(date.year, date.monthValue, date.dayOfMonth) +
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        Text(
            text = dateText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 每秒刷新一次的所选时区时间。 */
@Composable
private fun rememberTickingTime(zone: ZoneId): androidx.compose.runtime.State<LocalTime> {
    val time = remember(zone) { mutableStateOf(LocalTime.now(zone)) }
    LaunchedEffect(zone) {
        while (true) {
            delay(1000)
            time.value = LocalTime.now(zone)
        }
    }
    return time
}

/**
 * 极简模拟时钟：圆形表盘、四向刻度点、时/分/秒三针。指针由补间动画驱动，
 * 切换时区导致的时间跳变表现为指针旋转过去而非瞬间换位。
 */
@Composable
private fun AnalogClock(time: LocalTime, size: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val hourAngle = rememberAnimatedHandAngle((time.hour % 12 + time.minute / 60f) * 30f).value
    val minuteAngle = rememberAnimatedHandAngle(time.minute * 6f).value
    val secondAngle = rememberAnimatedHandAngle(time.second * 6f).value
    Canvas(modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(colors.surfaceVariant, radius = radius)
        drawCircle(colors.outlineVariant.copy(alpha = 0.5f), radius = radius, style = Stroke(width = 2f))
        // 12/3/6/9 四个刻度点
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            rotate(angle, center) {
                drawCircle(colors.onSurfaceVariant, radius = radius * 0.035f, center = Offset(center.x, center.y - radius * 0.86f))
            }
        }
        fun hand(angle: Float, lengthFraction: Float, width: Float, color: androidx.compose.ui.graphics.Color, cap: StrokeCap = StrokeCap.Round) {
            rotate(angle, center) {
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - radius * lengthFraction),
                    strokeWidth = width,
                    cap = cap,
                )
            }
        }
        hand(hourAngle, 0.5f, radius * 0.07f, colors.onSurface)
        hand(minuteAngle, 0.72f, radius * 0.05f, colors.onSurface)
        hand(secondAngle, 0.82f, radius * 0.02f, colors.primary)
        drawCircle(colors.primary, radius = radius * 0.04f, center = center)
    }
}

/** 指针角度补间：沿最短弧转向目标，避免 360→0 回卷时倒转一整圈。 */
@Composable
private fun rememberAnimatedHandAngle(target: Float): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val animatable = remember { Animatable(target) }
    LaunchedEffect(target) {
        val current = animatable.value
        val delta = (target - current) % 360f
        val nearest = if (delta > 180f) delta - 360f else if (delta < -180f) delta + 360f else delta
        animatable.animateTo(current + nearest, tween(durationMillis = 550, easing = FastOutSlowInEasing))
    }
    return animatable
}

private val CollapsedRegionsSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)
