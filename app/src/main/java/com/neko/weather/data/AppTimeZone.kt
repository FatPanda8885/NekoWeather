package com.neko.weather.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** 时区目录中的一个时区。[offsetSeconds] 是生成目录那一刻的 UTC 偏移。 */
data class TimeZoneOption(
    val zoneId: String,
    val offsetSeconds: Int,
) {
    /** 例如 `UTC+08:00`。 */
    val offsetLabel: String get() = formatUtcOffset(offsetSeconds)

    /** IANA ID 中 `/` 之后的部分，例如 `Shanghai`。 */
    val city: String get() = zoneId.substringAfter('/', "")
}

/** 同一 Region 下的时区，[options] 按偏移升序、同偏移按 ID 字典序排列。 */
data class TimeZoneSection(
    val region: String,
    val options: List<TimeZoneOption>,
)

/** App 的显示时区解析与时间换算。 */
object AppTimeZone {
    /** 偏好 ID 解析为时区：空串或无效值回退系统时区。 */
    fun resolve(prefId: String): ZoneId =
        prefId.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { ZoneId.of(raw) }.getOrNull()
        } ?: ZoneId.systemDefault()

    /** 天气数据所在地时区：优先快照自带的 IANA ID / 偏移串，否则回退系统时区。 */
    fun locationZone(timezoneId: String?): ZoneId =
        timezoneId?.let { raw -> runCatching { ZoneId.of(raw) }.getOrNull() }
            ?: ZoneId.systemDefault()

    /**
     * 把「所在地本地时间串」换算到 [to] 时区的显示串。
     * 输入形如 `2026-10-24T18:00`；输出 `HH:mm`。解析失败时原样返回输入的末段时间。
     */
    fun convertLocationTime(text: String, from: ZoneId, to: ZoneId): String {
        if (from.id == to.id) return text.takeLast(5)
        val parsed = runCatching { LocalDateTime.parse(text) }.getOrNull() ?: return text.takeLast(5)
        val shifted = parsed.atZone(from).withZoneSameInstant(to)
        return shifted.format(DISPLAY_TIME)
    }

    /** 把完整日期时间换算后仍以 `yyyy-MM-ddTHH:mm` 形式返回（供需要日期的场景）。 */
    fun convertLocationDateTime(text: String, from: ZoneId, to: ZoneId): String {
        if (from.id == to.id) return text
        val parsed = runCatching { LocalDateTime.parse(text) }.getOrNull() ?: return text
        return parsed.atZone(from).withZoneSameInstant(to).format(FULL_TIME)
    }

    fun offsetLabel(zone: ZoneId, at: Instant = Instant.now()): String =
        formatUtcOffset(zone.rules.getOffset(at).totalSeconds)

    /** 生成时区目录：只保留 `Region/City` 形式的 IANA ID，按 Region 分组。 */
    fun catalog(now: Instant = Instant.now(), systemZoneId: String = ZoneId.systemDefault().id): List<TimeZoneSection> {
        val byRegion = mutableMapOf<String, MutableList<TimeZoneOption>>()
        for (zoneId in ZoneId.getAvailableZoneIds()) {
            if (zoneId == systemZoneId) continue
            val region = zoneId.substringBefore('/', "")
            if (region !in TIMEZONE_REGIONS) continue
            val offsetSeconds = runCatching { ZoneId.of(zoneId).rules.getOffset(now).totalSeconds }.getOrNull() ?: continue
            byRegion.getOrPut(region) { mutableListOf() }.add(TimeZoneOption(zoneId, offsetSeconds))
        }
        return byRegion.entries
            .sortedBy { it.key }
            .map { (region, options) ->
                TimeZoneSection(
                    region = region,
                    options = options
                        .distinctBy { it.zoneId }
                        .sortedWith(compareBy({ it.offsetSeconds }, { it.zoneId })),
                )
            }
    }

    /** 按 [query] 过滤目录，丢弃没有命中的分组；空白查询命中一切。 */
    fun filterSections(sections: List<TimeZoneSection>, query: String): List<TimeZoneSection> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return sections
        return sections.mapNotNull { section ->
            val options = section.options.filter { option ->
                option.zoneId.contains(trimmed, true) ||
                    option.city.contains(trimmed, true) ||
                    section.region.contains(trimmed, true) ||
                    option.offsetLabel.contains(trimmed, true)
            }
            if (options.isEmpty()) null else section.copy(options = options)
        }
    }

    private val DISPLAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val FULL_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    private val TIMEZONE_REGIONS = setOf(
        "Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic",
        "Australia", "Europe", "Indian", "Pacific",
    )
}

/** 把 UTC 偏移格式化为 `UTC+08:00` 形式。 */
fun formatUtcOffset(offsetSeconds: Int): String {
    val sign = if (offsetSeconds < 0) '-' else '+'
    val totalMinutes = abs(offsetSeconds) / 60
    return "UTC$sign${(totalMinutes / 60).toString().padStart(2, '0')}:${(totalMinutes % 60).toString().padStart(2, '0')}"
}
