package com.example.morsecode

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.moodel.Postavke
import kotlinx.coroutines.*
import java.lang.StringBuilder
import kotlin.random.Random

class TutorialActivity : AppCompatActivity() {
    lateinit var tap_button: Button
    lateinit var service_not_started:TextView
    lateinit var tutorial_status_text:TextView
    lateinit var tutorial_status_image:ImageView
    lateinit var tutorial_text_view:TextView
    lateinit var tutorial_number_view:TextView
    lateinit var visual_feedback_container:VisualFeedbackFragment
    var mAccessibilityService:GlobalActionBarService? = null
    var tutorial_number:Int = 0
    var tutorial_text:String = ""
    var text_samples:MutableList<String> = mutableListOf("the","of","and","a","to","in","is","you","that","it","he","was","for","on","are","as","with","his","they","I","at","be","this","have","from","or","one","had","by","word","but","not","what","all","were","we","when","your","can","said","there","use","an","each","which","she","do","how","their","if","will","up","other","about","out","many","then","them","these","so","some","her","would","make","like","him","into","time","has","look","two","more","write","go","see","number","no","way","could","people","my","than","first","water","been","call","who","oil","its","now","find","long","down","day","did","get","come","made","may","part")
    lateinit var korutina_next_tutorial: Job
    lateinit var korutina_refresh_text: Job

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        service_not_started = findViewById(R.id.service_not_started)
        service_not_started.setOnClickListener {
            checkService();
        }
        tap_button = findViewById(R.id.tap)
        tutorial_status_text = findViewById(R.id.tutorial_status_text)
        tutorial_status_image = findViewById(R.id.tutorial_status_image)
        tutorial_text_view = findViewById(R.id.tutorial_text_view)
        tutorial_number_view = findViewById(R.id.tutorial_number_view)

        visual_feedback_container = VisualFeedbackFragment()
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
        generateNewTest()
    }

    suspend fun nextTutorial() { // this: CoroutineScope
        delay(3000)
        withContext(Dispatchers.Main){
            generateNewTest()
            tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
            tutorial_status_text.text = "So far so good"
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
            if(tekst != null){
                if(tutorial_text == tekst){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst + " - Bravo! Loading new test..."
                    if(::korutina_next_tutorial.isInitialized && korutina_next_tutorial.isActive) {
                        korutina_next_tutorial.cancel()
                    }
                    korutina_next_tutorial = lifecycleScope.launch(Dispatchers.Default){
                        nextTutorial()
                    }
                    visual_feedback_container.reset()
                } else if(tutorial_text.startsWith(tekst)){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst + " - So far so good"
                } else if(tekst.length > 1 && tutorial_text.startsWith(tekst.slice(IntRange(0, tekst.length-2))) && mAccessibilityService?.isCharacterFinished() == false){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_check_circle_24)
                    tutorial_status_text.text = tekst.slice(IntRange(0, tekst.length-2)) + " - So far so good"
                } else if(tekst.length > 0 && mAccessibilityService?.isCharacterFinished() == true){
                    tutorial_status_image.setImageResource(R.drawable.ic_baseline_cancel_24)
                    tutorial_status_text.text = tekst + " - Wrong"
                }
            }
        }
    }

    fun generateNewTest(){
        tutorial_number++
        val random_sample_text_index = Random.nextInt(0, text_samples.size)
        tutorial_text = text_samples[random_sample_text_index].lowercase()
        text_samples.removeAt(random_sample_text_index)

        tutorial_number_view.text = "Tutorial: " + tutorial_number + "/20"
        tutorial_text_view.text = "Pokušajte upisati: " + tutorial_text
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

    fun cancelKorutina(){
        if(::korutina_next_tutorial.isInitialized && korutina_next_tutorial.isActive) {
            korutina_next_tutorial.cancel()
        }
        if(::korutina_refresh_text.isInitialized && korutina_refresh_text.isActive) {
            korutina_refresh_text.cancel()
        }
    }

    override fun onResume() {
        toggleTesting(true)
        korutina_refresh_text = lifecycleScope.launch(Dispatchers.Default){
            refreshTextJob()
        }
        super.onResume()
    }

    override fun onPause() {
        toggleTesting(false)
        cancelKorutina()
        super.onPause()
    }

    fun toggleTesting(testing:Boolean){
        mAccessibilityService?.toggleTesting(testing)
    }
}