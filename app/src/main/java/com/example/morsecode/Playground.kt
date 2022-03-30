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
import com.example.morsecode.moodel.Postavke
import kotlinx.coroutines.*
import java.lang.StringBuilder

class Playground : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var service_not_started:TextView
    lateinit var visual_feedback_container:VisualFeedbackFragment
    var mAccessibilityService:GlobalActionBarService? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        service_not_started = findViewById(R.id.service_not_started)
        service_not_started.setOnClickListener {
            checkService();
        }
        tap_button = findViewById(R.id.tap)
        visual_feedback_container = VisualFeedbackFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        checkService();
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

    fun checkService(){
        mAccessibilityService = GlobalActionBarService.getSharedInstance();
        if(mAccessibilityService == null) {
            service_not_started.visibility = View.VISIBLE
            tap_button.isEnabled = false
        } else {
            service_not_started.visibility = View.GONE
            tap_button.isEnabled = true
            toggleTesting(true)
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