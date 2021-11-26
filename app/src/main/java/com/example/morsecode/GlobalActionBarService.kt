package com.example.morsecode

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.example.morsecode.moodel.Poruka
import com.example.morsecode.network.MarsApi
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


private val VIBRATE_PATTERN: List<Long> = listOf(500, 500)

private val MORSE = arrayOf(".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----")
private val ALPHANUM:String = "abcdefghijklmnopqrstuvwxyz1234567890"
// ABCDEFGHIJKLMNOPQRSTUVWXYZ

class GlobalActionBarService: AccessibilityService(), CoroutineScope{
    var mLayout: FrameLayout? = null
    var buttonHistory:MutableList<Int> = mutableListOf()
    var lastTimeMillis:Long = 0L
    lateinit var korutina:Job
    var oneTimeUnit: Long = 400
    lateinit var vibrator:Vibrator
    var lastMessage: String = ""
    var aaa:Long = 5
    var sss:Long = 1

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onServiceConnected() {
        // Create an overlay and display the action bar
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mLayout = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.TOP
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.action_bar, mLayout)
        wm.addView(mLayout, lp)
        /*configurePowerButton()
        configureVolumeButton()*/
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrateee(VIBRATE_PATTERN, aaa, sss)
        //maian()
        korutina = scope.launch {
            // New coroutine that can call suspend functions
            maian()
        }
    }

    fun makeWaveformFromText(text: String): List<Long>{
        val vvv: MutableList<Long> = mutableListOf(0)

        text.forEach { c ->
            Log.d("ingo", c.toString())
            val index:Int = ALPHANUM.indexOfFirst { it == c.lowercaseChar() }
            if(index == -1){
                // razmak
                Log.d("ingo", "unknown")
                vvv[vvv.lastIndex] = oneTimeUnit*7
            } else {
                val morse:String = MORSE.get(index)
                Log.d("ingo", "mors ->" + morse)
                for(i: Int in morse.indices) {
                    if(morse[i] == '.'){
                        vvv.add(oneTimeUnit)
                    } else if(morse[i] == '-'){
                        vvv.add(oneTimeUnit*3)
                    }
                    if(i != morse.length-1){
                        vvv.add(oneTimeUnit)
                    } else {
                        vvv.add(oneTimeUnit*3)
                    }
                }
            }
        }
        Log.d("ingo", vvv.toString())
        return vvv.toList()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("ingo", "Key pressed via accessibility is: " + event.keyCode)
        if(event.keyCode == 79 || event.keyCode == 25){ //25 down, 24 up, 79 ok
            // maybeSendMessage()
            vibrator.cancel()
            val currentTimeMillis:Long = System.currentTimeMillis()
            val diff:Int = (currentTimeMillis-lastTimeMillis).toInt()
            lastTimeMillis = currentTimeMillis
            buttonHistory.add(diff)
            Log.d("ingo", diff.toString())
            //if(event.action == KeyEvent.ACTION_DOWN)
            return true
        }
        //This allows the key pressed to function normally after it has been used by your app.
        return super.onKeyEvent(event)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun vibrateee(lista: List<Long>, aaa:Long, sss:Long) {

        //val vibrator=vibratorm.defaultVibrator
        var nova_lista = mutableListOf<Long>()
        lista.forEachIndexed{index, item ->
            if(index % 2 == 0){
                nova_lista.add(item)
            } else {
                var counter = 0
                repeat((item/(sss+aaa)).toInt()){
                    if(counter % 2 == 0){
                        nova_lista.add(aaa)
                    } else {
                        nova_lista.add(sss)
                    }
                    counter++
                }
                if(counter % 2 == 0) nova_lista.add(aaa)
            }
        }
        //val wave_ampl = IntArray(lista.size, { i -> (if (i%2 == 0) 0 else 1 ) })
        //Log.d("ingo", wave_ampl.toString())
        //Log.d("ingo", lista.toString())
        vibrator.vibrate(VibrationEffect.createWaveform(nova_lista.toLongArray(), -1))
        //vibrator.vibrate(VIBRATE_PATTERN, 0)

    }

    override fun onDestroy() {
        korutina.cancel()
        scope.cancel()
        Log.d("ingo", "oncancel")
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun maybeSendMessage(){
        val currentTimeMillis:Long = System.currentTimeMillis()
        val diff:Int = (currentTimeMillis-lastTimeMillis).toInt()

        if(buttonHistory.size > 0 && diff > 5000){
            Log.d("ingo", "inside")
            //sendMessage()
            val moje:String = getMessage()
            if(isNumeric(moje[0].toString())){
                Log.d("ingo", "ide jer je numericki")
                sendMessage(moje)
            } else if(moje[0] == 'e') {
                vibrateee(makeWaveformFromText(lastMessage), aaa, sss)
            } else if(moje[0] == 'i') {
                when(moje[1]){
                    'e' -> oneTimeUnit = 100
                    'i' -> oneTimeUnit = 200
                    's' -> oneTimeUnit = 300
                    'h' -> oneTimeUnit = 400 // najvise pase a 5 s 1
                    '5' -> oneTimeUnit = 500
                }
                Log.d("ingo", "oneTimeUnit changed to " + oneTimeUnit)
                vibrateee(makeWaveformFromText("ee"), aaa, sss)
            } else if(moje[0] == 'a') {
                when(moje[1]){
                    'e' -> {aaa = 10; sss=1}
                    'i' -> {aaa = 5; sss=1}
                    's' -> {aaa = 3; sss=1}
                    'h' -> {aaa = 10; sss=2}
                    '5' -> {aaa = 10; sss=4}
                }
                Log.d("ingo", "aaa to " + aaa.toString() + ", sss to " + sss.toString())
                vibrateee(makeWaveformFromText("ee"), aaa, sss)
            }
        }
    }

    fun isNumeric(str: String) = str.all { it in '0'..'9' }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun sendMessage(moje:String){
        /*withContext(Dispatchers.IO) {

        }*/
        //thread { // this: CoroutineScope
        //    launch {
                try {
                    //val stt:String = MarsApi.retrofitService.sendMessage(getMessage())

                    val stringCall:Poruka = MarsApi.retrofitService.sendMessage(moje)

                    buttonHistory.clear()
                    if(stringCall.vibrate != null){
                        Log.d("ingo", "vibrira")
                        lastMessage = stringCall.vibrate
                        vibrateee(makeWaveformFromText(stringCall.vibrate), aaa, sss )
                    }
                    //stringCall.enqueue()
                    /*stringCall.enqueue( object: Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.isSuccessful()) {
                                val responseString = response.body();
                                Log.d("ingo", "responseString " + responseString.toString())
                                // todo: do something with the response string
                            } else {
                                Log.d("ingo", "response not succ")
                            }
                        }
                        override fun onFailure(call: Call<String>, t: Throwable) {
                            t.printStackTrace()
                            Log.d("ingo", "++++ fail " + t.message)
                        }
                    })*/
                    if(stringCall.poruka != null) Log.d("ingo", "++++" + stringCall.poruka)


                } catch (e: Exception) {
                    Log.d("ingo", "greska " + e.stackTraceToString() + e.message.toString())
                }

            //}
        //}
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun maian() { // this: CoroutineScope
        //korutina = launch { // launch a new coroutine and continue
            while(true) {
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                //Log.d("ingo", "test")
                maybeSendMessage()
                /*maybeSendMessage()*/
            }
        //}
        println("Hello") // main coroutine continues while a previous one is delayed
    }

    private fun getMessage():String{
        /*var aaa:StringBuilder = StringBuilder()
        for(p:Int in buttonHistory){
            aaa.append(p.toString() + " ")
        }
        Log.d("ingo", "ints are " + aaa.toString())*/
        var message:StringBuilder = StringBuilder()
        var sb:StringBuilder = StringBuilder()
        for(i: Int in buttonHistory.indices step 2){
            val duration = buttonHistory[i+1]
            val delay = if (i != 0) buttonHistory[i] else 0
            if(delay > 600){
                val index:Int = MORSE.indexOfFirst { it == sb.toString() }
                if(index != -1) {
                    message.append(ALPHANUM.get(index))
                } else {
                    message.append("-")
                }
                sb.clear()
            }
            if(delay > 2000) {
                message.append(" ")
            }
            if(duration < 200){
                sb.append(".")
            } else {
                sb.append("-")
            }

        }
        if(sb.length > 0){
            val index:Int = MORSE.indexOfFirst { it == sb.toString() }
            if(index != -1) {
                message.append(ALPHANUM.get(index))
            } else {
                message.append("-")
            }
            sb.clear()
        }
        buttonHistory.clear()
        Log.d("ingo", "mess->" + message.toString()) // print after delay
        return message.toString()
    }

    private fun getIntsMessage():String{
        return buttonHistory.toString()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(::korutina.isInitialized) korutina.cancel()
        Log.d("ingo", "onbind")
        return super.onUnbind(intent)
    }
}