package com.example.morsecode

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.PorukaDao
import com.example.morsecode.models.Postavke
import com.example.morsecode.models.VibrationMessage
import com.example.morsecode.network.ContactsApi
import com.example.morsecode.network.VibrationMessagesApi
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction0


private val VIBRATE_PATTERN: List<Long> = listOf(500, 500)
private val MORSE = arrayOf(".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----")
private val ALPHANUM:String = "abcdefghijklmnopqrstuvwxyz1234567890"
// ABCDEFGHIJKLMNOPQRSTUVWXYZ

class MorseCodeService: Service(), CoroutineScope{
    val MORSECODE_ON = "MorseCode ON"
    val CHANNEL_ID = "MorseTalk"
    var buttonHistory:MutableList<Int> = mutableListOf()
    var lastTimeMillis:Long = 0L
    lateinit var korutina:Job
    lateinit var vibrator:Vibrator
    var lastMessage: String = ""
    var servicePostavke: Postavke = Postavke(5, 1, 400L)
    var testing:Boolean = false
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    var messageReceiveCallback: KFunction0<Unit>? = null // ovo je callback (funkcija) koji ovaj servis pozove kad završi slanjem poruke, moguće je registrirati bilo koju funkciju u bilo kojem activityu. Koristi se kao momentalni feedback da je poruka poslana.

    var testMode = false
    val ONGOING_NOTIFICATION_ID = 1

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MorseCodeService = this@MorseCodeService
    }

    // Binder given to clients
    private val binder = LocalBinder()

    fun makeWaveformFromText(text: String): List<Long>{
        val vvv: MutableList<Long> = mutableListOf(0)
        Log.d("ingo", "makeWaveformFromText " + text)
        var morse_all:StringBuilder = StringBuilder()
        text.forEach { c ->
            val index:Int = ALPHANUM.indexOfFirst { it == c.lowercaseChar() }
            if(index == -1){
                // samo ubaci razmak jer smo dobili nešto nepoznato
                Log.d("ingo", "_")
                vvv[vvv.lastIndex] = servicePostavke.oneTimeUnit*7
            } else {
                val morse:String = MORSE.get(index)
                morse_all.append(morse)
                for(i: Int in morse.indices) {
                    if(morse[i] == '.'){
                        vvv.add(servicePostavke.oneTimeUnit)
                    } else if(morse[i] == '-'){
                        vvv.add(servicePostavke.oneTimeUnit*3)
                    }
                    if(i != morse.length-1){
                        vvv.add(servicePostavke.oneTimeUnit)
                    } else {
                        vvv.add(servicePostavke.oneTimeUnit*3)
                    }
                }
            }
        }
        Log.d("ingo", "makeWaveformFromText ->" + morse_all.toString())
        Log.d("ingo", vvv.toString())
        return vvv.toList()
    }


    fun onKeyEvent(event: KeyEvent) {
        /*
            KEYCODE_HEADSETHOOK = middle button on phone headset
            KEYCODE_VOLUME_UP = volume up button on phone
            KEYCODE_VOLUME_DOWN = volume down button on phone
         */
        Log.d("ingo", "Key send to MorseCodeService is: " + event.keyCode)
        if(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN){
            testing = !testing
            Log.d("ingo", "testing " + testing)
        } else if(testing && event.action == KeyEvent.ACTION_DOWN){
            if(event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
                vibrator.cancel()
                vibrateWithPWM(makeWaveformFromText("eeerpm"))
                Log.d(
                    "ingo",
                    "aaa " + servicePostavke.pwm_on.toString() + ", sss " + servicePostavke.pwm_off.toString() + " onetimeunit " + servicePostavke.oneTimeUnit.toString()
                )
            }
            if(event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                servicePostavke.pwm_on += 2
                if (servicePostavke.pwm_on > 20) {
                    servicePostavke.pwm_on = 2;
                    servicePostavke.pwm_off += 1;
                }
                if (servicePostavke.pwm_off > 20) {
                    servicePostavke.oneTimeUnit += 100
                }
                Log.d(
                    "ingo",
                    "aaa " + servicePostavke.pwm_on.toString() + ", sss " + servicePostavke.pwm_off.toString() + " onetimeunit " + servicePostavke.oneTimeUnit.toString()
                )
            }
        } else {
            if(event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                onKeyPressed()
            }
        }
    }

    // ovo pozivati kod upisivanja morseovog koda, ovo popunjava buttonHistory s vremenskim razlikama
    fun onKeyPressed(){
        vibrator.cancel()
        val diff:Int = getTimeDifference()
        lastTimeMillis = System.currentTimeMillis()
        buttonHistory.add(diff)
        Log.d("ingo", diff.toString())
        //textView.setText(MORSECODE_ON + ": " + getMessage())
    }

    fun vibrateWithPWM(listWithoutPWM: List<Long>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var nisu_sve_nule = false
            listWithoutPWM.forEach {
                if (it != 0L) {
                    nisu_sve_nule = true
                }
            }
            if (!nisu_sve_nule) return
            var listWithPWM = mutableListOf<Long>()
            listWithoutPWM.forEachIndexed { index, item ->
                if (index % 2 == 0) {
                    listWithPWM.add(item)
                } else {
                    var counter = 0
                    repeat((item / (servicePostavke.pwm_off + servicePostavke.pwm_on)).toInt()) {
                        if (counter % 2 == 0) {
                            listWithPWM.add(servicePostavke.pwm_on)
                        } else {
                            listWithPWM.add(servicePostavke.pwm_off)
                        }
                        counter++
                    }
                    if (counter % 2 == 0) listWithPWM.add(servicePostavke.pwm_on)
                }
            }
            val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(listWithPWM.toLongArray(), -1)
            vibrator.vibrate(vibrationEffect)
        }else{
            vibrator.vibrate(listWithoutPWM.toLongArray(), -1)
        }
    }

    fun createNotification(title: String, text: String): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
        val intentStopService = Intent(this, StopServiceReceiver::class.java)
        val stopServicePendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intentStopService,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_cancel_24, getString(R.string.stopService),
                    stopServicePendingIntent)
                .setOnlyAlertOnce(true)
                .build()
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_cancel_24, getString(R.string.stopService),
                    stopServicePendingIntent)
                .setOnlyAlertOnce(true)
                .build()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setup()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        korutina.cancel()
        scope.cancel()
        Log.d("ingo", "MorseCodeService onDestroy")
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? {

        return binder
    }


    fun setup(){
        createNotificationChannel()
        // Create an overlay and display the action bar
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        vibrateWithPWM(VIBRATE_PATTERN)

        //maybeSendMessageCoroutineLoop()
        korutina = scope.launch {
            // New coroutine that can call suspend functions
            maybeSendMessageCoroutineLoop()
        }
        serviceSharedInstance = this

        servicePostavke.pwm_on = PreferenceManager.getDefaultSharedPreferences(this).getLong("pwm_on", 5)
        servicePostavke.pwm_off = PreferenceManager.getDefaultSharedPreferences(this).getLong("pwm_off", 1)
        servicePostavke.oneTimeUnit = PreferenceManager.getDefaultSharedPreferences(this).getLong("oneTimeUnit", 400)
        servicePostavke.token = PreferenceManager.getDefaultSharedPreferences(this).getString("token", "").toString()

        val notification = createNotification("Morse talk", "Running...")
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    suspend fun maybeSendMessage(){
        //Log.d("ingo", "maybeSendMessage")
        val diff:Int = getTimeDifference()
        if(buttonHistory.size > 0 && diff > servicePostavke.oneTimeUnit*7){
            Log.d("ingo", "buttonHistory not empty, time difference is big enough")
            val porukaIzMorsea:String = getMessage()
            if(porukaIzMorsea.isEmpty()) return
            buttonHistory.clear()
            if(isNumeric(porukaIzMorsea[0].toString())){
                Log.d("ingo", "šalji poruku jer je prvo slovo broj")
                sendMessage(porukaIzMorsea)

            // specijalne naredbe koje ne rezultiraju slanjem na server
            } else if(porukaIzMorsea[0] == 'e') {
                vibrateWithPWM(makeWaveformFromText(lastMessage))
            } else if(porukaIzMorsea[0] == 'l') {
                Log.d("ingo", "aaa " + servicePostavke.pwm_on.toString() + ", sss " + servicePostavke.pwm_off.toString())
            } else if(porukaIzMorsea.length == 2) {
                if(porukaIzMorsea[0] == 'i') {
                    when (porukaIzMorsea[1]) {
                        'e' -> servicePostavke.oneTimeUnit = 100
                        'i' -> servicePostavke.oneTimeUnit = 200
                        's' -> servicePostavke.oneTimeUnit = 300
                        'h' -> servicePostavke.oneTimeUnit = 400 // najvise pase s servicePostavke.pwm_on 5 servicePostavke.pwm_off 1
                        '5' -> servicePostavke.oneTimeUnit = 500
                    }
                    Log.d("ingo", "servicePostavke.oneTimeUnit changed to " + servicePostavke.oneTimeUnit)
                    vibrateWithPWM(makeWaveformFromText("ee"))
                } else if(porukaIzMorsea[0] == 'a') {
                    when(porukaIzMorsea[1]){
                        'e' -> {servicePostavke.pwm_on = 10; servicePostavke.pwm_off=1}
                        'i' -> {servicePostavke.pwm_on = 5; servicePostavke.pwm_off=1}
                        's' -> {servicePostavke.pwm_on = 3; servicePostavke.pwm_off=1}
                        'h' -> {servicePostavke.pwm_on = 10; servicePostavke.pwm_off=2}
                        '5' -> {servicePostavke.pwm_on = 10; servicePostavke.pwm_off=4}
                    }
                    Log.d("ingo", "aaa to " + servicePostavke.pwm_on.toString() + ", sss to " + servicePostavke.pwm_off.toString())
                    vibrateWithPWM(makeWaveformFromText("ee"))
                }
            } else {
                Log.d("ingo", "Neispravna naredba.")
                scope.launch {
                    databaseAddNewPoruka(VibrationMessage(id=null,poruka = "neispravna naredba: "+porukaIzMorsea,vibrate = null))
                }

               setText()
            }
        }
    }

    private suspend fun setText(){
        withContext(Dispatchers.IO){
            //textView.setText(MORSECODE_ON + ": neispravna naredba")
        }
    }

    fun isNumeric(str: String) = str.all { it in '0'..'9' }


    suspend fun sendMessage(stringZaPoslati:String){
        try {
            val response: VibrationMessage = VibrationMessagesApi.retrofitService.sendMessage(stringZaPoslati, servicePostavke.token)
            databaseAddNewPoruka(response)
            withContext(Dispatchers.Main){
                //textView.setText(MORSECODE_ON + ": received " + response.poruka)
                messageReceiveCallback?.let { it() }
            }
            if(response.vibrate != null && response.vibrate != ""){
                Log.d("ingo", "vibrira")
                lastMessage = response.vibrate
                vibrateWithPWM(makeWaveformFromText(response.vibrate) )
            }
            if(response.poruka != null) Log.d("ingo", "poruka: " + response.poruka)
            buttonHistory.clear()
        } catch (e: Exception) {
            Log.d("ingo", "greska " + e.stackTraceToString() + e.message.toString())
        }
    }


    suspend fun maybeSendMessageCoroutineLoop() { // this: CoroutineScope
        while(true) {
            delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
            if(!testMode) maybeSendMessage()
        }
    }

    fun databaseAddNewPoruka(poruka: VibrationMessage) {
        Log.d("ingo", "databaseAddNewPoruka(" + poruka.poruka + ")")
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        porukaDao.insertAll(poruka)
    }

    fun retrofitFetchUserIp(username: String){
        scope.launch {
            ContactsApi.retrofitService.getContact(username);
        }
    }

    fun getMorse():String{
        var sb:StringBuilder = StringBuilder()
        for(i: Int in buttonHistory.indices step 2){
            if(buttonHistory.size <= i+1) break
            val duration = buttonHistory[i+1]
            val delay = if (i != 0) buttonHistory[i] else 0
            if(delay > servicePostavke.oneTimeUnit) {
                sb.append(" ")
            }
            if(delay > servicePostavke.oneTimeUnit*3) {
                sb.append(" ")
            }
            if(duration < servicePostavke.oneTimeUnit) {
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

    // funkcija vraća true ako je kod upisa morseovog koda nakon puštanja tipke prošlo vrijeme veće od servicePostavke.oneTimeUnit (znači počinje se gledati novi znak), bitno kod tutoriala da možemo odrediti je li korisnik pogrešno unio slovo ili samo još nije završio upisivanjem pravog slova
    fun isCharacterFinished():Boolean {
        val diff:Int = getTimeDifference()
        if(buttonHistory.size % 2 == 0 && buttonHistory.size > 0 && diff > servicePostavke.oneTimeUnit){
                return true
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
            if(delay > servicePostavke.oneTimeUnit){ // character change
                val index:Int = MORSE.indexOfFirst { it == sb.toString() }
                if(index != -1) {
                    message.append(ALPHANUM.get(index))
                } else {
                    message.append("?")
                }
                sb.clear()
            }
            if(delay > servicePostavke.oneTimeUnit*3) {
                message.append(" ")
            }
            if(duration < servicePostavke.oneTimeUnit) {
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

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("ingo", "service rebinded")
    }

    companion object{
        // Funkcije u companion objektu u Kotlinu su isto kao static funkcije u Javi, znači mogu se pozvati bez instanciranja
        // U ovom slučaju koristimo funkciju getSharedInstance da bismo iz neke druge klase statičkom funkcijom uzeli instancu servisa (MorseCodeService). Ako servis nije pokrenut, dobit ćemo null.
        // serviceSharedInstance se inicijalizira (postavlja vrijednost) kod pokretanja accessibility servisa, a uništava kod zaustavljanja accessibility servisa

        var serviceSharedInstance:MorseCodeService? = null
        fun getSharedInstance():MorseCodeService?{
            return serviceSharedInstance;
        }
    }

    fun getPostavke(): Postavke {
        return Postavke(servicePostavke.pwm_on, servicePostavke.pwm_off, servicePostavke.oneTimeUnit)
    }

    fun setPostavke(novePostavke: Postavke){
        servicePostavke.pwm_on = novePostavke.pwm_on
        servicePostavke.pwm_off = novePostavke.pwm_off
        servicePostavke.oneTimeUnit = novePostavke.oneTimeUnit
        servicePostavke.token = novePostavke.token
    }

    fun toggleTesting(testing: Boolean){
        testMode = testing
        buttonHistory.clear()
        if(testMode){
            //textView.setBackgroundColor(Color.parseColor("#FFA500"))
            //textView.setText("MorseCode TESTING")
        } else {
            //textView.setBackgroundColor(Color.parseColor("#03DAC5"))
            //textView.setText(MORSECODE_ON)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(::korutina.isInitialized) korutina.cancel()
        Log.d("ingo", "onUnbind")
        serviceSharedInstance = null;
        NotificationManagerCompat.from(this).cancelAll()
        super.onUnbind(intent)
        return true
    }

}