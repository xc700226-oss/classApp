package com.classapp.schedule.data.local

import androidx.room.*
import com.classapp.schedule.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSlot")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek ORDER BY startSlot")
    fun getCoursesByDay(dayOfWeek: Int): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
