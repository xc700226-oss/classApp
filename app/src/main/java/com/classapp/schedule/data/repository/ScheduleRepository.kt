package com.classapp.schedule.data.repository

import com.classapp.schedule.data.local.CourseDao
import com.classapp.schedule.data.model.Course
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val courseDao: CourseDao) {

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()

    fun getCoursesForDay(day: Int): Flow<List<Course>> =
        courseDao.getCoursesByDay(day)

    suspend fun addCourse(course: Course): Long = courseDao.insertCourse(course)

    suspend fun updateCourse(course: Course) = courseDao.updateCourse(course)

    suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course)

    suspend fun deleteAll() = courseDao.deleteAll()
}
