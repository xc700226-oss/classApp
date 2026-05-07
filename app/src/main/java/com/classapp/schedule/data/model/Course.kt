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
    val weeks: String = "1-20", // e.g. "1-13", "3", "1-4,6,8,10,12,14", "1-13(单)"
    val colorIndex: Int = 0
)
