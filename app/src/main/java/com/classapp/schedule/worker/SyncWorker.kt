package com.classapp.schedule.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.classapp.schedule.ClassApp
import com.classapp.schedule.data.fetcher.FetchResult
import com.classapp.schedule.data.fetcher.ScheduleFetcher
import com.classapp.schedule.data.importer.SchoolParser
import com.classapp.schedule.data.model.Course
import com.classapp.schedule.util.CredentialStore
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "ScheduleSyncWorker"
        private const val WORK_NAME = "schedule_periodic_sync"
        private const val NOTIFICATION_CHANNEL_ID = "schedule_sync_channel"
        private const val NOTIFICATION_ID = 1001

        const val KEY_RESULT_MESSAGE = "result_message"
        const val KEY_COURSE_COUNT = "course_count"
        const val KEY_SUCCESS = "success"

        fun enqueuePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            ).addTag(TAG).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "已注册24小时周期同步任务")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "已取消周期同步任务")
        }

        fun enqueueOneTimeSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "已触发手动同步")
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "课表同步",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "后台自动同步课表时显示"
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "开始同步课表...")

        if (!CredentialStore.hasCredentials()) {
            Log.w(TAG, "未配置凭据，跳过同步")
            return Result.failure(
                Data.Builder()
                    .putBoolean(KEY_SUCCESS, false)
                    .putString(KEY_RESULT_MESSAGE, "未配置教务系统凭据")
                    .build()
            )
        }

        val portalUrl = CredentialStore.portalUrl
        val username = CredentialStore.username
        val password = CredentialStore.password

        setForeground(createForegroundInfo("正在同步课表..."))

        val fetcher = ScheduleFetcher()
        val fetchResult = fetcher.fetchSchedule(portalUrl, username, password)

        when (fetchResult) {
            is FetchResult.Error -> {
                Log.e(TAG, "获取课表失败: ${fetchResult.message}")
                showNotification("课表同步失败", fetchResult.message)
                return Result.failure(
                    Data.Builder()
                        .putBoolean(KEY_SUCCESS, false)
                        .putString(KEY_RESULT_MESSAGE, fetchResult.message)
                        .build()
                )
            }
            is FetchResult.Success -> {
                val courses = try {
                    SchoolParser.parseHtml(fetchResult.html)
                } catch (e: Exception) {
                    Log.e(TAG, "解析课表HTML失败", e)
                    showNotification("课表解析失败", "无法解析课表页面: ${e.message}")
                    return Result.failure(
                        Data.Builder()
                            .putBoolean(KEY_SUCCESS, false)
                            .putString(KEY_RESULT_MESSAGE, "解析失败: ${e.message}")
                            .build()
                    )
                }

                if (courses.isEmpty()) {
                    Log.w(TAG, "解析结果为空")
                    showNotification("课表同步完成", "未找到课程数据")
                    return Result.failure(
                        Data.Builder()
                            .putBoolean(KEY_SUCCESS, false)
                            .putString(KEY_RESULT_MESSAGE, "未找到课程数据")
                            .build()
                    )
                }

                val app = applicationContext as ClassApp
                val repository = app.repository
                try {
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
                    Log.i(TAG, "同步成功，共 ${courses.size} 门课程")
                    showNotification("课表同步成功", "已更新 ${courses.size} 门课程")

                    return Result.success(
                        Data.Builder()
                            .putBoolean(KEY_SUCCESS, true)
                            .putInt(KEY_COURSE_COUNT, courses.size)
                            .putString(KEY_RESULT_MESSAGE, "同步成功，共 ${courses.size} 门课程")
                            .build()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "保存课程失败", e)
                    return Result.failure(
                        Data.Builder()
                            .putBoolean(KEY_SUCCESS, false)
                            .putString(KEY_RESULT_MESSAGE, "保存失败: ${e.message}")
                            .build()
                    )
                }
            }
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("课程表同步")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun showNotification(title: String, content: String) {
        createNotificationChannel(applicationContext)
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
