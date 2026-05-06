package com.classapp.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int, // 1=Mon ... 7=Sun
    val startSlot: Int, // 1-based
    val endSlot: Int,
    val weekStart: Int = 1,
    val weekEnd: Int = 20,
    val oddEven: Int = 0, // 0=all, 1=odd weeks, 2=even weeks
    val colorIndex: Int = 0
)
