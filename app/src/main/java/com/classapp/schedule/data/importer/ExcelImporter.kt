package com.classapp.schedule.data.importer

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class ParsedCourse(
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int = 1,
    val startSlot: Int = 1,
    val endSlot: Int = 2,
    val weeks: String = "1-20",
    val colorIndex: Int = 0
)

sealed class ImportResult {
    data class Success(val courses: List<ParsedCourse>) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class ExcelImporter(private val context: Context) {

    fun parse(uri: Uri): ImportResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return ImportResult.Error("无法打开文件")

        val entries = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(inputStream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: Exception) {
            return ImportResult.Error("无法解析 XLSX 文件（非标准格式）")
        }

        if (entries.isEmpty()) {
            return ImportResult.Error("文件为空或格式不正确")
        }

        try {
            val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
            val sheetData = parseSheet(entries["xl/worksheets/sheet1.xml"], sharedStrings)
            if (sheetData.isEmpty()) {
                return ImportResult.Error("工作表中没有数据")
            }

            val courses = detectAndParse(sheetData)
            if (courses.isEmpty()) {
                return ImportResult.Error("未能识别课表格式，请检查表头")
            }
            return ImportResult.Success(courses)
        } catch (e: Exception) {
            return ImportResult.Error("解析数据时出错: ${e.message}")
        }
    }

    private fun parseSharedStrings(xmlBytes: ByteArray?): List<String> {
        if (xmlBytes == null) return emptyList()
        val strings = mutableListOf<String>()
        val parser = newParser(xmlBytes)

        var inSi = false
        var inT = false
        var text = StringBuilder()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            text = StringBuilder()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) text.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            strings.add(text.toString())
                            inSi = false
                        }
                        "t" -> inT = false
                    }
                }
            }
            parser.next()
        }
        return strings
    }

    private fun parseSheet(xmlBytes: ByteArray?, sharedStrings: List<String>): List<List<String>> {
        if (xmlBytes == null) return emptyList()
        val rows = mutableListOf<List<String>>()
        val parser = newParser(xmlBytes)

        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellValue = StringBuilder()
        var inCell = false
        var inValue = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            inCell = true
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = StringBuilder()
                        }
                        "v" -> if (inCell) inValue = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) cellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            val raw = cellValue.toString()
                            val value = if (cellType == "s" && raw.isNotEmpty()) {
                                val idx = raw.toIntOrNull() ?: -1
                                if (idx in sharedStrings.indices) sharedStrings[idx] else ""
                            } else {
                                raw
                            }
                            currentRow.add(value.trim())
                            inCell = false
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotEmpty() }) {
                                rows.add(currentRow)
                            }
                        }
                        "v" -> inValue = false
                    }
                }
            }
            parser.next()
        }
        return rows
    }

    private fun detectAndParse(rows: List<List<String>>): List<ParsedCourse> {
        if (rows.isEmpty()) return emptyList()

        val headerRow = rows[0].map { it.trim() }

        return if (isListFormat(headerRow)) {
            parseListFormat(rows)
        } else if (isGridFormat(headerRow)) {
            parseGridFormat(rows)
        } else {
            emptyList()
        }
    }

    private fun isListFormat(headers: List<String>): Boolean {
        val keywords = listOf("课程", "科目", "名称", "教师", "老师", "教室", "节次", "星期", "周次")
        return headers.count { h -> keywords.any { h.contains(it) } } >= 2
    }

    private fun isGridFormat(headers: List<String>): Boolean {
        val dayKeywords = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日", "星期")
        return headers.count { h -> dayKeywords.any { h.contains(it) } } >= 3
    }

    private fun parseListFormat(rows: List<List<String>>): List<ParsedCourse> {
        val headers = rows[0].map { it.trim() }

        val nameIdx = indexOfAny(headers, "课程名称", "课程名", "课程", "科目", "名称")
        val teacherIdx = indexOfAny(headers, "教师", "老师", "授课教师")
        val roomIdx = indexOfAny(headers, "教室", "上课地点", "地点")
        val dayIdx = indexOfAny(headers, "星期", "周", "day", "Day", "DayOfWeek")
        val startSlotIdx = indexOfAny(headers, "开始节次", "开始", "start", "StartSlot")
        val endSlotIdx = indexOfAny(headers, "结束节次", "结束", "end", "EndSlot")
        val weekStartIdx = indexOfAny(headers, "起始周", "开始周", "week_start", "WeekStart")
        val weekEndIdx = indexOfAny(headers, "结束周", "week_end", "WeekEnd")
        val oddEvenIdx = indexOfAny(headers, "单双周", "odd_even", "OddEven")

        val courses = mutableListOf<ParsedCourse>()
        if (nameIdx < 0) return courses

        for (i in 1 until rows.size) {
            val row = rows[i]
            val name = getField(row, nameIdx, "")
            if (name.isBlank()) continue

            val dayStr = getField(row, dayIdx, "1")
            val startStr = getField(row, startSlotIdx, "1")
            val endStr = getField(row, endSlotIdx, "2")

            val ws = getField(row, weekStartIdx, "1").toIntOrNull() ?: 1
            val we = getField(row, weekEndIdx, "20").toIntOrNull() ?: 20
            val oe = getField(row, oddEvenIdx, "0").toIntOrNull() ?: 0
            val weeksStr = when (oe) {
                1 -> "$ws-$we(单)"
                2 -> "$ws-$we(双)"
                else -> "$ws-$we"
            }

            courses.add(
                ParsedCourse(
                    name = name,
                    teacher = getField(row, teacherIdx, ""),
                    classroom = getField(row, roomIdx, ""),
                    dayOfWeek = parseDayOfWeek(dayStr),
                    startSlot = parseSlot(startStr),
                    endSlot = parseSlot(endStr),
                    weeks = weeksStr
                )
            )
        }
        return courses
    }

    private fun parseGridFormat(rows: List<List<String>>): List<ParsedCourse> {
        val headers = rows[0]
        val dayCols = mutableListOf<Pair<Int, Int>>() // (colIndex, dayOfWeek)
        for (ci in headers.indices) {
            val h = headers[ci]
            for (d in 1..7) {
                val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                if (h.contains(dayNames[d - 1]) || h.contains("星期${toChineseNum(d)}")) {
                    dayCols.add(ci to d)
                    break
                }
            }
        }

        val courses = mutableListOf<ParsedCourse>()

        val slotMap = listOf(
            1..2, 3..4, 5..6, 7..8, 9..10
        )

        for (ri in 1 until rows.size) {
            val row = rows[ri]
            val slotLabel = row.getOrElse(0) { "" }

            val slotIndex = slotMap.indexOfFirst { range ->
                val digits = slotLabel.filter { it.isDigit() }
                digits.isNotEmpty() && range.contains(digits.take(2).toIntOrNull() ?: -1)
            }
            if (slotIndex == -1) continue

            val range = slotMap[slotIndex]
            for ((colIdx, day) in dayCols) {
                val cell = row.getOrElse(colIdx) { "" }.trim()
                if (cell.isBlank()) continue

                val parts = cell.split(Regex("[/\n，,、]")).map { it.trim() }.filter { it.isNotEmpty() }
                val name = parts.getOrElse(0) { cell }
                val teacher = if (parts.size > 2) parts[1] else ""
                val classroom = when {
                    parts.size > 2 -> parts[2]
                    parts.size > 1 -> parts[1]
                    else -> ""
                }

                courses.add(
                    ParsedCourse(
                        name = name,
                        teacher = teacher,
                        classroom = classroom,
                        dayOfWeek = day,
                        startSlot = range.first,
                        endSlot = range.last
                    )
                )
            }
        }
        return courses
    }

    private fun newParser(xmlBytes: ByteArray): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        return parser
    }

    private fun indexOfAny(headers: List<String>, vararg keys: String): Int {
        for (key in keys) {
            val idx = headers.indexOfFirst { it.equals(key, ignoreCase = true) || it.contains(key) }
            if (idx >= 0) return idx
        }
        return -1
    }

    private fun getField(row: List<String>, idx: Int, default: String): String {
        return if (idx in row.indices) row[idx].ifBlank { default } else default
    }

    private fun parseDayOfWeek(s: String): Int {
        val cleaned = s.trim()
        return when {
            cleaned.toIntOrNull() != null -> cleaned.toInt().coerceIn(1, 7)
            cleaned.contains("一") || cleaned.contains("1") -> 1
            cleaned.contains("二") || cleaned.contains("2") -> 2
            cleaned.contains("三") || cleaned.contains("3") -> 3
            cleaned.contains("四") || cleaned.contains("4") -> 4
            cleaned.contains("五") || cleaned.contains("5") -> 5
            cleaned.contains("六") || cleaned.contains("6") -> 6
            cleaned.contains("日") || cleaned.contains("天") || cleaned.contains("7") -> 7
            else -> 1
        }
    }

    private fun parseSlot(s: String): Int {
        val digits = s.trim().filter { it.isDigit() }
        if (digits.isEmpty()) return 1
        val n = digits.toInt()
        return when {
            n <= 2 -> 1
            n <= 4 -> 3
            n <= 6 -> 5
            n <= 8 -> 7
            else -> 9
        }
    }

    private fun toChineseNum(d: Int): String = when (d) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"
        5 -> "五"; 6 -> "六"; 7 -> "日"; else -> ""
    }
}
