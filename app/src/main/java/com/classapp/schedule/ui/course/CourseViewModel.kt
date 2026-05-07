package com.classapp.schedule.ui.course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classapp.schedule.ClassApp
import com.classapp.schedule.data.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseFormState(
    val name: String = "",
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int = 1,
    val startSlot: Int = 1,
    val endSlot: Int = 2,
    val weeks: String = "1-20",
    val colorIndex: Int = 0,
    val editingCourseId: Long? = null
)

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ClassApp).repository

    private val _formState = MutableStateFlow(CourseFormState())
    val formState: StateFlow<CourseFormState> = _formState.asStateFlow()

    fun initForAdd(defaultDay: Int?) {
        _formState.value = CourseFormState(
            dayOfWeek = defaultDay ?: 1
        )
    }

    fun initForEdit(course: Course) {
        _formState.value = CourseFormState(
            name = course.name,
            teacher = course.teacher,
            classroom = course.classroom,
            dayOfWeek = course.dayOfWeek,
            startSlot = course.startSlot,
            endSlot = course.endSlot,
            weeks = course.weeks,
            colorIndex = course.colorIndex,
            editingCourseId = course.id
        )
    }

    fun updateName(name: String) { _formState.update { it.copy(name = name) } }
    fun updateTeacher(teacher: String) { _formState.update { it.copy(teacher = teacher) } }
    fun updateClassroom(classroom: String) { _formState.update { it.copy(classroom = classroom) } }
    fun updateDay(day: Int) { _formState.update { it.copy(dayOfWeek = day) } }
    fun updateStartSlot(slot: Int) { _formState.update { it.copy(startSlot = slot) } }
    fun updateEndSlot(slot: Int) { _formState.update { it.copy(endSlot = slot) } }
    fun updateWeeks(weeks: String) { _formState.update { it.copy(weeks = weeks) } }
    fun updateColor(index: Int) { _formState.update { it.copy(colorIndex = index) } }

    fun save(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            val course = Course(
                id = state.editingCourseId ?: 0,
                name = state.name.trim(),
                teacher = state.teacher.trim(),
                classroom = state.classroom.trim(),
                dayOfWeek = state.dayOfWeek,
                startSlot = state.startSlot,
                endSlot = state.endSlot,
                weeks = state.weeks.trim(),
                colorIndex = state.colorIndex
            )

            if (state.editingCourseId != null) {
                repository.updateCourse(course)
            } else {
                repository.addCourse(course)
            }

            onSuccess()
        }
    }
}
