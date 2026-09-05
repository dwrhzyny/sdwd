package com.example.productivitygate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val checkBoxes = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()
        ReminderScheduler.schedule(this)

        findViewById<Button>(R.id.button_enable_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.button_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.button_mark_done).setOnClickListener {
            markDoneIfAllChecked()
        }

        findViewById<Button>(R.id.button_not_done).setOnClickListener {
            PrefsHelper.setTasksDone(this, false)
            SyncManager.push(this)
            Toast.makeText(this, getString(R.string.toast_still_blocked), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.button_add_task_quick).setOnClickListener {
            val input = findViewById<EditText>(R.id.edit_new_task_quick)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                val tasks = PrefsHelper.getTasks(this).toMutableList()
                tasks.add(text)
                PrefsHelper.setTasks(this, tasks)
                SyncManager.push(this)
                input.text.clear()
                buildChecklist()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // מושכים עדכון מהמחשב (אם מוגדר סנכרון) ואז בונים את המסך מחדש
        SyncManager.pull(this) {
            runOnUiThread { buildChecklist() }
        }
        buildChecklist()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    private fun buildChecklist() {
        PrefsHelper.resetIfNewDay(this)
        val container = findViewById<LinearLayout>(R.id.container_tasks)
        container.removeAllViews()
        checkBoxes.clear()

        val tasksDone = PrefsHelper.isTasksDone(this)
        val tasks = PrefsHelper.getTasks(this)

        for (task in tasks) {
            val checkBox = CheckBox(this)
            checkBox.text = task
            checkBox.isChecked = tasksDone
            checkBox.textDirection = android.view.View.TEXT_DIRECTION_RTL
            checkBox.textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_END
            container.addView(checkBox)
            checkBoxes.add(checkBox)
        }

        val statusText = findViewById<TextView>(R.id.text_status)
        statusText.text = if (tasksDone) {
            getString(R.string.status_unlocked)
        } else {
            getString(R.string.status_locked)
        }
    }

    private fun markDoneIfAllChecked() {
        if (checkBoxes.isEmpty() || checkBoxes.all { it.isChecked }) {
            PrefsHelper.setTasksDone(this, true)
            SyncManager.push(this)
            Toast.makeText(this, getString(R.string.toast_unlocked), Toast.LENGTH_SHORT).show()
            buildChecklist()
        } else {
            Toast.makeText(this, getString(R.string.toast_not_all_checked), Toast.LENGTH_SHORT).show()
        }
    }
}
