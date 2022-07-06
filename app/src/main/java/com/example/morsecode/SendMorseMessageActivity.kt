package com.example.morsecode

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.ZadaciAdapter
import com.example.morsecode.models.Zadatak
import kotlinx.coroutines.*

class SendMorseMessageActivity : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var visual_feedback_container:VisualFeedbackFragment

    var mAccessibilityService:MorseCodeService? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_morse_message)
        tap_button = findViewById(R.id.tap)

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = false
        visual_feedback_container.layout1 = false
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        mAccessibilityService = MorseCodeService.getSharedInstance();

        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    visual_feedback_container.down()
                }
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    v.performClick()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        mAccessibilityService?.setMessageFeedback(null)
        super.onPause()
    }
}