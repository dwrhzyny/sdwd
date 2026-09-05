package com.example.productivitygate

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * מסנכרן משימות + סטטוס "סיימתי היום" מול שרת משותף (Firebase Realtime Database),
 * כדי שהטלפון והמחשב יראו את אותו הדבר. ראו README להוראות הקמת השרת.
 *
 * הכלל: "מי שעדכן אחרון מנצח" (last-write-wins) לפי חותמת זמן updated_at.
 * רק רשימת המשימות והסטטוס היומי מסונכרנים - אפליקציות חסומות וטיימרים
 * נשארים מקומיים לכל מכשיר (כי שמות האפליקציות שונים בין אנדרואיד לווינדוס).
 */
object SyncManager {

    private const val TAG = "SyncManager"

    private fun endpoint(context: Context): String? {
        val base = PrefsHelper.getSyncUrl(context)
        if (base.isBlank()) return null
        return base.trimEnd('/') + "/sync.json"
    }

    /** שולח את המצב המקומי לשרת. קורא לזה אחרי כל שינוי (הוספת משימה, סימון סיום). */
    fun push(context: Context) {
        val url = endpoint(context) ?: return
        Thread {
            try {
                val payload = JSONObject().apply {
                    put("tasks", JSONArray(PrefsHelper.getTasks(context)))
                    put("tasks_done", PrefsHelper.isTasksDone(context))
                    put("date", PrefsHelper.getTodayDateString())
                    put("updated_at", System.currentTimeMillis())
                }
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode // מפעיל את הבקשה בפועל
                conn.disconnect()
                PrefsHelper.setUpdatedAt(context, payload.getLong("updated_at"))
            } catch (e: Exception) {
                Log.w(TAG, "sync push failed: ${e.message}")
            }
        }.start()
    }

    /** מושך מהשרת וממזג פנימה. onUpdated נקרא ב-thread ברקע אם משהו השתנה. */
    fun pull(context: Context, onUpdated: (() -> Unit)? = null) {
        val url = endpoint(context) ?: return
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (text.isBlank() || text == "null") return@Thread

                val remote = JSONObject(text)
                val remoteUpdatedAt = remote.optLong("updated_at", 0L)
                val localUpdatedAt = PrefsHelper.getUpdatedAt(context)
                if (remoteUpdatedAt <= localUpdatedAt) return@Thread

                var changed = false
                if (remote.has("tasks")) {
                    val arr = remote.getJSONArray("tasks")
                    val tasks = (0 until arr.length()).map { arr.getString(it) }
                    PrefsHelper.setTasks(context, tasks)
                    changed = true
                }
                if (remote.optString("date") == PrefsHelper.getTodayDateString() &&
                    remote.has("tasks_done")
                ) {
                    PrefsHelper.setTasksDone(context, remote.getBoolean("tasks_done"))
                    changed = true
                }
                // מיישרים את חותמת הזמן המקומית לזו שהתקבלה, כדי לא ליצור לולאת עדכונים
                PrefsHelper.setUpdatedAt(context, remoteUpdatedAt)

                if (changed) onUpdated?.invoke()
            } catch (e: Exception) {
                Log.w(TAG, "sync pull failed: ${e.message}")
            }
        }.start()
    }
}
