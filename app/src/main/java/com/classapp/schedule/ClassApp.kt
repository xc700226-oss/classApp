package com.classapp.schedule

import android.app.Application
import android.webkit.WebView
import com.classapp.schedule.BuildConfig
import com.classapp.schedule.data.local.AppDatabase
import com.classapp.schedule.data.repository.ScheduleRepository
import com.classapp.schedule.util.WeekUtils

class ClassApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ScheduleRepository by lazy { ScheduleRepository(database.courseDao()) }

    override fun onCreate() {
        super.onCreate()
        WeekUtils.init(this)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
