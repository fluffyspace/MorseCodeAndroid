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
import com.example.morsecode.moodel.Zadatak
import com.example.morsecode.network.MarsApi
import kotlinx.coroutines.*

class SendMorseActivity : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var service_not_started:TextView
    lateinit var visual_feedback_container:VisualFeedbackFragment

    var mAccessibilityService:GlobalActionBarService? = null
    lateinit var korutina_refresh_text: Job

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_morse)
        service_not_started = findViewById(R.id.service_not_started)
        service_not_started.setOnClickListener {
            checkService();
        }
        tap_button = findViewById(R.id.tap)

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = false
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        checkService();
        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    visual_feedback_container.down()
                    refreshText()
                }
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    refreshText()
                    v.performClick()
                }
            }
            true
        }
        refreshMessages()
    }

    fun refreshMessages(){
        val zadaciRecyclerView: RecyclerView = findViewById(R.id.zadaciRecyclerView)
        zadaciRecyclerView.layoutManager = LinearLayoutManager(this)
        val context = this
        lifecycleScope.launch(Dispatchers.Default){
            try {
                val zadaci:List<Zadatak> = MarsApi.retrofitService.getAll("all", if (mAccessibilityService != null) mAccessibilityService?.token.toString() else "")
                withContext(Dispatchers.Main){
                    zadaciRecyclerView.adapter = ZadaciAdapter(context, zadaci)
                }
            } catch (e: Exception) {
                Log.d("ingo", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    suspend fun refreshTextJob() { // this: CoroutineScope
        while(true) {
            delay(100)
            withContext(Dispatchers.Main) {
                refreshText()
            }
        }
    }

    fun refreshText(){
        if(mAccessibilityService?.buttonHistory?.size!! >= 2) {
            var tekst = mAccessibilityService?.getMessage()
            tekst = tekst?.replace("\\s".toRegex(), "")
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
            mAccessibilityService?.setMessageFeedback(::refreshMessages)
        }
    }

    fun cancelKorutina(){
        if(::korutina_refresh_text.isInitialized && korutina_refresh_text.isActive) {
            korutina_refresh_text.cancel()
        }
    }

    override fun onResume() {
        korutina_refresh_text = lifecycleScope.launch(Dispatchers.Default){
            refreshTextJob()
        }
        mAccessibilityService?.setMessageFeedback(::refreshMessages)
        super.onResume()
    }

    override fun onPause() {
        cancelKorutina()
        mAccessibilityService?.setMessageFeedback(null)
        super.onPause()
    }
}