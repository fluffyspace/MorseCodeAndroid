package com.example.morsecode

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.models.Postavke
import kotlinx.coroutines.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [VisualFeedbackFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VisualFeedbackFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var progressbar_down:CustomProgressBarView
    lateinit var progressbar_up:CustomProgressBarView
    lateinit var playground_text: TextView
    lateinit var timer_text: TextView
    lateinit var all_timers_text: TextView
    lateinit var korutina: Job
    var mAccessibilityService:MorseCodeService? = null
    var oneTimeUnit: Int = 0
    var up_or_down:Boolean = false
    var testing: Boolean = true
    var layout1: Boolean = false

    interface Listener {
        fun onTranslation(changeText: String)
        fun finish(gotovo: Boolean)
    }

    private var listener: Listener? = null

    fun setListener(l: Listener) {
        listener = l
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_visual_feedback_message,container,false)

        if (layout1){
            view = inflater.inflate(R.layout.fragment_visual_feedback_message,container,false)
        }

        progressbar_down = view.findViewById<CustomProgressBarView>(R.id.custom_progress_down)
        progressbar_up = view.findViewById<CustomProgressBarView>(R.id.custom_progress_up)
        playground_text = view.findViewById(R.id.playground_text)
        timer_text = view.findViewById(R.id.timer)
        all_timers_text = view.findViewById(R.id.all_timers_text)
        checkService()
        // Inflate the layout for this fragment
        return view
    }

    fun touchListener(view: View, event: MotionEvent){
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                down()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                up()
                view.performClick()
            }
        }
    }

    fun up(){
        Log.d("ingo", "up")
        mAccessibilityService?.onKeyPressed()
        refreshText()
        up_or_down = false
        progressbar_down.setNewProgress(0)
        updateGraphics(0)
        cancelKorutina()
        korutina = lifecycleScope.launch(Dispatchers.Default){
            petlja()
        }
    }

    fun down(){
        Log.d("ingo", "down")
        mAccessibilityService?.onKeyPressed()
        refreshText()
        up_or_down = true
        progressbar_up.setNewProgress(0)
        cancelKorutina()
        korutina = lifecycleScope.launch(Dispatchers.Default){
            petlja()
        }
    }

    fun checkService(){
        mAccessibilityService = MorseCodeService.getSharedInstance()
        val (_, _, oneTimeUnitLong) = mAccessibilityService?.getPostavke() ?: Postavke(-1, -1, -1)
        oneTimeUnit = oneTimeUnitLong.toInt()
        progressbar_down.updateThings(0, oneTimeUnit, -1)
        progressbar_up.updateThings(oneTimeUnit, oneTimeUnit*3, oneTimeUnit*7)
    }

    fun refreshText(){
        if(mAccessibilityService?.buttonHistory?.size!! >= 2) {
            playground_text.text = "Text: " + mAccessibilityService?.getMessage()
            if (listener != null){
                mAccessibilityService?.getMessage()?.let { listener!!.onTranslation(it) }
            }
        }
    }

    fun setMessage(text: String) {
        playground_text.text = text
    }

    fun getMessage(): String {
        return playground_text.text.toString()
    }

    fun updateGraphics(progress: Int){
        timer_text.text = progress.toString() + " ms"
        all_timers_text.text = "Morse: " + mAccessibilityService?.getMorse()
        if(up_or_down){
            // down
            progressbar_down.setNewProgress(progress)
            if(progress > oneTimeUnit*7){
                cancelKorutina()
                if(testing) {
                    mAccessibilityService?.buttonHistory?.clear()
                    if (listener != null){
                        listener!!.finish(false)
                    }
                }
                //progressbar_up.setNewProgress(0)
                // send happens
            }
        } else {
            // up
            progressbar_up.setNewProgress(progress)
            if(progress > oneTimeUnit*7){
                cancelKorutina()
                if(testing) {
                    mAccessibilityService?.buttonHistory?.clear()
                    if (listener != null){
                        listener!!.finish(true)
                    }
                }
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment VisualFeedbackFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            VisualFeedbackFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun reset() {
        cancelKorutina()
        progressbar_down.setNewProgress(0)
        progressbar_up.setNewProgress(0)
        if(testing) mAccessibilityService?.buttonHistory?.clear()
        refreshText()
        all_timers_text.text = "Morse: "
    }

    fun cancelKorutina(){
        if(::korutina.isInitialized && korutina.isActive) {
            korutina.cancel()
        }
    }

    override fun onPause() {
        super.onPause()
        cancelKorutina()
    }
}