package com.neko.weather.ui.settings

import android.Manifest
import android.content.Context
import android.icu.util.TimeZone as IcuTimeZone
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.sp
import com.neko.weather.data.AppTimeZone
import com.neko.weather.data.DeviceLocationService
import com.neko.weather.model.AppLanguage
import com.neko.weather.ui.LocalAppLanguage
import com.neko.weather.ui.MainUiState
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 竖屏与宽屏时钟布局的宽度分界.
 */
private val ClockLayoutWidthThreshold = 600.dp
private val ClockSize = 168.dp
private val ItemHorizontalPadding = 16.dp

/**
 * 窄屏时钟头部折叠后的表盘尺寸.
 */
private val CollapsedClockSize = 72.dp

/**
 * 列表上滑超过该距离后, 窄屏时钟头部进入折叠态.
 */
private val ClockCollapseThreshold = 48.dp

/**
 * 时钟头部在展开与折叠之间的变形时长.
 */
private const val ClockCollapseDurationMillis = 300

/**
 * 窄屏展开态里表盘与日期、日期与时间之间的间距.
 */
private val ClockTextGap = 12.dp

/**
 * 宽屏时钟区右列里日期与时间之间的间距.
 */
private val ClockTimeGap = 8.dp

/**
 * 宽屏时钟区右列里表盘与文字之间的间距.
 */
private val ClockColumnSpacing = 32.dp

/**
 * 宽屏右列时间的可用高度下限, 相对表盘高度. 极端文字缩放下仍保证数字可见.
 */
private const val ClockTimeMinHeightFraction = 0.4f

/**
 * 宽屏右列时间字号的收缩余量, 避免亚像素误差让行盒比可用高度多出一点.
 */
private const val ClockTimeSafetyScale = 0.995f

/**
 * 折叠的分组在重组与配置变更后保持状态; 不写入设置, 重启页面即恢复全部分组展开.
 */
private val CollapsedRegionsSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

/**
 * 时区设置二级页（UI 与动画移植自 animeko TimeZoneSettingsPage）.
 *
 * 页面自上而下是搜索框、「当前时区」小字、时钟、定位框与时区列表. 列表自带 [LazyColumn],
 * 因此本页面要放在不接受外层滚动的容器里.
 *
 * 显示用的时区按所选偏好解析（空串跟随系统）, 写入只改 [com.neko.weather.model.DisplaySettings.timeZoneId].
 */
@Composable
internal fun TimeZoneSettingsPage(
    state: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val prefId = state.displaySettings.timeZoneId
    val selectedTimeZoneId: String? = prefId.takeIf { it.isNotBlank() }
    val displayZone = remember(prefId) { AppTimeZone.resolve(prefId) }
    val systemTimeZone = remember { ZoneId.systemDefault() }
    // 目录只用于排序与展示偏移, 打开页面时取一次, 避免每秒重算全部时区
    val catalogNow = remember { Instant.now() }
    val sections = remember(catalogNow, systemTimeZone) { AppTimeZone.catalog(catalogNow, systemTimeZone.id) }
    var query by rememberSaveable { mutableStateOf("") }
    var collapsedRegions by rememberSaveable(stateSaver = CollapsedRegionsSaver) { mutableStateOf(emptySet<String>()) }

    val now = rememberTickingInstant()
    val localDateTime = remember(now, displayZone) { LocalDateTime.ofInstant(now, displayZone) }

    val systemZoneOffsetLabel = remember(systemTimeZone, catalogNow) { AppTimeZone.offsetLabel(systemTimeZone, catalogNow) }
    val selectedZone = selectedTimeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    val currentZoneLabel = if (selectedZone == null) {
        "跟随系统（${systemTimeZone.id}）"
    } else {
        "${selectedZone.id}（${AppTimeZone.offsetLabel(selectedZone, catalogNow)}）"
    }
    val selectedPinLabel = selectedZone?.let { "${it.id}（${AppTimeZone.offsetLabel(it, catalogNow)}）" }
    val followSystemRowLabel = "跟随系统时区（${systemTimeZone.id} · $systemZoneOffsetLabel）"

    val rows = remember(
        sections,
        query,
        selectedTimeZoneId,
        selectedPinLabel,
        followSystemRowLabel,
        collapsedRegions,
    ) {
        buildTimeZoneRows(
            sections = AppTimeZone.filterSections(sections, query),
            query = query,
            collapsedRegions = collapsedRegions,
            selectedTimeZoneId = selectedTimeZoneId,
            selectedPinLabel = selectedPinLabel,
            systemZoneId = systemTimeZone.id,
            followSystemRowLabel = followSystemRowLabel,
        )
    }

    val selectTimeZone: (String?) -> Unit = { zoneId ->
        viewModel.updateDisplaySettings(state.displaySettings.copy(timeZoneId = zoneId.orEmpty()))
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
                    LocationTimeZoneUiState.Failure(
                        LocationFailureKind.LocateFailed,
                        canRetry = error !is IllegalStateException,
                    ),
                )
                return@launch
            }
            val zoneId = withContext(Dispatchers.IO) { resolveZoneByLocation(context, location) }
            locateState = LocateState.Result(
                if (zoneId != null) LocationTimeZoneUiState.Success(zoneId)
                else LocationTimeZoneUiState.Failure(LocationFailureKind.ZoneNotFound, canRetry = false),
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startLocate()
        } else {
            locateState = LocateState.Result(
                LocationTimeZoneUiState.Failure(LocationFailureKind.PermissionDenied, canRetry = true),
            )
        }
    }
    val onLocate: () -> Unit = {
        if (locationService.hasPermission()) {
            startLocate()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // ---- 窄屏时钟头部的两段式折叠: 列表上滑越过阈值即折叠, 回到顶部后继续下拉才恢复 ----
    val listState = rememberLazyListState()
    var clockCollapsed by rememberSaveable { mutableStateOf(false) }
    val collapseThresholdPx = with(LocalDensity.current) { ClockCollapseThreshold.toPx() }
    LaunchedEffect(listState, collapseThresholdPx) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }.filter { it }.collect { clockCollapsed = true }
    }
    val clockCollapseScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只观察不消费, 列表与顶栏照常处理这次滚动
                if (source == NestedScrollSource.UserInput && available.y > 0f && !listState.canScrollBackward) {
                    clockCollapsed = false
                }
                return Offset.Zero
            }
        }
    }
    // 一旦越过阈值就补间到终点, 不存在停在中间的表盘
    val clockCollapsedFraction by animateFloatAsState(
        targetValue = if (clockCollapsed) 1f else 0f,
        animationSpec = tween(durationMillis = ClockCollapseDurationMillis, easing = FastOutSlowInEasing),
        label = "timeZoneClockCollapse",
    )

    Column(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // 窄屏的搜索框与两侧留白对齐; 宽屏保持满宽, 与时钟区、分割线同边
            val searchHorizontalPadding =
                if (maxWidth < ClockLayoutWidthThreshold) ItemHorizontalPadding else 0.dp
            TimeZoneSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = searchHorizontalPadding, vertical = 8.dp),
            )
        }
        Text(
            text = "当前时区：$currentZoneLabel",
            modifier = Modifier.padding(horizontal = ItemHorizontalPadding),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TimeZoneClockArea(
            dateTime = localDateTime,
            collapsedFraction = clockCollapsedFraction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )
        LocationTimeZoneBox(
            locateState = locateState,
            onLocate = onLocate,
            onApply = { zoneId -> selectTimeZone(zoneId) },
            modifier = Modifier
                .padding(horizontal = ItemHorizontalPadding)
                .padding(bottom = 24.dp),
        )
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(clockCollapseScrollConnection),
            state = listState,
        ) {
            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                val showDivider = index != rows.lastIndex
                when (row) {
                    is TimeZoneListRow.SectionHeader -> TimeZoneSectionHeader(
                        header = row,
                        showDivider = showDivider,
                        onToggleExpand = {
                            collapsedRegions = if (row.region in collapsedRegions) {
                                collapsedRegions - row.region
                            } else {
                                collapsedRegions + row.region
                            }
                        },
                    )

                    is TimeZoneListRow.Entry -> TimeZoneEntryRow(
                        row = row,
                        showDivider = showDivider,
                        onClick = { selectTimeZone(row.selectedZoneId) },
                    )
                }
            }
        }
    }
}

/**
 * 定位框的请求状态. 只有 [Result] 保存定位结果, 请求进行中与未请求是两个独立状态.
 */
private sealed interface LocateState {
    data object Idle : LocateState
    data object Locating : LocateState
    data class Result(val state: LocationTimeZoneUiState) : LocateState
}

/**
 * 定位框可直接绑定的终态.
 */
private sealed interface LocationTimeZoneUiState {
    data class Success(val zoneId: String) : LocationTimeZoneUiState
    data class Failure(val kind: LocationFailureKind, val canRetry: Boolean) : LocationTimeZoneUiState
}

private enum class LocationFailureKind { PermissionDenied, LocateFailed, ZoneNotFound }

private sealed interface TimeZoneListRow {
    val key: String

    /**
     * 分组标题. [expanded] 为 false 时该组只保留标题行.
     */
    data class SectionHeader(val region: String, val expanded: Boolean) : TimeZoneListRow {
        override val key: String get() = "header:$region"
    }

    /**
     * @param selectedZoneId 点击后写入的值. 「跟随系统」为 null.
     * @param label 置顶项的完整文案; 分组项为 null, 直接显示时区 ID.
     */
    data class Entry(
        override val key: String,
        val selectedZoneId: String?,
        val zoneId: String,
        val offsetLabel: String,
        val selected: Boolean,
        val pinned: Boolean,
        val label: String? = null,
    ) : TimeZoneListRow
}

/**
 * 置顶区在前 (选中时区、跟随系统), 之后是分组列表. 置顶项与时区项都参与 [query] 过滤.
 *
 * 置顶区不属于任何分组, 不受 [collapsedRegions] 影响; [query] 非空时忽略折叠状态, 强制展开每个分组.
 */
private fun buildTimeZoneRows(
    sections: List<com.neko.weather.data.TimeZoneSection>,
    query: String,
    collapsedRegions: Set<String>,
    selectedTimeZoneId: String?,
    selectedPinLabel: String?,
    systemZoneId: String,
    followSystemRowLabel: String,
): List<TimeZoneListRow> {
    val rows = mutableListOf<TimeZoneListRow>()

    if (selectedTimeZoneId != null && selectedPinLabel != null &&
        timeZoneQueryMatches(query, selectedPinLabel, selectedTimeZoneId)
    ) {
        rows += TimeZoneListRow.Entry(
            key = "pin:selected",
            selectedZoneId = selectedTimeZoneId,
            zoneId = selectedTimeZoneId,
            offsetLabel = "",
            selected = true,
            pinned = true,
            label = selectedPinLabel,
        )
    }
    if (timeZoneQueryMatches(query, followSystemRowLabel, systemZoneId)) {
        rows += TimeZoneListRow.Entry(
            key = "pin:follow-system",
            selectedZoneId = null,
            zoneId = systemZoneId,
            offsetLabel = "",
            selected = selectedTimeZoneId == null,
            pinned = true,
            label = followSystemRowLabel,
        )
    }

    val forceExpand = query.isNotBlank()
    for (section in sections) {
        val expanded = forceExpand || section.region !in collapsedRegions
        rows += TimeZoneListRow.SectionHeader(section.region, expanded)
        if (!expanded) continue
        for (option in section.options) {
            rows += TimeZoneListRow.Entry(
                key = "zone:${option.zoneId}",
                selectedZoneId = option.zoneId,
                zoneId = option.zoneId,
                offsetLabel = option.offsetLabel,
                selected = option.zoneId == selectedTimeZoneId,
                pinned = false,
            )
        }
    }
    return rows
}

/**
 * [query] 是否命中 [fields] 中的任意一项. 忽略大小写与首尾空白; 空白查询命中一切.
 */
private fun timeZoneQueryMatches(query: String, vararg fields: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return fields.any { it.contains(trimmed, ignoreCase = true) }
}

@Composable
private fun rememberTickingInstant(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = Instant.now()
        }
    }
    return now
}

@Composable
private fun TimeZoneSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索时区") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/**
 * 时钟区. 宽度小于 [ClockLayoutWidthThreshold] 时, 表盘、日期与时间在展开态里纵向居中排列,
 * 在折叠态里排成左上角小表盘加右侧上下两行, 两种状态之间由 [collapsedFraction] 连续插值.
 * 宽度足够时表盘在左, 右侧一列等高排布日期与时间, 两行的行高受表盘高度约束,
 * 因此始终不重叠也不越出表盘.
 */
@Composable
private fun TimeZoneClockArea(
    dateTime: LocalDateTime,
    collapsedFraction: Float,
    modifier: Modifier = Modifier,
) {
    val dateLabel = clockDateLabel(dateTime)
    val weekdayLabel = clockWeekdayLabel(dateTime.dayOfWeek)
    val dateWeekdayLabel = "$dateLabel $weekdayLabel"
    val timeLabel = formatClockTime(dateTime)
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier) {
        if (maxWidth < ClockLayoutWidthThreshold) {
            NarrowClockArea(
                time = dateTime.toLocalTime(),
                dateText = dateWeekdayLabel,
                timeText = timeLabel,
                secondaryColor = secondaryColor,
                collapsedFraction = collapsedFraction,
                availableWidth = maxWidth,
            )
        } else {
            val dateStyle = MaterialTheme.typography.bodyMedium
            // 两行共用表盘高度: 日期贴表盘顶边, 时间贴表盘底边. 时间一行的字号由可用高度换算,
            // 因此数字占满日期下方到表盘底边之间的空间; 右列放不下时再按宽度收缩.
            val density = LocalDensity.current
            val dateHeight = with(density) { dateStyle.lineHeight.toDp() }
            val timeHeight = (ClockSize - dateHeight - ClockTimeGap)
                .coerceAtLeast(ClockSize * ClockTimeMinHeightFraction)
            val wideTimeStyle = rememberClockTimeStyle(
                text = timeLabel,
                height = timeHeight,
                width = maxWidth - ClockSize - ClockColumnSpacing,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                AnalogClock(
                    time = dateTime.toLocalTime(),
                    size = ClockSize,
                )
                Spacer(Modifier.width(ClockColumnSpacing))
                Column(
                    Modifier.height(ClockSize).weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = dateWeekdayLabel,
                        style = dateStyle,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = timeLabel,
                        style = wideTimeStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 窄屏时钟区. [collapsedFraction] 为 0 时表盘居中、日期与时间依次排在其下; 为 1 时表盘缩到左上角,
 * 日期与时间变成表盘右侧的上下两行, 两行的行高合计等于表盘高度.
 *
 * 三个元素都按插值后的位置摆放, 因此变形过程中表盘与文字不会停在中间状态.
 */
@Composable
private fun NarrowClockArea(
    time: LocalTime,
    dateText: String,
    timeText: String,
    secondaryColor: Color,
    collapsedFraction: Float,
    availableWidth: Dp,
) {
    val dateStyle = MaterialTheme.typography.bodyMedium
    val expandedTimeStyle = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")
    val dateHeight = with(LocalDensity.current) { dateStyle.lineHeight.toDp() }
    // 折叠后两行合计占满表盘高度: 时间一行吃掉日期之外的全部空间.
    val collapsedTimeHeight = (CollapsedClockSize - dateHeight)
        .coerceAtLeast(CollapsedClockSize * ClockTimeMinHeightFraction)
    val collapsedTimeStyle = rememberClockTimeStyle(
        text = timeText,
        height = collapsedTimeHeight,
        width = availableWidth - CollapsedClockSize - ClockColumnSpacing,
    )
    val timeStyle = expandedTimeStyle.lerpTo(collapsedTimeStyle, collapsedFraction)
    val clockSize = lerp(ClockSize, CollapsedClockSize, collapsedFraction)

    Layout(
        content = {
            AnalogClock(
                time = time,
                size = clockSize,
            )
            Text(
                text = dateText,
                style = dateStyle,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeText,
                style = timeStyle,
                maxLines = 1,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val clock = measurables[0].measure(Constraints())
        val date = measurables[1].measure(Constraints(maxWidth = width))
        val time = measurables[2].measure(Constraints(maxWidth = width))

        val fraction = collapsedFraction.coerceIn(0f, 1f)
        // 抬升晚于收缩: 日期与时间先跟着表盘底边留在下方, 等表盘缩小、右列让出横向空间后再上移,
        // 变形过程中文字不会从表盘上滑过.
        val lift = fraction * fraction
        val textGap = ClockTextGap.toPx()
        val clockBottom = clock.height.toFloat()
        val centeredLeft = ((width - ClockSize.toPx()) / 2f) * (1f - fraction)
        // 折叠完成时表盘左端与「当前时区」文案对齐; 日期与时间的左边界保持折叠前的位置.
        val clockLeft = centeredLeft + ItemHorizontalPadding.toPx() * fraction
        val columnLeft = centeredLeft + clock.width + ClockColumnSpacing.toPx()
        val dateTop = lerp(clockBottom + textGap, 0f, lift)
        val timeTop = lerp(dateTop + date.height + textGap, clockBottom - time.height, lift)
        val dateLeft = lerp((width - date.width) / 2f, columnLeft, fraction)
        val timeLeft = lerp((width - time.width) / 2f, columnLeft, fraction)
        val height = maxOf(clockBottom, dateTop + date.height, timeTop + time.height).roundToInt()

        layout(width, height) {
            clock.place(clockLeft.roundToInt(), 0)
            date.place(dateLeft.roundToInt(), dateTop.roundToInt())
            time.place(timeLeft.roundToInt(), timeTop.roundToInt())
        }
    }
}

/** 日期文案：中文「10月24日」，英文「10/24」，其余按各自习惯。 */
@Composable
private fun clockDateLabel(dateTime: LocalDateTime): String {
    return when (LocalAppLanguage.current) {
        AppLanguage.ENGLISH -> "${dateTime.monthValue}/${dateTime.dayOfMonth}"
        else -> "${dateTime.monthValue}月${dateTime.dayOfMonth}日"
    }
}

/** 星期文案：中文「周一」，英文「Mon」，日文「月」。 */
@Composable
private fun clockWeekdayLabel(dayOfWeek: DayOfWeek): String {
    val language = LocalAppLanguage.current
    val single = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
    return when (language) {
        AppLanguage.ENGLISH -> dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        AppLanguage.JAPANESE -> dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.JAPANESE)
        else -> "周$single"
    }
}

private fun formatClockTime(dateTime: LocalDateTime): String = buildString(8) {
    append(dateTime.hour.toString().padStart(2, '0'))
    append(':')
    append(dateTime.minute.toString().padStart(2, '0'))
    append(':')
    append(dateTime.second.toString().padStart(2, '0'))
}

/**
 * 宽屏右列时间一行的样式: 字号按 [height] 定, 使一整行（含字体自身的上下留白）正好占满 [height];
 * [width] 放不下这一行时再按同一比例缩小, 保证不横向溢出, 也不换行.
 *
 * 行盒高度与文字宽度都与字号成正比, 因此先按参考字号量一次, 再按比例换算即可一步到位.
 */
@Composable
private fun rememberClockTimeStyle(
    text: String,
    height: Dp,
    width: Dp,
): TextStyle {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.displaySmall
    return remember(text, height, width, baseStyle, density, textMeasurer) {
        val referenceSize = with(density) { height.toSp() }
        val style = baseStyle.copy(
            fontSize = referenceSize,
            lineHeight = referenceSize,
            fontFeatureSettings = "tnum",
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
        val measured = textMeasurer.measure(text, style).size
        val heightScale = if (measured.height == 0) 1f else with(density) { height.toPx() } / measured.height
        val widthScale = if (measured.width == 0) 1f else with(density) { width.toPx() } / measured.width
        // 留一点余量: 行盒若比可用高度多出不足一像素, 列布局会把它压扁并吃掉间距
        val scale = minOf(heightScale, widthScale) * ClockTimeSafetyScale
        style.copy(fontSize = referenceSize * scale, lineHeight = referenceSize * scale)
    }
}

/**
 * 在 [this] 与 [target] 之间按 [fraction] 插值字号与行高, 其余样式沿用 [this].
 *
 * 两个样式的字号都是同一密度下的 sp, 单位一致, 因此可以直接按数值插值.
 */
private fun TextStyle.lerpTo(target: TextStyle, fraction: Float): TextStyle {
    if (fraction <= 0f) return this
    if (fraction >= 1f) return target
    return copy(
        fontSize = lerpTextUnit(fontSize, target.fontSize, fraction),
        lineHeight = lerpTextUnit(lineHeight, target.lineHeight, fraction),
    )
}

private fun lerpTextUnit(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit =
    (start.value + (stop.value - start.value) * fraction).sp

/**
 * 极简模拟时钟: 圆形表盘、12/3/6/9 四个刻度点, 时/分/秒三针.
 *
 * 三针都由补间动画驱动: 秒针匀速转动且只顺时针前进; 时/分针沿最短弧转向目标角度.
 * 因此切换时区导致的时间跳变表现为指针旋转过去, 而不是瞬间换位.
 *
 * [time] 每秒变化一次.
 */
@Composable
private fun AnalogClock(
    time: LocalTime,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hourAngle = rememberAnimatedHandAngle(
        target = hourHandTargetAngle(time.hour, time.minute),
        animationSpec = tween(durationMillis = HAND_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        advance = ::nearestHandAngle,
    )
    val minuteAngle = rememberAnimatedHandAngle(
        target = minuteHandTargetAngle(time.minute, time.second),
        animationSpec = tween(durationMillis = HAND_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        advance = ::nearestHandAngle,
    )
    val secondAngle = rememberAnimatedHandAngle(
        target = secondHandTargetAngle(time.second, time.nano),
        animationSpec = tween(durationMillis = SECOND_HAND_ANIMATION_MILLIS, easing = LinearEasing),
        advance = ::forwardHandAngle,
    )

    Canvas(modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val dialCenter = this.center
        val rimWidth = radius * 0.04f

        drawCircle(color = colors.surface, radius = radius - rimWidth, center = dialCenter)
        drawCircle(
            color = colors.outline,
            radius = radius - rimWidth,
            center = dialCenter,
            style = Stroke(width = rimWidth),
        )

        val tickRadius = radius * 0.03f
        val tickDistance = radius * 0.76f
        repeat(4) { index ->
            rotate(index * 90f, pivot = dialCenter) {
                drawCircle(
                    color = colors.outline,
                    radius = tickRadius,
                    center = dialCenter - Offset(0f, tickDistance),
                )
            }
        }

        rotate(hourAngle, pivot = dialCenter) {
            drawLine(
                color = colors.onSurface,
                start = dialCenter,
                end = dialCenter - Offset(0f, radius * 0.46f),
                strokeWidth = radius * 0.06f,
                cap = StrokeCap.Round,
            )
        }
        rotate(minuteAngle, pivot = dialCenter) {
            drawLine(
                color = colors.onSurface,
                start = dialCenter,
                end = dialCenter - Offset(0f, radius * 0.68f),
                strokeWidth = radius * 0.045f,
                cap = StrokeCap.Round,
            )
        }
        rotate(secondAngle, pivot = dialCenter) {
            drawLine(
                color = colors.primary,
                start = dialCenter + Offset(0f, radius * 0.12f),
                end = dialCenter - Offset(0f, radius * 0.78f),
                strokeWidth = radius * 0.02f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = colors.primary, radius = radius * 0.035f, center = dialCenter)
    }
}

/**
 * 把指针补间到 [target] 换算出的角度. [advance] 决定从当前角度到目标的走法:
 * 秒针只前进, 时/分针走最短弧.
 *
 * 目标每次变化都从当前动画值重新起步, 所以连续变化不会积累误差, 也不会回跳.
 */
@Composable
private fun rememberAnimatedHandAngle(
    target: Float,
    animationSpec: AnimationSpec<Float>,
    advance: (current: Float, target: Float) -> Float,
): Float {
    val angle = remember { Animatable(target) }
    LaunchedEffect(target) {
        angle.animateTo(advance(angle.value, target), animationSpec)
    }
    return angle.value
}

// ---- 表盘指针角度: 12 点方向为 0 度, 顺时针递增, 一圈 360 度 ----

private const val FULL_TURN = 360f
private const val DEGREES_PER_SECOND = 6f
private const val DEGREES_PER_MINUTE = 6f
private const val DEGREES_PER_HOUR = 30f
private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * 秒针的目标角度, 含 [nanosecond] 表示的不足一秒的偏移.
 */
private fun secondHandTargetAngle(second: Int, nanosecond: Int): Float =
    second * DEGREES_PER_SECOND + nanosecond / NANOS_PER_SECOND * DEGREES_PER_SECOND

/**
 * 分针的目标角度, 含 [second] 表示的不足一分钟的偏移.
 */
private fun minuteHandTargetAngle(minute: Int, second: Int): Float =
    minute * DEGREES_PER_MINUTE + second * (DEGREES_PER_MINUTE / 60f)

/**
 * 时针的目标角度, 含 [minute] 表示的不足一小时的偏移.
 */
private fun hourHandTargetAngle(hour: Int, minute: Int): Float =
    (hour % 12) * DEGREES_PER_HOUR + minute * (DEGREES_PER_HOUR / 60f)

/**
 * 从 [current] 顺时针转到与 [target] 同角度的最近位置, 结果不小于 [current].
 *
 * 秒针只向前走: 59 秒到 0 秒的跳变只前进一格, 不会倒扫一整圈.
 */
private fun forwardHandAngle(current: Float, target: Float): Float =
    current + (target - current).floorMod(FULL_TURN)

/**
 * 从 [current] 转到与 [target] 同角度的最近位置, 位移不超过半圈.
 *
 * 时/分针用它: 时间跳变时沿最短弧转向新角度, 不会多转整圈.
 */
private fun nearestHandAngle(current: Float, target: Float): Float {
    val forward = (target - current).floorMod(FULL_TURN)
    return current + if (forward > FULL_TURN / 2f) forward - FULL_TURN else forward
}

private fun Float.floorMod(divisor: Float): Float = ((this % divisor) + divisor) % divisor

/**
 * 时/分针的补间时长.
 */
private const val HAND_ANIMATION_MILLIS = 700

/**
 * 秒针补间时长. 比一秒略短, 让相邻两次指针跳动之间留出静止的一小段.
 */
private const val SECOND_HAND_ANIMATION_MILLIS = 900

/**
 * 整块填充的定位框. 无描边.
 *
 * 定位成功后框内显示匹配到的时区 ID, 再点一次才写入偏好.
 */
@Composable
private fun LocationTimeZoneBox(
    locateState: LocateState,
    onLocate: () -> Unit,
    onApply: (zoneId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val label = when (locateState) {
        LocateState.Idle -> "获取定位"
        LocateState.Locating -> "定位中"
        is LocateState.Result -> when (val result = locateState.state) {
            is LocationTimeZoneUiState.Success -> result.zoneId
            is LocationTimeZoneUiState.Failure -> when (result.kind) {
                LocationFailureKind.PermissionDenied -> "定位获取失败"
                LocationFailureKind.LocateFailed -> "定位获取失败"
                LocationFailureKind.ZoneNotFound -> "时区匹配失败"
            }
        }
    }
    val enabled = when (locateState) {
        LocateState.Idle -> true
        LocateState.Locating -> false
        is LocateState.Result -> when (val result = locateState.state) {
            is LocationTimeZoneUiState.Success -> true
            is LocationTimeZoneUiState.Failure -> result.canRetry
        }
    }
    val result = (locateState as? LocateState.Result)?.state
    val containerColor = when {
        result is LocationTimeZoneUiState.Success -> scheme.primaryContainer
        else -> scheme.surfaceContainerHighest
    }
    val contentColor = when {
        result is LocationTimeZoneUiState.Success -> scheme.onPrimaryContainer
        result is LocationTimeZoneUiState.Failure -> scheme.error
        else -> scheme.onSurface
    }
    val iconColor = when {
        result is LocationTimeZoneUiState.Success -> scheme.primary
        result is LocationTimeZoneUiState.Failure -> contentColor
        else -> scheme.primary
    }

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(enabled = enabled) {
                when (locateState) {
                    LocateState.Idle -> onLocate()
                    LocateState.Locating -> Unit
                    is LocateState.Result -> when (val state = locateState.state) {
                        is LocationTimeZoneUiState.Success -> onApply(state.zoneId)
                        is LocationTimeZoneUiState.Failure -> onLocate()
                    }
                }
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = iconColor)
        Spacer(Modifier.width(12.dp))
        if (locateState == LocateState.Locating) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = iconColor)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
        )
    }
}

/**
 * 由坐标匹配 IANA 时区 ID（同 animeko AndroidLocationTimeZoneProvider 的策略）:
 * 先反查国家/地区, 再在该地区的规范时区中选择:
 * 1. 设备的当前时区属于该国家/地区时取设备当前时区;
 * 2. 否则取 UTC 偏移与经度推算值最近的规范时区.
 *
 * 坐标本身不含行政区划, 因此多时区国家内的结果是近似值, 由设置页展示给用户确认.
 */
@Suppress("DEPRECATION") // 同步 Geocoder 在 API 33 起废弃, 异步版本仅 API 33+ 存在
private fun resolveZoneByLocation(context: Context, location: Location): String? {
    if (!Geocoder.isPresent()) return null
    val region = runCatching {
        Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()?.countryCode
    }.getOrNull()?.takeIf(String::isNotBlank) ?: return null
    // CANONICAL_LOCATION 筛选需要 API 28; 更早版本退回该国家的全部时区（含别名）
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
    // 经度每 15 度对应一个时区, 用推算偏移挑最近的候选
    val estimatedOffsetMillis = (location.longitude / 15.0 * 3_600_000).toInt()
    return candidates.minByOrNull { abs(it.rawOffset - estimatedOffsetMillis) }?.id
}

/**
 * 可点击的分组标题: 点击折叠或展开该组. 右侧箭头指向当前状态, 展开朝下, 折叠朝右.
 */
@Composable
private fun TimeZoneSectionHeader(
    header: TimeZoneListRow.SectionHeader,
    showDivider: Boolean,
    onToggleExpand: () -> Unit,
) {
    val indicatorRotation by animateFloatAsState(
        targetValue = if (header.expanded) 0f else -90f,
        label = "timeZoneSectionIndicator",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = ItemHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = header.region,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(indicatorRotation),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * 一行时区. 相邻两行共用一条分割线, 因此分割线画在行下方, 列表最后一行不画.
 */
@Composable
private fun TimeZoneEntryRow(
    row: TimeZoneListRow.Entry,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(selected = row.selected, onClick = onClick)
                .padding(horizontal = ItemHorizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.label ?: row.zoneId,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (row.pinned) FontWeight.Medium else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!row.pinned) {
                Text(
                    text = row.offsetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (row.selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(Modifier.size(20.dp))
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
