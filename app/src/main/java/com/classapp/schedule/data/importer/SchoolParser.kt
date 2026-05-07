package com.classapp.schedule.data.importer

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses course data extracted from the 教务系统课表 page.
 *
 * Data comes from JavaScript injection in WebView which returns JSON:
 *   [{
 *     "name": "Linux操作系统",
 *     "teacher": "王守强",
 *     "classroom": "长清交通机电楼B座机电楼B407",
 *     "weeks": "4-12(周)[01-02节]",
 *     "day": 1,     // Mon=1 .. Sun=7
 *     "row": 0      // 0=第一大节 .. 4=第五大节
 *   }]
 *
 * Also handles raw HTML fallback from the page.
 */
object SchoolParser {

    private val SLOT_MAP = listOf(
        1..2,   // 第一大节
        3..4,   // 第二大节
        5..6,   // 第三大节
        7..8,   // 第四大节
        9..10   // 第五大节
    )

    // ---- JSON path (from JS injection) ----

    fun parseJson(json: String): List<ParsedCourse> {
        val raw = mutableListOf<ParsedCourse>()
        val arr = try { JSONArray(json) } catch (_: Exception) { return emptyList() }

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val day = obj.optInt("day", 1)
            val rowIdx = obj.optInt("row", 0)
            val (s, e) = parseSlots(obj.optString("slots", ""), rowIdx)
            val weeks = parseWeeks(obj.optString("weeks", ""))

            raw.add(
                ParsedCourse(
                    name = obj.optString("name", ""),
                    teacher = obj.optString("teacher", ""),
                    classroom = obj.optString("classroom", ""),
                    dayOfWeek = day.coerceIn(1, 7),
                    startSlot = s,
                    endSlot = e,
                    weeks = weeks
                )
            )
        }
        return raw
    }

    // ---- HTML fallback path (for when JS can't run) ----

    fun parseHtml(html: String): List<ParsedCourse> {
        // Try new format (class="kb_table", data in <p title="...">)
        val tableStart = indexOf(html, "kb_table") ?: return parseHtmlOldFormat(html)
        return parseHtmlKbTable(html, tableStart)
    }

    /** New format: table class="kb_table", course data in <p title="..."> */
    private fun parseHtmlKbTable(html: String, tableHint: Int): List<ParsedCourse> {
        val raw = mutableListOf<ParsedCourse>()
        val tableTagStart = html.lastIndexOf("<table", tableHint)
        if (tableTagStart < 0) return emptyList()
        val tableEnd = html.indexOf("</table>", tableHint)
        if (tableEnd < 0) return emptyList()
        val table = html.substring(tableTagStart, tableEnd + 8)

        val rows = extractTags(table, "tr")
        for ((ri, rowHtml) in rows.withIndex()) {
            if (ri == 0) continue // header
            val cells = extractTags(rowHtml, "td")
            if (cells.size < 2) continue

            for (ci in 1 until cells.size) { // skip time-label column
                val day = ci
                val cell = cells[ci]

                // Find <p> tags with title attribute
                val courses = parseCellParagraphs(cell, day, ri - 1)
                raw.addAll(courses)
            }
        }
        return raw
    }

    /** Parse <p title="..."> inside a table cell. */
    private fun parseCellParagraphs(cellHtml: String, day: Int, rowIdx: Int): List<ParsedCourse> {
        val result = mutableListOf<ParsedCourse>()
        var pos = 0
        while (true) {
            val pStart = cellHtml.indexOf("<p", pos) ?: break
            val pEnd = cellHtml.indexOf(">", pStart) ?: break
            val pTag = cellHtml.substring(pStart, pEnd + 1)

            // Extract title attribute (supports single or double quotes)
            val titleMatch = Regex("""title\s*=\s*["']([^"']*)["']""").find(pTag)
            val title = titleMatch?.groupValues?.get(1) ?: ""
            if (title.isBlank()) { pos = pEnd + 1; continue }

            var name = ""
            var classroom = ""
            var weekStr = ""
            var slotStr = ""

            val parts = title.split(Regex("<br\\/?>", RegexOption.IGNORE_CASE))
            for (part in parts) {
                val line = part.replace(Regex("<[^>]+>"), "").trim()

                val nm = Regex("""课程名称[：:]?\s*(.+)""").find(line)
                if (nm != null) name = nm.groupValues[1].trim()

                val rm = Regex("""上课地点[：:]?\s*(.+)""").find(line)
                if (rm != null) classroom = rm.groupValues[1].trim()

                val tm = Regex("""上课时间[：:]?\s*第?([\d,\-]+)\(?\s*周\s*\)?\s*(?:星期[一二三四五六日天]\s*)?\[(\d+)-(\d+)\]节""").find(line)
                if (tm != null) {
                    weekStr = tm.groupValues[1]
                    slotStr = "[${tm.groupValues[2]}-${tm.groupValues[3]}]节"
                }
            }

            if (name.isNotBlank()) {
                val (s, e) = parseSlots(slotStr, rowIdx)
                val weeks = parseWeeks(weekStr)
                result.add(ParsedCourse(name, "", classroom, day, s, e, weeks = weeks))
            }
            pos = pEnd + 1
        }
        return result
    }

    /** Old format: id="kbtable", data in div.kbcontent */
    private fun parseHtmlOldFormat(html: String): List<ParsedCourse> {
        val raw = mutableListOf<ParsedCourse>()

        // Find the schedule table
        val tableStart = indexOf(html, "id=\"kbtable\"") ?: return emptyList()
        val tableTagStart = html.lastIndexOf("<table", tableStart)
        if (tableTagStart < 0) return emptyList()
        val tableEnd = html.indexOf("</table>", tableStart)
        if (tableEnd < 0) return emptyList()
        val table = html.substring(tableTagStart, tableEnd + 8)

        // Split rows
        val rows = extractTags(table, "tr")
        for ((ri, rowHtml) in rows.withIndex()) {
            if (ri == 0) continue // header
            val isLastRow = ri == rows.size - 1

            val cells = extractTags(rowHtml, if (isLastRow) "td" else "td")
            if (cells.isEmpty()) continue

            // Row slot from <th>
            val thText = extractTagText(rowHtml, "th")
            val rowIdx = detectRowIndex(thText)
            if (rowIdx < 0 && !isLastRow) continue

            for ((ci, cell) in cells.withIndex()) {
                val day = ci + 1
                // Look for div.kbcontent (not kbcontent1)
                val kbc = extractKbContent(cell) ?: continue
                val parts = kbc.split(Regex("-{20,}"))
                for (part in parts) {
                    val course = parseCellText(part.trim(), day, rowIdx) ?: continue
                    raw.add(course)
                }
            }
        }

        return raw
    }

    private fun parseCellText(text: String, day: Int, rowIdx: Int): ParsedCourse? {
        // Strip HTML tags
        var clean = text
            .replace(Regex("<span[^>]*>[\\s\\S]*?</span>"), "")
            .replace(Regex("<[^>]+>"), "\n")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("\n{2,}"), "\n")
            .trim()

        val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        if (lines.all { it == "&nbsp;" || it.isEmpty() }) return null

        var name = ""
        var teacher = ""
        var classroom = ""
        var weekInfo = ""
        var codeFound = false

        for (i in lines.indices) {
            val line = lines[i]
            when {
                i == 0 && line.matches(Regex("\\d{6}[-–].*")) -> codeFound = true
                codeFound && name.isEmpty() -> name = line
                !codeFound && name.isEmpty() && !line.contains("老师") && !line.contains("教室") ->
                    name = line
                line.contains("老师") -> teacher = line.replace(Regex(".*老师[：:]?"), "").trim()
                line.contains("教室") -> classroom = line.replace(Regex(".*教室[：:]?"), "").trim()
                line.contains("周次") || line.contains("(周)") || line.contains("[") || line.contains("节]") ->
                    weekInfo = line
                // Fallback for font-title format: teacher name on its own line (no label)
                teacher.isEmpty() && !line.contains("教室") && !line.matches(Regex("\\d{6}.*")) &&
                    !line.contains("周") && !line.contains("[") ->
                    teacher = line
                // Fallback for classroom name on its own line
                classroom.isEmpty() && teacher.isNotEmpty() &&
                    !line.contains("周") && !line.contains("[") ->
                    classroom = line
            }
        }

        if (name.isBlank()) return null

        val (s, e) = parseSlots(weekInfo, rowIdx)
        val weeks = parseWeeks(weekInfo)

        return ParsedCourse(name, teacher, classroom, day, s, e, weeks = weeks)
    }

    // ---- Slot parsing ----

    private fun parseSlots(text: String, rowIdx: Int): Pair<Int, Int> {
        val m = Regex("\\[(\\d+)-(\\d+)节?\\]").find(text)
        if (m != null) {
            val s = m.groupValues[1].toIntOrNull()
            val e = m.groupValues[2].toIntOrNull()
            if (s != null && e != null) return Pair(s, e)
        }
        val range = SLOT_MAP.getOrNull(rowIdx) ?: return Pair(1, 2)
        return Pair(range.first, range.last)
    }

    private fun detectRowIndex(thText: String): Int = when {
        thText.contains("第一") || thText.contains("08:30") -> 0
        thText.contains("第二") || thText.contains("10:15") -> 1
        thText.contains("第三") || thText.contains("13:30") -> 2
        thText.contains("第四") || thText.contains("15:15") -> 3
        thText.contains("第五") || thText.contains("19:00") -> 4
        else -> -1
    }

    // ---- Week parsing (supports "4-12", "2,4,6,8,10,12", "1-4,6,8") ----

    private fun parseWeeks(text: String): String {
        val clean = text
            .replace(Regex("\\(周\\).*"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*\\]"), "")
            .trim()
        if (clean.isBlank()) return "1-20"

        // Validate and normalize: keep only valid comma-separated ranges/numbers
        val parts = clean.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val validParts = mutableListOf<String>()
        for (part in parts) {
            val r = Regex("^(\\d+)\\s*-\\s*(\\d+)$").find(part)
            if (r != null) {
                validParts.add("${r.groupValues[1]}-${r.groupValues[2]}")
            } else if (part.toIntOrNull() != null) {
                validParts.add(part)
            }
        }

        return if (validParts.isNotEmpty()) validParts.joinToString(",") else "1-20"
    }

    // ---- Low-level HTML helpers ----

    private fun indexOf(text: String, target: String, start: Int = 0): Int? {
        val idx = text.indexOf(target, start)
        return if (idx >= 0) idx else null
    }

    /** Extract all bodies of <tag>…</tag> pairs (non-recursive). */
    private fun extractTags(html: String, tag: String): List<String> {
        val result = mutableListOf<String>()
        var pos = 0
        while (true) {
            val open = html.indexOf("<$tag", pos) ?: break
            val close = html.indexOf(">", open) ?: break
            val end = html.indexOf("</$tag>", close + 1) ?: break
            result.add(html.substring(close + 1, end))
            pos = end + tag.length + 3
        }
        return result
    }

    /** Extract text content of the first <th> in html. */
    private fun extractTagText(html: String, tag: String): String {
        val open = html.indexOf("<$tag", 0) ?: return ""
        val close = html.indexOf(">", open) ?: return ""
        val end = html.indexOf("</$tag>", close + 1) ?: return ""
        return html.substring(close + 1, end)
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .trim()
    }

    /** Find the visible kbcontent div (not kbcontent1). */
    private fun extractKbContent(cellHtml: String): String? {
        // Find <div … class="kbcontent" …> (not kbcontent1, not kbcontent12, etc.)
        var pos = 0
        while (true) {
            val start = cellHtml.indexOf("<div", pos) ?: break
            val end = cellHtml.indexOf(">", start) ?: break
            val tag = cellHtml.substring(start, end + 1)
            if (Regex("""class\s*=\s*"kbcontent"[^"]*""").containsMatchIn(tag) &&
                !Regex("""class\s*=\s*"kbcontent\d""").containsMatchIn(tag)) {
                val inner = cellHtml.substring(end + 1)
                return extractDivContent(inner)
            }
            pos = end + 1
        }
        return null
    }

    private fun extractDivContent(rest: String): String? {
        var depth = 1
        var pos = 0
        val openR = Regex("<div[\\s>]")
        val closeR = Regex("</div>")

        while (depth > 0) {
            val o = openR.find(rest, pos)
            val c = closeR.find(rest, pos)
            when {
                c == null -> return null
                o == null || c.range.first < o.range.first -> {
                    depth--
                    pos = c.range.last + 1
                }
                else -> {
                    depth++
                    pos = o.range.last + 1
                }
            }
        }
        return rest.substring(0, pos - 6)
    }
}
