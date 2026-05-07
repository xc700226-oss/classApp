package com.classapp.schedule.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classapp.schedule.ClassApp
import com.classapp.schedule.data.model.Course
import com.classapp.schedule.util.WeekUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val currentWeek: Int = WeekUtils.getCurrentWeek(),
    val courses: List<Course> = emptyList(),
    val displayDateRange: Pair<LocalDate, LocalDate> = WeekUtils.getWeekDateRange(WeekUtils.getCurrentWeek())
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ClassApp).repository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCourses.collect { courses ->
                _uiState.update { it.copy(courses = courses) }
            }
        }
    }

    fun selectWeek(week: Int) {
        _uiState.update {
            it.copy(
                currentWeek = week,
                displayDateRange = WeekUtils.getWeekDateRange(week)
            )
        }
    }

    fun goToCurrentWeek() {
        selectWeek(WeekUtils.getCurrentWeek())
    }

    fun previousWeek() {
        val current = _uiState.value.currentWeek
        if (current > 1) selectWeek(current - 1)
    }

    fun nextWeek() {
        val current = _uiState.value.currentWeek
        if (current < WeekUtils.totalWeeks) selectWeek(current + 1)
    }

    fun getCoursesForWeek(): List<Pair<Course, Boolean>> {
        val week = _uiState.value.currentWeek
        return _uiState.value.courses.map { course ->
            course to WeekUtils.isCourseActiveThisWeek(course.weeks, week)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
