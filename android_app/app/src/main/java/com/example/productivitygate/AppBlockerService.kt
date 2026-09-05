package com.example.productivitygate

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * שירות נגישות שרץ ברקע, מזהה איזו אפליקציה פתוחה כרגע (foreground),
 * וחוסם אותה (מציג את מסך החסימה) אם:
 *  1) האפליקציה ברשימת "האפליקציות החסומות" והמשימות של היום לא סומנו כהושלמו, או
 *  2) לאפליקציה יש טיימר יומי, והזמן היומי שהוקצה לה נגמר.
 *
 * כדי להפעיל: הגדרות -> נגישות -> Productivity Gate -> הפעל.
 */
class AppBlockerService : AccessibilityService() {

    private var currentPackage: String? = null
    private val tickHandler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        startTicking()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentPackage = pkg
            checkAndBlockIfNeeded(pkg)
        }
    }

    override fun onInterrupt() {
        // לא נדרש כאן
    }

    /** בודק מיד עם מעבר לאפליקציה חדשה אם צריך לחסום אותה. */
    private fun checkAndBlockIfNeeded(packageName: String) {
        if (packageName == this.packageName) return
        if (packageName in IGNORED_PACKAGES) return

        PrefsHelper.resetIfNewDay(this)

        val blockedPackages = PrefsHelper.getBlockedPackages(this)
        val tasksDone = PrefsHelper.isTasksDone(this)

        if (packageName in blockedPackages && !tasksDone) {
            launchBlockScreen(reason = BlockOverlayActivity.REASON_TASKS_NOT_DONE)
            return
        }

        val timers = PrefsHelper.getAppTimers(this)
        val limitMinutes = timers[packageName]
        if (limitMinutes != null) {
            val usedSeconds = PrefsHelper.getUsageSeconds(this, packageName)
            if (usedSeconds >= limitMinutes * 60) {
                launchBlockScreen(reason = BlockOverlayActivity.REASON_TIMER_UP)
            }
        }
    }

    /** לולאה שרצה כל שנייה: מוסיפה זמן שימוש לאפליקציה הפתוחה כרגע אם יש לה טיימר. */
    private fun startTicking() {
        tickRunnable = object : Runnable {
            override fun run() {
                val pkg = currentPackage
                if (pkg != null && pkg != packageName) {
                    val timers = PrefsHelper.getAppTimers(this@AppBlockerService)
                    if (timers.containsKey(pkg)) {
                        PrefsHelper.addUsageSeconds(this@AppBlockerService, pkg, 1)
                        checkAndBlockIfNeeded(pkg)
                    }
                }
                tickHandler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
        tickHandler.postDelayed(tickRunnable!!, TICK_INTERVAL_MS)
    }

    private fun launchBlockScreen(reason: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_REASON, reason)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        tickRunnable?.let { tickHandler.removeCallbacks(it) }
    }
}
