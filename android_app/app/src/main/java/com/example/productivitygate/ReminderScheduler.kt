package com.example.productivitygate

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "productivity_gate_reminder"

    /**
     * מתזמן את התזכורת התקופתית. אנדרואיד לא מאפשר ל-WorkManager תקופתי
     * לרוץ בתדירות גבוהה מ-15 דקות, אז אם המשתמש הגדיר פחות מזה - נעגל ל-15.
     *
     * replace=false (ברירת מחדל): לא נוגע בתזמון קיים אם כבר יש אחד (למשל בכל פתיחת אפליקציה).
     * replace=true: מבטל ומתזמן מחדש - להשתמש כשהמשתמש משנה את מרווח התזכורת בהגדרות.
     */
    fun schedule(context: Context, replace: Boolean = false) {
        val minutes = PrefsHelper.getReminderIntervalMinutes(context).coerceAtLeast(15)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .build()

        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }
}
