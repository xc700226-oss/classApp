package com.classapp.schedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.classapp.schedule.data.model.Course

@Database(entities = [Course::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "class_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
