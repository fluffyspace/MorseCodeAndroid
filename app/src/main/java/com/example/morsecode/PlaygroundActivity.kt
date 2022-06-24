package com.example.morsecode

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class PlaygroundActivity : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var visual_feedback_container:VisualFeedbackFragment
    var mAccessibilityService:MorseCodeService? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        tap_button = findViewById(R.id.tap)
        visual_feedback_container = VisualFeedbackFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        mAccessibilityService = MorseCodeService.getSharedInstance();
        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> visual_feedback_container.down()//Do Something
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    v.performClick()
                }
            }
            true
        }
    }

    override fun onResume() {
        toggleTesting(true)
        super.onResume()
    }

    override fun onPause() {
        toggleTesting(false)
        super.onPause()
    }

    fun toggleTesting(testing:Boolean){
        mAccessibilityService?.toggleTesting(testing)
    }
}