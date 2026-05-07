package com.classapp.schedule.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classapp.schedule.ClassApp
import com.classapp.schedule.data.importer.ExcelImporter
import com.classapp.schedule.data.importer.ImportResult
import com.classapp.schedule.data.importer.ParsedCourse
import com.classapp.schedule.data.model.Course
import com.classapp.schedule.util.WeekUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SettingsUiState(
    val semesterStart: LocalDate = WeekUtils.semesterStart,
    val totalWeeks: Int = WeekUtils.totalWeeks,
    val showWeekend: Boolean = WeekUtils.showWeekend,
    val showNonCurrentWeek: Boolean = WeekUtils.showNonCurrentWeek,
    val importStatus: ImportStatus = ImportStatus.Idle,
    val parsedCourses: List<ParsedCourse> = emptyList(),
    val importMessage: String = ""
)

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data object Parsing : ImportStatus()
    data object Preview : ImportStatus()
    data object Importing : ImportStatus()
    data object Done : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ClassApp).repository
    private val importer = ExcelImporter(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateSemesterStart(date: LocalDate) {
        WeekUtils.semesterStart = date
        _uiState.update { it.copy(semesterStart = date) }
    }

    fun updateTotalWeeks(weeks: Int) {
        WeekUtils.totalWeeks = weeks
        _uiState.update { it.copy(totalWeeks = WeekUtils.totalWeeks) }
    }

    fun toggleShowWeekend(show: Boolean) {
        WeekUtils.showWeekend = show
        _uiState.update { it.copy(showWeekend = show) }
    }

    fun toggleShowNonCurrentWeek(show: Boolean) {
        WeekUtils.showNonCurrentWeek = show
        _uiState.update { it.copy(showNonCurrentWeek = show) }
    }

    fun parseExcelFile(uri: Uri) {
        _uiState.update { it.copy(importStatus = ImportStatus.Parsing, importMessage = "") }

        viewModelScope.launch {
            val result = importer.parse(uri)
            when (result) {
                is ImportResult.Success -> {
                    if (result.courses.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                importStatus = ImportStatus.Error("未能识别课表格式"),
                                importMessage = "请检查 Excel 文件格式"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                importStatus = ImportStatus.Preview,
                                parsedCourses = result.courses,
                                importMessage = "共解析出 ${result.courses.size} 门课程"
                            )
                        }
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            importStatus = ImportStatus.Error(result.message),
                            importMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun confirmImport() {
        val courses = _uiState.value.parsedCourses
        if (courses.isEmpty()) return

        _uiState.update { it.copy(importStatus = ImportStatus.Importing) }

        viewModelScope.launch {
            try {
                val colorMap = mutableMapOf<String, Int>()
                var nextColor = 0
                courses.forEach { course ->
                    val colorIdx = colorMap.getOrPut(course.name) { nextColor++ }
                    repository.addCourse(
                        Course(
                            name = course.name,
                            teacher = course.teacher,
                            classroom = course.classroom,
                            dayOfWeek = course.dayOfWeek,
                            startSlot = course.startSlot,
                            endSlot = course.endSlot,
                            weeks = course.weeks,
                            colorIndex = colorIdx
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        importStatus = ImportStatus.Done,
                        parsedCourses = emptyList(),
                        importMessage = "成功导入 ${courses.size} 门课程"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importStatus = ImportStatus.Error("保存失败: ${e.message}"),
                        importMessage = ""
                    )
                }
            }
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(importStatus = ImportStatus.Idle, parsedCourses = emptyList(), importMessage = "")
        }
    }

    fun resetImportStatus() {
        _uiState.update {
            it.copy(importStatus = ImportStatus.Idle, importMessage = "")
        }
    }
}
