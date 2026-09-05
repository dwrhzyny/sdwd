package com.example.productivitygate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        refreshLists()
        findViewById<EditText>(R.id.edit_sync_url).setText(PrefsHelper.getSyncUrl(this))
        findViewById<EditText>(R.id.edit_reminder_minutes)
            .setText(PrefsHelper.getReminderIntervalMinutes(this).toString())

        // --- הוספת משימה ---
        findViewById<Button>(R.id.button_add_task).setOnClickListener {
            val input = findViewById<EditText>(R.id.edit_new_task)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                val tasks = PrefsHelper.getTasks(this).toMutableList()
                tasks.add(text)
                PrefsHelper.setTasks(this, tasks)
                SyncManager.push(this)
                input.text.clear()
                refreshLists()
            }
        }

        // --- הוספת אפליקציה חסומה (לפי שם חבילה, package name) ---
        findViewById<Button>(R.id.button_add_blocked).setOnClickListener {
            val input = findViewById<EditText>(R.id.edit_blocked_package)
            val pkg = input.text.toString().trim()
            if (pkg.isNotEmpty()) {
                PrefsHelper.addBlockedPackage(this, pkg)
                input.text.clear()
                refreshLists()
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_package), Toast.LENGTH_SHORT).show()
            }
        }

        // --- הוספת טיימר לאפליקציה ---
        findViewById<Button>(R.id.button_add_timer).setOnClickListener {
            val pkgInput = findViewById<EditText>(R.id.edit_timer_package)
            val minutesInput = findViewById<EditText>(R.id.edit_timer_minutes)
            val pkg = pkgInput.text.toString().trim()
            val minutesText = minutesInput.text.toString().trim()
            val minutes = minutesText.toIntOrNull()
            if (pkg.isNotEmpty() && minutes != null && minutes > 0) {
                PrefsHelper.setAppTimer(this, pkg, minutes)
                pkgInput.text.clear()
                minutesInput.text.clear()
                refreshLists()
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_package_and_minutes), Toast.LENGTH_SHORT).show()
            }
        }

        // --- שמירת כתובת סנכרון ---
        findViewById<Button>(R.id.button_save_sync).setOnClickListener {
            val url = findViewById<EditText>(R.id.edit_sync_url).text.toString().trim()
            PrefsHelper.setSyncUrl(this, url)
            if (url.isNotEmpty()) {
                SyncManager.push(this)
            }
            Toast.makeText(this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
        }

        // --- שמירת מרווח תזכורות ---
        findViewById<Button>(R.id.button_save_reminder).setOnClickListener {
            val minutesText = findViewById<EditText>(R.id.edit_reminder_minutes).text.toString().trim()
            val minutes = minutesText.toIntOrNull()
            if (minutes != null && minutes > 0) {
                PrefsHelper.setReminderIntervalMinutes(this, minutes)
                ReminderScheduler.schedule(this, replace = true)
                Toast.makeText(this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_minutes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshLists() {
        val tasksText = PrefsHelper.getTasks(this).joinToString("\n") { "\u2022 $it" }
        findViewById<TextView>(R.id.text_current_tasks).text = tasksText

        val blockedText = PrefsHelper.getBlockedPackages(this).joinToString("\n") { "\u2022 $it" }
        findViewById<TextView>(R.id.text_current_blocked).text = blockedText

        val timersText = PrefsHelper.getAppTimers(this)
            .entries.joinToString("\n") { (pkg, minutes) -> "\u2022 $pkg — $minutes " + getString(R.string.minutes_suffix) }
        findViewById<TextView>(R.id.text_current_timers).text = timersText
    }
}
