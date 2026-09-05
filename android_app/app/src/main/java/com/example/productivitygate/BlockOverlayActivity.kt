package com.example.productivitygate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class BlockOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "extra_reason"
        const val REASON_TASKS_NOT_DONE = "tasks_not_done"
        const val REASON_TIMER_UP = "timer_up"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_overlay)

        val reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_TASKS_NOT_DONE
        val message = findViewById<TextView>(R.id.text_block_message)
        message.text = if (reason == REASON_TIMER_UP) {
            getString(R.string.block_message_timer_up)
        } else {
            getString(R.string.block_message_tasks_not_done)
        }

        findViewById<Button>(R.id.button_go_to_tasks).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            finish()
        }

        findViewById<Button>(R.id.button_go_home).setOnClickListener {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }

    // חוסמים את כפתור "חזרה" כדי שלא יחזרו ישר לאפליקציה החסומה
    override fun onBackPressed() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }
}
