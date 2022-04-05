package com.example.morsecode

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.PorukaDao
import com.example.morsecode.moodel.Poruka
import com.example.morsecode.moodel.Postavke
import com.example.morsecode.network.MarsApi
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction0


private val VIBRATE_PATTERN: List<Long> = listOf(500, 500)

private val MORSE = arrayOf(".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----")
private val ALPHANUM:String = "abcdefghijklmnopqrstuvwxyz1234567890"
// ABCDEFGHIJKLMNOPQRSTUVWXYZ

class GlobalActionBarService: AccessibilityService(), CoroutineScope{
    val MORSECODE_ON = "MorseCode ON"
    val CHANNEL_ID = "MorseTalk"
    var mLayout: FrameLayout? = null
    var buttonHistory:MutableList<Int> = mutableListOf()
    var lastTimeMillis:Long = 0L
    lateinit var korutina:Job
    var oneTimeUnit: Long = 400
    var token: String = ""
    lateinit var vibrator:Vibrator
    var lastMessage: String = ""
    var aaa:Long = 5
    var sss:Long = 1
    var testing:Boolean = false
    lateinit var k:ToneGenerator
    var tonebr:Int = 0
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    lateinit var textView: TextView
    var messageReceiveCallback: KFunction0<Unit>? = null

    var testMode = false

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onServiceConnected() {
        createNotificationChannel()
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
        var view = inflater.inflate(R.layout.action_bar, mLayout)
        wm.addView(mLayout, lp)
        textView = view.findViewById<TextView>(R.id.power)
        /*configurePowerButton()
        configureVolumeButton()*/
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrateee(VIBRATE_PATTERN)
        //maian()
        korutina = scope.launch {
            // New coroutine that can call suspend functions
            maian()
        }
        sSharedInstance = this
        //k = ToneGenerator(STREAM_MUSIC, 10)

        aaa = PreferenceManager.getDefaultSharedPreferences(this).getLong("aaa", 5)
        sss = PreferenceManager.getDefaultSharedPreferences(this).getLong("sss", 1)
        oneTimeUnit = PreferenceManager.getDefaultSharedPreferences(this).getLong("oneTimeUnit", 400)
        token = PreferenceManager.getDefaultSharedPreferences(this).getString("token", "").toString()

        createNotification()
    }

    fun makeWaveformFromText(text: String): List<Long>{
        val vvv: MutableList<Long> = mutableListOf(0)
        Log.d("ingo", "makeWaveformFromText " + text)
        var morse_all:StringBuilder = StringBuilder()
        text.forEach { c ->
            val index:Int = ALPHANUM.indexOfFirst { it == c.lowercaseChar() }
            if(index == -1){
                // razmak
                Log.d("ingo", "_")
                vvv[vvv.lastIndex] = oneTimeUnit*7
            } else {
                val morse:String = MORSE.get(index)
                morse_all.append(morse)
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
        Log.d("ingo", "makeWaveformFromText ->" + morse_all.toString())
        Log.d("ingo", vvv.toString())
        return vvv.toList()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("ingo", "Key pressed via accessibility is: " + event.keyCode)
        if(event.keyCode == 24 && event.action == KeyEvent.ACTION_DOWN){
            /*tonebr++
            if(tonebr > 100) tonebr = 0
            k.startTone(tonebr, 2000)*/
//55
            // 87 kao na vokitokiju
            // 88
            testing = !testing
            Log.d("ingo", "testing " + testing)
//            Log.d("ingo", "tonebr " + tonebr)
        } else if(testing && event.action == KeyEvent.ACTION_DOWN){

            if(event.keyCode == 79){
                vibrator.cancel()
                vibrateee(makeWaveformFromText("eeerpm"))
                Log.d(
                    "ingo",
                    "aaa " + aaa.toString() + ", sss " + sss.toString() + " onetimeunit " + oneTimeUnit.toString()
                )
            }
            if(event.keyCode == 25) { //25 down, 24 up, 79 ok
                aaa += 2
                if (aaa > 20) {
                    aaa = 2;
                    sss += 1;
                }
                if (sss > 20) {
                    oneTimeUnit += 100
                }
                Log.d(
                    "ingo",
                    "aaa " + aaa.toString() + ", sss " + sss.toString() + " onetimeunit " + oneTimeUnit.toString()
                )
            }
        } else {
            if(event.keyCode == 79 || event.keyCode == 25) {
                onKeyPressed()
                //if(event.action == KeyEvent.ACTION_DOWN)
                return true
            }
        }
        //This allows the key pressed to function normally after it has been used by your app.
        return super.onKeyEvent(event)
    }

    fun onKeyPressed(){
        vibrator.cancel()
        val diff:Int = getTimeDifference()
        lastTimeMillis = System.currentTimeMillis()
        buttonHistory.add(diff)
        Log.d("ingo", diff.toString())
        textView.setText(MORSECODE_ON + ": " + getMessage())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun vibrateee(lista: List<Long>) {
        var nisu_sve_nule = false
        lista.forEach{
            if(it != 0L){
                nisu_sve_nule = true
            }
        }
        if(!nisu_sve_nule) return
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

    fun createNotification(){
        // Create an explicit intent for an Activity in your app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
            .setContentTitle("Morse Talk")
            .setContentText("Running...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            /*.setStyle(NotificationCompat.MessagingStyle("Me")
                .setConversationTitle("Team lunch")
                .addMessage("Hi", timestamp1, null) // Pass in null for user.
                .addMessage("What's up?", timestamp2, "Coworker")
                .addMessage("Not much", timestamp3, null)
                .addMessage("How about lunch?", timestamp4, "Coworker"))*/

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        korutina.cancel()
        scope.cancel()
        Log.d("ingo", "oncancel")
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun maybeSendMessage(){
        val diff:Int = getTimeDifference()
        Log.d("ingo", "maybeSendMessage")
        if(buttonHistory.size > 0 && diff > oneTimeUnit*14){
            Log.d("ingo", "inside")
            //sendMessage()
            val moje:String = getMessage()
            if(moje.isEmpty()) return
            buttonHistory.clear()
            if(isNumeric(moje[0].toString())){
                Log.d("ingo", "ide jer je numericki")
                sendMessage(moje)
            } else if(moje[0] == 'e') {
                vibrateee(makeWaveformFromText(lastMessage))
            } else if(moje[0] == 'l') {
                Log.d("ingo", "aaa " + aaa.toString() + ", sss " + sss.toString())

            } else if(moje.length == 2) {
                // specijalne naredbe koje ne rezultiraju slanjem na server
                if(moje[0] == 'i') {
                    when (moje[1]) {
                        'e' -> oneTimeUnit = 100
                        'i' -> oneTimeUnit = 200
                        's' -> oneTimeUnit = 300
                        'h' -> oneTimeUnit = 400 // najvise pase a 5 s 1
                        '5' -> oneTimeUnit = 500
                    }
                    Log.d("ingo", "oneTimeUnit changed to " + oneTimeUnit)
                    vibrateee(makeWaveformFromText("ee"))
                } else if(moje[0] == 'a') {
                    when(moje[1]){
                        'e' -> {aaa = 10; sss=1}
                        'i' -> {aaa = 5; sss=1}
                        's' -> {aaa = 3; sss=1}
                        'h' -> {aaa = 10; sss=2}
                        '5' -> {aaa = 10; sss=4}
                    }
                    Log.d("ingo", "aaa to " + aaa.toString() + ", sss to " + sss.toString())
                    vibrateee(makeWaveformFromText("ee"))
                }
            } else {
                Log.d("ingo", "Neispravna naredba.")
                scope.launch {
                    databaseAddNewPoruka(Poruka(id=null,poruka = "neispravna naredba: "+moje,vibrate = null))
                }
                textView.setText(MORSECODE_ON + ": neispravna naredba")
            }
        }
    }

    fun isNumeric(str: String) = str.all { it in '0'..'9' }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun sendMessage(stringZaPoslati:String){
        try {
            val poruka:Poruka = MarsApi.retrofitService.sendMessage(stringZaPoslati, token)
            databaseAddNewPoruka(poruka)
            withContext(Dispatchers.Main){
                textView.setText(MORSECODE_ON + ": received " + poruka.poruka)
                messageReceiveCallback?.let { it() }
            }
            if(poruka.vibrate != null && poruka.vibrate != ""){
                Log.d("ingo", "vibrira")
                lastMessage = poruka.vibrate
                vibrateee(makeWaveformFromText(poruka.vibrate) )
            }
            if(poruka.poruka != null) Log.d("ingo", "poruka: " + poruka.poruka)
            buttonHistory.clear()
        } catch (e: Exception) {
            Log.d("ingo", "greska " + e.stackTraceToString() + e.message.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun maian() { // this: CoroutineScope
        while(true) {
            delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
            if(!testMode) maybeSendMessage()
        }
    }

    fun databaseAddNewPoruka(poruka: Poruka) {
        Log.d("ingo", "databaseAddNewPoruka(" + poruka.poruka + ")")
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        porukaDao.insertAll(poruka)
    }

    fun getMorse():String{
        var sb:StringBuilder = StringBuilder()
        for(i: Int in buttonHistory.indices step 2){
            if(buttonHistory.size <= i+1) break
            val duration = buttonHistory[i+1]
            val delay = if (i != 0) buttonHistory[i] else 0
            if(delay > oneTimeUnit) {
                sb.append(" ")
            }
            if(delay > oneTimeUnit*3) {
                sb.append(" ")
            }
            if(duration < oneTimeUnit) {
                sb.append("•")
            } else {
                sb.append("–")
            }
        }
        Log.d("ingo", "morse->" + sb.toString()) // print after delay
        return sb.toString()
    }

    fun setMessageFeedback(callback: KFunction0<Unit>?){
        messageReceiveCallback = callback
    }

    fun isCharacterFinished():Boolean {
        val diff:Int = getTimeDifference()
        if(buttonHistory.size % 2 == 0 && buttonHistory.size > 0){
            if(diff > oneTimeUnit){
                return true
            }
        }
        return false
    }

    fun getTimeDifference(): Int{
        val currentTimeMillis:Long = System.currentTimeMillis()
        return (currentTimeMillis-lastTimeMillis).toInt()
    }

    fun getMessage():String{
        var message:StringBuilder = StringBuilder()
        var sb:StringBuilder = StringBuilder()
        for(i: Int in buttonHistory.indices step 2){
            if(buttonHistory.size <= i+1) break
            val duration = buttonHistory[i+1]
            val delay = if (i != 0) buttonHistory[i] else 0
            if(delay > oneTimeUnit){ // character change
                val index:Int = MORSE.indexOfFirst { it == sb.toString() }
                if(index != -1) {
                    message.append(ALPHANUM.get(index))
                } else {
                    message.append("?")
                }
                sb.clear()
            }
            if(delay > oneTimeUnit*3) {
                message.append(" ")
            }
            if(duration < oneTimeUnit) {
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
                message.append("?")
            }
            sb.clear()
        }

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

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("ingo", "binded1!!")
    }

    companion object{
        var sSharedInstance:GlobalActionBarService? = null
        fun getSharedInstance():GlobalActionBarService?{
            return sSharedInstance;
        }
    }

    fun getPostavke(): Postavke {
        return Postavke(aaa, sss, oneTimeUnit)
    }

    fun setPostavke(postavke: Postavke){
        aaa = postavke.aaa
        sss = postavke.sss
        oneTimeUnit = postavke.oneTimeUnit
        token = postavke.token
    }

    fun toggleTesting(testing: Boolean){
        testMode = testing
        buttonHistory.clear()
        if(testMode){
            textView.setBackgroundColor(Color.parseColor("#FFA500"))
            textView.setText("MorseCode TESTING")
        } else {
            textView.setBackgroundColor(Color.parseColor("#03DAC5"))
            textView.setText(MORSECODE_ON)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(::korutina.isInitialized) korutina.cancel()
        Log.d("ingo", "onUnbind")
        sSharedInstance = null;
        NotificationManagerCompat.from(this).cancelAll()
        super.onUnbind(intent)
        return true
    }

}