package com.classapp.schedule.util

import android.content.Context
import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object WeekUtils {

    private const val PREFS_NAME = "classapp_settings"
    private const val KEY_SEMESTER_START = "semester_start"
    private const val KEY_TOTAL_WEEKS = "total_weeks"
    private const val KEY_SHOW_WEEKEND = "show_weekend"
    private const val KEY_SHOW_NON_CURRENT = "show_non_current_week"
    private const val KEY_SLOT_START_PREFIX = "slot_start_"
    private const val KEY_SLOT_END_PREFIX = "slot_end_"

    private lateinit var prefs: SharedPreferences

    var semesterStart: LocalDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        .let { if (it.dayOfWeek != DayOfWeek.MONDAY) it.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)) else it }
        set(value) {
            field = value
            if (::prefs.isInitialized) prefs.edit().putString(KEY_SEMESTER_START, value.toString()).apply()
        }

    var totalWeeks: Int = 20
        set(value) {
            field = value.coerceIn(10, 30)
            if (::prefs.isInitialized) prefs.edit().putInt(KEY_TOTAL_WEEKS, field).apply()
        }

    var showWeekend: Boolean = true
        set(value) {
            field = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_WEEKEND, value).apply()
        }

    var showNonCurrentWeek: Boolean = false
        set(value) {
            field = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_NON_CURRENT, value).apply()
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        semesterStart = prefs.getString(KEY_SEMESTER_START, null)?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: defaultSemesterStart()
        totalWeeks = prefs.getInt(KEY_TOTAL_WEEKS, 20).coerceIn(10, 30)
        showWeekend = prefs.getBoolean(KEY_SHOW_WEEKEND, true)
        showNonCurrentWeek = prefs.getBoolean(KEY_SHOW_NON_CURRENT, false)

        // 加载自定义时间段
        val loaded = defaultSlotTimes.mapIndexed { i, triple ->
            val s = prefs.getString(KEY_SLOT_START_PREFIX + i, triple.second) ?: triple.second
            val e = prefs.getString(KEY_SLOT_END_PREFIX + i, triple.third) ?: triple.third
            Triple(triple.first, s, e)
        }
        timeSlotLabels = loaded
    }

    private fun defaultSemesterStart(): LocalDate {
        var date = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        if (date.dayOfWeek != DayOfWeek.MONDAY) {
            date = date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
        }
        return date
    }

    fun getCurrentWeek(): Int {
        val today = LocalDate.now()
        if (today < semesterStart) return 1
        val diff = ChronoUnit.WEEKS.between(semesterStart, today).toInt() + 1
        return diff.coerceIn(1, totalWeeks)
    }

    fun getWeekDateRange(week: Int): Pair<LocalDate, LocalDate> {
        val start = semesterStart.plusWeeks((week - 1).toLong())
        val end = start.plusDays(6)
        return Pair(start, end)
    }

    fun getDateForWeekDay(week: Int, dayOfWeek: Int): LocalDate {
        val monday = semesterStart.plusWeeks((week - 1).toLong())
        return monday.plusDays((dayOfWeek - 1).toLong())
    }

    fun isCourseActiveThisWeek(weeks: String, week: Int): Boolean {
        val weekSet = parseWeekSet(weeks)
        return week in weekSet
    }

    fun parseWeekSet(weeks: String): Set<Int> {
        val result = mutableSetOf<Int>()
        // Handle "(单)" / "(双)" suffix on entire string
        val isOdd = weeks.contains("(单)")
        val isEven = weeks.contains("(双)")
        val cleaned = weeks.replace(Regex("\\(单\\)|\\(双\\)"), "").trim()

        for (part in cleaned.split(",")) {
            val trimmed = part.trim()
            val range = Regex("^(\\d+)-(\\d+)$").find(trimmed)
            if (range != null) {
                val s = range.groupValues[1].toIntOrNull() ?: continue
                val e = range.groupValues[2].toIntOrNull() ?: continue
                (s..e).forEach { result.add(it) }
            } else {
                trimmed.toIntOrNull()?.let { result.add(it) }
            }
        }

        return when {
            isOdd -> result.filter { it % 2 == 1 }.toSet()
            isEven -> result.filter { it % 2 == 0 }.toSet()
            else -> result
        }
    }

    fun getDayName(dayIndex: Int): String = when (dayIndex) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }

    // Triple(节次名, 开始时间, 结束时间) - 可自定义
    private val defaultSlotTimes = listOf(
        Triple("1-2", "08:30", "10:00"),
        Triple("3-4", "10:10", "11:40"),
        Triple("5-6", "13:00", "14:30"),
        Triple("7-8", "14:40", "16:10"),
        Triple("9-10", "16:40", "17:20")
    )

    var timeSlotLabels = defaultSlotTimes
        private set

    fun updateSlotTime(index: Int, start: String, end: String) {
        if (index !in timeSlotLabels.indices) return
        val updated = timeSlotLabels.toMutableList()
        updated[index] = Triple(updated[index].first, start, end)
        timeSlotLabels = updated
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_SLOT_START_PREFIX + index, start)
                .putString(KEY_SLOT_END_PREFIX + index, end)
                .apply()
        }
    }

    val slotGroups = listOf(
        1..2, 3..4, 5..6, 7..8, 9..10
    )

    fun getSlotGroupIndex(slot: Int): Int {
        return when {
            slot <= 2 -> 0
            slot <= 4 -> 1
            slot <= 6 -> 2
            slot <= 8 -> 3
            else -> 4
        }
    }

    fun getSlotGroupSpan(startSlot: Int, endSlot: Int): Int {
        return getSlotGroupIndex(endSlot) - getSlotGroupIndex(startSlot) + 1
    }

    fun getSlotGroupRange(slot: Int): IntRange {
        return slotGroups[getSlotGroupIndex(slot)]
    }
}
