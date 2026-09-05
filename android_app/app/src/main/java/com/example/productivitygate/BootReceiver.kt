package com.example.productivitygate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * לא חייבים לעשות כאן משהו מיוחד: אנדרואיד מפעיל מחדש שירותי נגישות שהמשתמש
 * הפעיל ידנית באופן אוטומטי אחרי אתחול. ה-receiver הזה שמור לשימוש עתידי
 * (למשל תזכורת בבוקר) ולא נדרש לפעולה כרגע.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PrefsHelper.resetIfNewDay(context)
        }
    }
}
