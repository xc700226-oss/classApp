package com.classapp.schedule

import android.app.Application
import com.classapp.schedule.data.local.AppDatabase
import com.classapp.schedule.data.repository.ScheduleRepository
import com.classapp.schedule.util.CredentialStore
import com.classapp.schedule.util.WeekUtils
import com.classapp.schedule.worker.SyncWorker

class ClassApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ScheduleRepository by lazy { ScheduleRepository(database.courseDao()) }

    override fun onCreate() {
        super.onCreate()
        WeekUtils.init(this)
        CredentialStore.init(this)
        SyncWorker.createNotificationChannel(this)

        if (CredentialStore.autoFetchEnabled && CredentialStore.hasCredentials()) {
            SyncWorker.enqueuePeriodicSync(this)
        }
    }
}
