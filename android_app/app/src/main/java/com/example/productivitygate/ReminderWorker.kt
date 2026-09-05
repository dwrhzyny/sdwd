package com.example.productivitygate

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * רץ ברקע כל X דקות (לפי reminder_interval_minutes, מינימום 15 דקות - מגבלת אנדרואיד
 * ל-WorkManager תקופתי). אם המשימות של היום עדיין לא סומנו כהושלמו - שולח התראה.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        PrefsHelper.resetIfNewDay(applicationContext)
        if (!PrefsHelper.isTasksDone(applicationContext)) {
            NotificationHelper.showReminder(applicationContext)
        }
        return Result.success()
    }
}
