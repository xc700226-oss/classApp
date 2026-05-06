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
import com.classapp.schedule.util.CredentialStore
import com.classapp.schedule.util.WeekUtils
import com.classapp.schedule.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class SettingsUiState(
    val semesterStart: LocalDate = WeekUtils.semesterStart,
    val totalWeeks: Int = WeekUtils.totalWeeks,
    val showWeekend: Boolean = WeekUtils.showWeekend,
    val showNonCurrentWeek: Boolean = WeekUtils.showNonCurrentWeek,
    val importStatus: ImportStatus = ImportStatus.Idle,
    val parsedCourses: List<ParsedCourse> = emptyList(),
    val importMessage: String = "",
    val portalUrl: String = CredentialStore.portalUrl,
    val username: String = CredentialStore.username,
    val password: String = "",
    val hasStoredCredentials: Boolean = CredentialStore.hasCredentials(),
    val autoFetchEnabled: Boolean = CredentialStore.autoFetchEnabled,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long = CredentialStore.lastSyncTime
)

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

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
                // 按课程名分配颜色索引（同名课程同色）
                val colorMap = mutableMapOf<String, Int>()
                var nextColor = 0
                courses.forEach { course ->
                    val colorIdx = colorMap.getOrPut(course.name) { nextColor++ }
                    repository.addCourse(
                        com.classapp.schedule.data.model.Course(
                            name = course.name,
                            teacher = course.teacher,
                            classroom = course.classroom,
                            dayOfWeek = course.dayOfWeek,
                            startSlot = course.startSlot,
                            endSlot = course.endSlot,
                            weekStart = course.weekStart,
                            weekEnd = course.weekEnd,
                            oddEven = course.oddEven,
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

    fun updatePortalUrl(url: String) {
        _uiState.update { it.copy(portalUrl = url) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun saveCredentials() {
        val state = _uiState.value
        CredentialStore.portalUrl = state.portalUrl.trim()
        CredentialStore.username = state.username.trim()
        if (state.password.isNotEmpty()) {
            CredentialStore.password = state.password
        }
        _uiState.update {
            it.copy(hasStoredCredentials = CredentialStore.hasCredentials())
        }
    }

    fun toggleAutoFetch(enabled: Boolean) {
        CredentialStore.autoFetchEnabled = enabled
        _uiState.update { it.copy(autoFetchEnabled = enabled) }

        val context = getApplication<ClassApp>()
        if (enabled && CredentialStore.hasCredentials()) {
            SyncWorker.enqueuePeriodicSync(context)
        } else {
            SyncWorker.cancelPeriodicSync(context)
        }
    }

    fun manualSync() {
        if (!CredentialStore.hasCredentials()) {
            _uiState.update {
                it.copy(syncStatus = SyncStatus.Error("请先填写教务系统凭据"))
            }
            return
        }

        _uiState.update { it.copy(syncStatus = SyncStatus.Syncing) }

        viewModelScope.launch {
            try {
                val fetcher = com.classapp.schedule.data.fetcher.ScheduleFetcher()
                val result = withContext(Dispatchers.IO) {
                    fetcher.fetchSchedule(
                        CredentialStore.portalUrl,
                        CredentialStore.username,
                        CredentialStore.password
                    )
                }

                when (result) {
                    is com.classapp.schedule.data.fetcher.FetchResult.Error -> {
                        _uiState.update {
                            it.copy(syncStatus = SyncStatus.Error(result.message))
                        }
                    }
                    is com.classapp.schedule.data.fetcher.FetchResult.Success -> {
                        val courses = com.classapp.schedule.data.importer.SchoolParser.parseHtml(result.html)
                        if (courses.isEmpty()) {
                            _uiState.update {
                                it.copy(syncStatus = SyncStatus.Error("未找到课程数据"))
                            }
                        } else {
                            repository.deleteAll()
                            val colorMap = mutableMapOf<String, Int>()
                            var nextColor = 0
                            courses.forEach { pc ->
                                val colorIdx = colorMap.getOrPut(pc.name) { nextColor++ }
                                repository.addCourse(
                                    Course(
                                        name = pc.name,
                                        teacher = pc.teacher,
                                        classroom = pc.classroom,
                                        dayOfWeek = pc.dayOfWeek,
                                        startSlot = pc.startSlot,
                                        endSlot = pc.endSlot,
                                        weekStart = pc.weekStart,
                                        weekEnd = pc.weekEnd,
                                        oddEven = pc.oddEven,
                                        colorIndex = colorIdx
                                    )
                                )
                            }
                            CredentialStore.lastSyncTime = System.currentTimeMillis()
                            _uiState.update {
                                it.copy(
                                    syncStatus = SyncStatus.Success("同步成功，共 ${courses.size} 门课程"),
                                    lastSyncTime = CredentialStore.lastSyncTime
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(syncStatus = SyncStatus.Error("同步失败: ${e.message}"))
                }
            }
        }
    }

    fun clearSyncStatus() {
        _uiState.update { it.copy(syncStatus = SyncStatus.Idle) }
    }
}
