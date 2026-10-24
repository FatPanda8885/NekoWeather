package com.neko.weather.ui.settings

import android.Manifest
import android.os.Build
import android.content.Context
import android.icu.util.TimeZone as IcuTimeZone
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neko.weather.data.AppTimeZone
import com.neko.weather.data.DeviceLocationService
import com.neko.weather.ui.MainUiState
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val ClockSize = 148.dp
private val CollapsedClockSize = 72.dp
private val ClockCollapseThreshold = 48.dp
private val ClockVerticalPadding = 20.dp
private val CollapsedClockVerticalPadding = 8.dp
private const val CLOCK_COLLAPSE_MILLIS = 350

/** 时/分针补间时长。 */
private const val HAND_ANIMATION_MILLIS = 700

/** 秒针补间时长：比一秒略短，让相邻两次跳动之间留出静止的一小段。 */
private const val SECOND_HAND_ANIMATION_MILLIS = 900

/**
 * 时区二级页（参照 animeko 时区设置页）：搜索 + 每秒跳动的模拟时钟（列表滚动时
 * 两段式折叠）+ 定位选择时区 + 「跟随系统」置顶 + 按 Region 分组的可折叠列表。
 * 空偏好代表跟随系统。
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

    // ---- 定位选择时区 ----
    val context = LocalContext.current
    val locationService = remember { DeviceLocationService(context) }
    val scope = rememberCoroutineScope()
    var locateState by remember { mutableStateOf<LocateState>(LocateState.Idle) }

    fun startLocate() {
        locateState = LocateState.Locating
        scope.launch {
            val location = runCatching { locationService.currentLocation() }.getOrElse { error ->
                locateState = LocateState.Result(
                    LocateOutcome.Failure(LocateFailureKind.LOCATE_FAILED, canRetry = error !is IllegalStateException),
                )
                return@launch
            }
            val zoneId = withContext(Dispatchers.IO) { resolveZoneByLocation(context, location) }
            locateState = LocateState.Result(
                if (zoneId != null) LocateOutcome.Success(zoneId)
                else LocateOutcome.Failure(LocateFailureKind.ZONE_NOT_FOUND, canRetry = false),
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startLocate()
        } else {
            locateState = LocateState.Result(LocateOutcome.Failure(LocateFailureKind.PERMISSION_DENIED, canRetry = true))
        }
    }
    val onLocateClicked: () -> Unit = {
        when (val current = locateState) {
            LocateState.Locating -> Unit
            is LocateState.Result -> when (val outcome = current.outcome) {
                is LocateOutcome.Success -> select(outcome.zoneId)
                is LocateOutcome.Failure ->
                    if (locationService.hasPermission()) startLocate()
                    else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            LocateState.Idle ->
                if (locationService.hasPermission()) startLocate()
                else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // ---- 时钟两段式折叠：列表上滑越过阈值即折叠，回到顶部后继续下拉才恢复 ----
    val listState = rememberLazyListState()
    var clockCollapsed by rememberSaveable { mutableStateOf(false) }
    val collapseThresholdPx = with(LocalDensity.current) { ClockCollapseThreshold.toPx() }
    LaunchedEffect(listState, collapseThresholdPx) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }.filter { it }.collect { clockCollapsed = true }
    }
    val collapseConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只观察不消费，列表与顶栏照常处理这次滚动
                if (source == NestedScrollSource.UserInput && available.y > 0f && !listState.canScrollBackward) {
                    clockCollapsed = false
                }
                return Offset.Zero
            }
        }
    }
    // 一旦越过阈值就补间到终点，不存在停在中间的表盘
    val collapseFraction by animateFloatAsState(
        targetValue = if (clockCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = CLOCK_COLLAPSE_MILLIS, easing = FastOutSlowInEasing),
        label = "timeZoneClockCollapse",
    )

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
        ClockArea(
            zone = displayZone,
            collapseFraction = collapseFraction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ClockVerticalPadding + (CollapsedClockVerticalPadding - ClockVerticalPadding) * collapseFraction),
        )
        LocateTimeZoneBox(
            state = locateState,
            onClick = onLocateClicked,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        )
        HorizontalDivider()
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(collapseConnection),
            state = listState,
        ) {
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

/** 时钟区：模拟表盘 + 数字时间与星期；随折叠程度收缩尺寸。 */
@Composable
private fun ClockArea(zone: ZoneId, collapseFraction: Float, modifier: Modifier = Modifier) {
    val time by rememberTickingTime(zone)
    val date = remember(time) { LocalDate.now(zone) }
    val clockSize = ClockSize + (CollapsedClockSize - ClockSize) * collapseFraction
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnalogClock(time = time, size = clockSize)
        Spacer(Modifier.size(10.dp + (6.dp - 10.dp) * collapseFraction))
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            fontSize = (22 + (15 - 22) * collapseFraction).sp,
            fontWeight = FontWeight.SemiBold,
        )
        AnimatedVisibility(visible = collapseFraction < 0.5f) {
            val dateText = "%04d-%02d-%02d · ".format(date.year, date.monthValue, date.dayOfMonth) +
                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    val secondAngle = rememberAnimatedHandAngle(time.second * 6f, SECOND_HAND_ANIMATION_MILLIS).value
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
        fun hand(angle: Float, lengthFraction: Float, width: Float, color: Color, cap: StrokeCap = StrokeCap.Round) {
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
private fun rememberAnimatedHandAngle(
    target: Float,
    durationMillis: Int = HAND_ANIMATION_MILLIS,
): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val animatable = remember { Animatable(target) }
    LaunchedEffect(target) {
        val current = animatable.value
        val delta = (target - current) % 360f
        val nearest = if (delta > 180f) delta - 360f else if (delta < -180f) delta + 360f else delta
        animatable.animateTo(current + nearest, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
    }
    return animatable
}

// ---- 定位选择时区 ----

private sealed interface LocateState {
    data object Idle : LocateState
    data object Locating : LocateState
    data class Result(val outcome: LocateOutcome) : LocateState
}

private sealed interface LocateOutcome {
    data class Success(val zoneId: String) : LocateOutcome
    data class Failure(val kind: LocateFailureKind, val canRetry: Boolean) : LocateOutcome
}

private enum class LocateFailureKind { PERMISSION_DENIED, LOCATE_FAILED, ZONE_NOT_FOUND }

/** 定位框：空闲提示、定位中转圈、成功显示时区（点击应用）、失败提示（可重试）。 */
@Composable
private fun LocateTimeZoneBox(state: LocateState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val outcome = (state as? LocateState.Result)?.outcome
    val label = when {
        state == LocateState.Locating -> "正在定位…"
        outcome is LocateOutcome.Success -> "已定位：${outcome.zoneId}"
        outcome is LocateOutcome.Failure -> when (outcome.kind) {
            LocateFailureKind.PERMISSION_DENIED -> "定位权限被拒绝，点击重试"
            LocateFailureKind.LOCATE_FAILED -> "定位失败，点击重试"
            LocateFailureKind.ZONE_NOT_FOUND -> "未找到匹配的时区"
        }
        else -> "使用定位选择时区"
    }
    val enabled = state != LocateState.Locating &&
        (outcome?.let { it is LocateOutcome.Success || (it is LocateOutcome.Failure && it.canRetry) } ?: true)
    val containerColor = if (outcome is LocateOutcome.Success) scheme.primaryContainer else scheme.surfaceContainerHighest
    val contentColor = when (outcome) {
        is LocateOutcome.Success -> scheme.onPrimaryContainer
        is LocateOutcome.Failure -> scheme.error
        else -> scheme.onSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = if (outcome == null) scheme.primary else contentColor)
        Spacer(Modifier.width(12.dp))
        if (state == LocateState.Locating) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = scheme.primary)
            Spacer(Modifier.width(8.dp))
        }
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = contentColor)
    }
}

/**
 * 由坐标匹配 IANA 时区 ID（参照 animeko）：先反查国家/地区，再在该地区的规范时区中
 * 优先取设备当前时区，否则取 UTC 偏移与经度推算值最近的时区。
 */
@Suppress("DEPRECATION") // 同步 Geocoder 在 API 33 起废弃，异步版本仅 API 33+ 存在
private fun resolveZoneByLocation(context: Context, location: Location): String? {
    if (!Geocoder.isPresent()) return null
    val region = runCatching {
        Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()?.countryCode
    }.getOrNull()?.takeIf(String::isNotBlank) ?: return null
    // CANONICAL_LOCATION 筛选需要 API 28；更早版本退回该国家的全部时区（含别名）
    val ids: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            IcuTimeZone.getAvailableIDs(
                IcuTimeZone.SystemTimeZoneType.CANONICAL_LOCATION,
                region.uppercase(Locale.ROOT),
                null as Int?,
            )
        }.getOrNull()?.toList()
    } else {
        runCatching {
            @Suppress("DEPRECATION")
            IcuTimeZone.getAvailableIDs(region.uppercase(Locale.ROOT))
        }.getOrNull()?.toList()
    } ?: emptyList()
    val candidates = ids.map(IcuTimeZone::getTimeZone)
    if (candidates.isEmpty()) return null
    val deviceZoneId = ZoneId.systemDefault().id
    candidates.firstOrNull { it.id == deviceZoneId }?.let { return it.id }
    // 经度每 15 度对应一个时区，用推算偏移挑最近的候选
    val estimatedOffsetMillis = (location.longitude / 15.0 * 3_600_000).toInt()
    return candidates.minByOrNull { abs(it.rawOffset - estimatedOffsetMillis) }?.id
}

private val CollapsedRegionsSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)
