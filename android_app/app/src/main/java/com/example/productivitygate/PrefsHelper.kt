package com.example.productivitygate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * שכבת נתונים מרכזית: משימות, אפליקציות חסומות, טיימרים, וסטטוס יומי.
 * גם המסכים (Activities) וגם ה-AccessibilityService קוראים/כותבים דרך כאן.
 */
object PrefsHelper {

    private const val PREFS_NAME = "productivity_gate_prefs"

    private const val KEY_TASKS = "tasks"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_APP_TIMERS_MINUTES = "app_timers_minutes"
    private const val KEY_STATUS_DATE = "status_date"
    private const val KEY_TASKS_DONE = "tasks_done"
    private const val KEY_APP_USAGE_SECONDS = "app_usage_seconds"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_SYNC_URL = "sync_url"
    private const val KEY_REMINDER_INTERVAL_MINUTES = "reminder_interval_minutes"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun todayString(): String = dateFormat.format(Date())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** מאפס את הסטטוס היומי (משימות + זמן שימוש) אם התחיל יום חדש. קוראים לזה בכל בדיקה. */
    fun resetIfNewDay(context: Context) {
        val p = prefs(context)
        val savedDate = p.getString(KEY_STATUS_DATE, null)
        if (savedDate != todayString()) {
            p.edit()
                .putString(KEY_STATUS_DATE, todayString())
                .putBoolean(KEY_TASKS_DONE, false)
                .putString(KEY_APP_USAGE_SECONDS, "{}")
                .apply()
        }
    }

    // ---------- משימות ----------

    fun getTasks(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TASKS, null) ?: return defaultTasks()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun setTasks(context: Context, tasks: List<String>) {
        val arr = JSONArray()
        tasks.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_TASKS, arr.toString()).apply()
        bumpUpdatedAt(context)
    }

    private fun defaultTasks(): List<String> = listOf(
        "לענות על הודעות/מיילים חשובים",
        "לסיים את המשימה הכי דחופה של היום",
        "30 דקות לימוד / עבודה ממוקדת"
    )

    fun isTasksDone(context: Context): Boolean {
        resetIfNewDay(context)
        return prefs(context).getBoolean(KEY_TASKS_DONE, false)
    }

    fun setTasksDone(context: Context, done: Boolean) {
        resetIfNewDay(context)
        prefs(context).edit().putBoolean(KEY_TASKS_DONE, done).apply()
        bumpUpdatedAt(context)
    }

    // ---------- אפליקציות חסומות ----------

    fun getBlockedPackages(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun addBlockedPackage(context: Context, packageName: String) {
        val current = getBlockedPackages(context).toMutableSet()
        current.add(packageName.trim())
        setBlockedPackages(context, current)
    }

    fun removeBlockedPackage(context: Context, packageName: String) {
        val current = getBlockedPackages(context).toMutableSet()
        current.remove(packageName)
        setBlockedPackages(context, current)
    }

    // ---------- טיימרים יומיים לאפליקציה (בדקות) ----------

    fun getAppTimers(context: Context): Map<String, Int> {
        val raw = prefs(context).getString(KEY_APP_TIMERS_MINUTES, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, Int>()
        obj.keys().forEach { key -> map[key] = obj.getInt(key) }
        return map
    }

    fun setAppTimer(context: Context, packageName: String, minutes: Int) {
        val current = getAppTimers(context).toMutableMap()
        current[packageName.trim()] = minutes
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        prefs(context).edit().putString(KEY_APP_TIMERS_MINUTES, obj.toString()).apply()
    }

    fun removeAppTimer(context: Context, packageName: String) {
        val current = getAppTimers(context).toMutableMap()
        current.remove(packageName)
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        prefs(context).edit().putString(KEY_APP_TIMERS_MINUTES, obj.toString()).apply()
    }

    // ---------- שימוש יומי בפועל (בשניות), לצורך אכיפת הטיימר ----------

    fun getUsageSeconds(context: Context, packageName: String): Int {
        resetIfNewDay(context)
        val raw = prefs(context).getString(KEY_APP_USAGE_SECONDS, "{}") ?: "{}"
        val obj = JSONObject(raw)
        return if (obj.has(packageName)) obj.getInt(packageName) else 0
    }

    fun addUsageSeconds(context: Context, packageName: String, seconds: Int) {
        resetIfNewDay(context)
        val raw = prefs(context).getString(KEY_APP_USAGE_SECONDS, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val current = if (obj.has(packageName)) obj.getInt(packageName) else 0
        obj.put(packageName, current + seconds)
        prefs(context).edit().putString(KEY_APP_USAGE_SECONDS, obj.toString()).apply()
    }

    // ---------- סנכרון עם המחשב ----------

    /** חותמת זמן (מילישניות) של השינוי המקומי האחרון - לצורך "מי עדכן אחרון מנצח". */
    fun getUpdatedAt(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    fun setUpdatedAt(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_UPDATED_AT, value).apply()
    }

    private fun bumpUpdatedAt(context: Context) {
        setUpdatedAt(context, System.currentTimeMillis())
    }

    fun getSyncUrl(context: Context): String =
        prefs(context).getString(KEY_SYNC_URL, "") ?: ""

    fun setSyncUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_SYNC_URL, url.trim()).apply()
    }

    fun getReminderIntervalMinutes(context: Context): Int =
        prefs(context).getInt(KEY_REMINDER_INTERVAL_MINUTES, 120)

    fun setReminderIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_REMINDER_INTERVAL_MINUTES, minutes).apply()
    }

    fun getTodayDateString(): String = todayString()
}
