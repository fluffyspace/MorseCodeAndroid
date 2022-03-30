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
    lateinit var playground_text: TextView
    lateinit var timer_text: TextView
    lateinit var all_timers_text: TextView
    lateinit var progressbar_down:CustomProgressBarView
    lateinit var progressbar_up:CustomProgressBarView
    lateinit var service_not_started:TextView

    lateinit var korutina: Job
    var all_timers = mutableListOf<Int>()

    var up_or_down:Boolean = false
    var mAccessibilityService:GlobalActionBarService? = null
    var oneTimeUnit: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        service_not_started = findViewById(R.id.service_not_started)
        service_not_started.setOnClickListener {
            checkService();
        }
        tap_button = findViewById(R.id.tap)
        progressbar_down = findViewById<CustomProgressBarView>(R.id.custom_progress_down)
        progressbar_up = findViewById<CustomProgressBarView>(R.id.custom_progress_up)
        playground_text = findViewById(R.id.playground_text)
        timer_text = findViewById(R.id.timer)
        all_timers_text = findViewById(R.id.all_timers_text)

        val aaa = 10//intent.getStringExtra("aaa")?.toLong()
        val sss = 1//intent.getStringExtra("sss")?.toLong()

        val width = findViewById<CustomProgressBarView>(R.id.custom_progress_down).width


        checkService();
        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> down()//Do Something
                MotionEvent.ACTION_UP -> {
                    up()
                    v.performClick()
                }//Do Something
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
            val (aaaa, sssa, oneTimeUnitLong) = mAccessibilityService?.getPostavke() ?: Postavke(-1, -1, -1)
            oneTimeUnit = oneTimeUnitLong.toInt()
            progressbar_down.updateThings(0, oneTimeUnit, -1)
            progressbar_up.updateThings(oneTimeUnit, oneTimeUnit*3, oneTimeUnit*7)
        }
    }

    fun up(){
        Log.d("ingo", "up")
        mAccessibilityService?.onKeyPressed()
        if(mAccessibilityService != null) all_timers.add(mAccessibilityService?.buttonHistory?.last() ?: -1)
        refreshText()
        up_or_down = false
        progressbar_down.setNewProgress(0)
        updateGraphics(0)
        if(::korutina.isInitialized && korutina.isActive) {
            korutina.cancel()
        }
        korutina = lifecycleScope.launch(Dispatchers.Default){
            petlja()
        }
    }

    fun down(){
        Log.d("ingo", "down")
        mAccessibilityService?.onKeyPressed()
        if(mAccessibilityService != null) all_timers.add(mAccessibilityService?.buttonHistory?.last() ?: -1)
        refreshText()
        up_or_down = true
        progressbar_up.setNewProgress(0)
        if(::korutina.isInitialized && korutina.isActive) {
            korutina.cancel()
        }
        korutina = lifecycleScope.launch(Dispatchers.Default){
            petlja()
        }
    }

    fun refreshText(){
        if(mAccessibilityService?.buttonHistory?.size!! >= 2) {
            playground_text.text = "Text: " + mAccessibilityService?.getMessage()
        }
    }

    fun updateGraphics(progress: Int){
        timer_text.text = progress.toString() + " ms"
        all_timers_text.text = "Morse: " + mAccessibilityService?.getMorse()//all_timers.drop(1).toString()
        if(up_or_down){
            // down
            progressbar_down.setNewProgress(progress)
        } else {
            // up
            progressbar_up.setNewProgress(progress)
            if(progress > oneTimeUnit*7){
                korutina.cancel()
                mAccessibilityService?.buttonHistory?.clear()
                all_timers.clear()
                //progressbar_up.setNewProgress(0)
                // send happens
            }
        }
    }

    suspend fun petlja() { // this: CoroutineScope
        //korutina = launch { // launch a new coroutine and continue
        var counter = 0
        val interval = 20
        while(true) {
            //Log.d("ingo", "test")
            counter += interval
            withContext(Dispatchers.Main){
                updateGraphics(counter)
            }
            delay(interval.toLong()) // non-blocking delay for 1 second (default time unit is ms)
            //Log.d("ingo", "counter " + counter)
            /*maybeSendMessage()*/
        }
        //}
        println("Hello") // main coroutine continues while a previous one is delayed
    }

    override fun onResume() {
        toggleTesting(true)
        super.onResume()
    }

    override fun onPause() {
        toggleTesting(false)
        super.onPause()
    }

    override fun onDestroy() {
        toggleTesting(false)
        super.onDestroy()
    }

    fun toggleTesting(testing:Boolean){
        mAccessibilityService?.toggleTesting(testing)
        if(::korutina.isInitialized && korutina.isActive) {
            korutina.cancel()
        }
    }
}