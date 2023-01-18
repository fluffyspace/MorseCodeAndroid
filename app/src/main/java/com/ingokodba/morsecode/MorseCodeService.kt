package com.ingokodba.morsecode

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.ingokodba.morsecode.Adapters.CommandListener
import com.ingokodba.morsecode.baza.AppDatabase
import com.ingokodba.morsecode.baza.MessageDao
import com.ingokodba.morsecode.baza.PorukaDao
import com.ingokodba.morsecode.models.*
import com.ingokodba.morsecode.models.Message
import com.ingokodba.morsecode.network.getContactsApiService
import com.ingokodba.morsecode.network.getMessagesApiService
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URISyntaxException
import java.util.List.copyOf
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction0


// ABCDEFGHIJKLMNOPQRSTUVWXYZ

class MorseCodeService: Service(), CoroutineScope, PhysicalButtonsService.OnKeyListener{
    val MORSECODE_ON = "MorseCode ON"
    val NOTIFICATION_CHANNEL_ID = "MorseTalk"
    var buttonHistory:MutableList<Int> = mutableListOf()
    var lastTimeMillis:Long = 0L
    lateinit var korutina:Job
    lateinit var vibrating_queue_korutina:Job
    lateinit var vibrator:Vibrator
    var last_vibrated: String = ""
    var servicePostavke: Postavke = Postavke()
    var testing:Boolean = false
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    var messageReceiveCallback: KFunction0<Unit>? = null // ovo je callback (funkcija) koji ovaj servis pozove kad završi slanjem poruke, moguće je registrirati bilo koju funkciju u bilo kojem activityu. Koristi se kao momentalni feedback da je poruka poslana.
    lateinit var sharedPreferences: SharedPreferences
    lateinit var mSocket: Socket
    var will_stop_vibrating: Long = -1
    var showOverlay: Boolean = true
    var dont_check_input: Boolean = false

    var current_menu: MorseCodeServiceMenus = MorseCodeServiceMenus.MENU_MAIN
    var entering_text: Boolean = false
    var current_contact_index: Int = -1
    var current_file_index: Int = -1
    var current_file_line_index: Int = -1
    var last_contact_index_played: Int = -1
    var contacts: MutableList<Contact> = mutableListOf()
    var files: MutableList<OpenedFile> = mutableListOf()
    var file_content: MutableList<String> = mutableListOf()
    var messages: MutableList<Message> = mutableListOf()
    var current_message_index: Int = -1
    var last_command_issued: MorseCodeServiceCommands = MorseCodeServiceCommands.MAIN
    var luck_follows_me: Boolean = true

    var testMode = false
    val ONGOING_NOTIFICATION_ID = 1
    val POLLING_WAIT_TIME = 10000

    var profile: LegProfile? = null
    var lastCommand: MorseCodeServiceCommands? = null
    var vibrate_new_messages: Boolean = true
    var vibrating_queue: MutableList<String> = mutableListOf()
    var search_query: String = ""

    var last_checked_for_new_messages: Long = -1

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

    fun commandIssued(command: MorseCodeServiceCommands, sufix: String = ""){
        commandListener?.commandChanged(command)
        PhysicalButtonsService.getSharedInstance()?.changeActionBarText(command, sufix)
        lastCommand = command
    }

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
                morse_all.append(morse + " ")
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

    fun cancelVibration(){
        vibrator.cancel()
        if(::vibrating_queue_korutina.isInitialized) vibrating_queue_korutina.cancel()
        will_stop_vibrating = -1
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
                cancelVibration()
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
        cancelVibration()
        val diff:Int = getTimeDifference()
        lastTimeMillis = System.currentTimeMillis()
        buttonHistory.add(diff)
        Log.d("ingo", "onKeyPressed " + diff.toString())
        //textView.setText(MORSECODE_ON + ": " + getMessage())
    }

    fun transformToPWM(listWithoutPWM: List<Long>): MutableList<Long>{
        var nisu_sve_nule = false
        listWithoutPWM.forEach {
            if (it != 0L) {
                nisu_sve_nule = true
            }
        }
        if (!nisu_sve_nule) return mutableListOf()
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
        return listWithPWM
    }

    fun onVibrationEnd(){
        if(vibrating_queue.isNotEmpty()){
            val message = vibrating_queue.removeAt(0)
            vibrate(message)
        }
    }

    fun startVibratingQueueCoroutineIfNotStarted(vibrationEndsIn: Long){
        if(::vibrating_queue_korutina.isInitialized && !vibrating_queue_korutina.isActive) {
            vibrating_queue_korutina = scope.launch {
                delay(vibrationEndsIn + 1000)
                withContext(Dispatchers.Main) {
                    onVibrationEnd()
                }
            }
        }
    }

    fun vibrate(text: String){
        if(text == "") return
        val isvibrating = isVibrating()
        if(isvibrating > 0){
            vibrating_queue.add(text)
            startVibratingQueueCoroutineIfNotStarted(isvibrating)
        } else {
            Log.d("ingo", "$text")
            last_vibrated = text
            commandListener?.lastCharactersVibrated(text)
            vibrateWithPWM(makeWaveformFromText(text))
        }
    }

    fun isVibrating(): Long{
        return will_stop_vibrating-System.currentTimeMillis()
    }

    fun vibrate(duration: Long){
        this.vibrate(longArrayOf(duration, duration))
    }

    fun vibrate(longArray: LongArray){
        val isvibrating = isVibrating()
        if(isvibrating <= 0){
            vibrateWithPWM(longArray.toList())
        }
    }

    fun vibrateWithPWM(listWithoutPWM: List<Long>) {
        var vibration_time: Long = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var listWithPWM = transformToPWM(listWithoutPWM)
            val vibrationEffect: VibrationEffect = VibrationEffect.createWaveform(listWithPWM.toLongArray(), -1)
            vibrator.vibrate(vibrationEffect)
            vibration_time = listWithPWM.toLongArray().reduce { acc, l -> acc+l }
            will_stop_vibrating = System.currentTimeMillis()+vibration_time
        }else{
            vibrator.vibrate(listWithoutPWM.toLongArray(), -1)
            vibration_time = listWithoutPWM.toLongArray().reduce { acc, l -> acc+l }
            will_stop_vibrating = System.currentTimeMillis()+vibration_time
        }
        if(vibrating_queue.isNotEmpty()){
            startVibratingQueueCoroutineIfNotStarted(vibration_time)
        }
    }

    fun createNotification(title: String, text: String, color: Int, bigText: String): Notification {
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
            PendingIntent.FLAG_IMMUTABLE
        )

        val intentAccesibilitySettings = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val accesibilitySettingsPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intentAccesibilitySettings,
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
                .setColor(color)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_cancel_24, getString(R.string.stopService),
                    stopServicePendingIntent)
                .addAction(R.drawable.ic_baseline_play_arrow_24, getString(R.string.accesibilitySettings),
                    accesibilitySettingsPendingIntent)
                .setOnlyAlertOnce(true)
                .build()
        } else {
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
                .setColor(color)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_baseline_cancel_24, getString(R.string.stopService),
                    stopServicePendingIntent)
                .addAction(R.drawable.ic_baseline_play_arrow_24, getString(R.string.accesibilitySettings),
                    accesibilitySettingsPendingIntent)
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
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ingo", "service started")
        setup()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        korutina.cancel()
        if(::vibrating_queue_korutina.isInitialized) vibrating_queue_korutina.cancel()
        scope.cancel()
        mSocket.disconnect();
        mSocket.off("new message", onNewMessage);
        Log.d("ingo", "MorseCodeService onDestroy")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun setup(){
        sharedPreferences = this.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)
        loadPostavke()

        createNotificationChannel()

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        serviceSharedInstance = this

        if(PhysicalButtonsService.getSharedInstance() != null)
        {
            notifyAccesibilityActive()
        } else {
            notifyAccesibilitInactive()
        }

        try {
            mSocket = IO.socket(servicePostavke.socketioIp);
            Log.d("connect0", "not error");
        } catch (e: URISyntaxException) {
            Log.e("connect0", "error");
        }
        mSocket.on("new message", onNewMessage);
        mSocket.on(Socket.EVENT_CONNECT) { Log.d("ingo", "socket connected") }
        mSocket.on(Socket.EVENT_DISCONNECT) { Log.d("ingo", "socket disconnected") }
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onError)
        mSocket.disconnect().connect()

        getContacts()
        getFiles()
        getNewMessages()
        last_checked_for_new_messages = System.currentTimeMillis()

        korutina = scope.launch {
            // New coroutine that can call suspend functions
            maybeSendMessageCoroutineLoop()
        }

        PhysicalButtonsService.getSharedInstance()?.addListener(this)
    }

    fun notifyAccesibilityActive(){
        if(servicePostavke.physicalButtons.isNotEmpty()) {
            PhysicalButtonsService.getSharedInstance()?.physicalButtons = copyOf(servicePostavke.physicalButtons).toMutableList()
        }
        val notification = createNotification("MorseTalk", getString(R.string.accesibility_service_active), Color.GREEN, getString(R.string.accesibility_service_active))
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun notifyAccesibilitInactive(){
        val notification = createNotification("MorseTalk", getString(R.string.accesibility_service_inactive), Color.parseColor("#FF8000"), getString(R.string.accesibility_service_inactive_bigtext))
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun accesibilityServiceOn(){
        PhysicalButtonsService.getSharedInstance()?.addListener(this)
        notifyAccesibilityActive()
    }

    fun accesibilityServiceOff(){
        PhysicalButtonsService.getSharedInstance()?.addListener(this)
        notifyAccesibilitInactive()
    }

    fun getContacts(){
        scope.launch {
            try {
                val kontakti: List<Contact> =
                    getContactsApiService(this@MorseCodeService).getMyFriends()
                withContext(Dispatchers.Main){
                    contacts = kontakti.toMutableList()
                    if(contacts.isNotEmpty()) current_contact_index = 0
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska getMyFriends " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    fun getFiles(){
        ReadFilesActivity
        scope.launch(Dispatchers.Default) {
            val previouslyOpenedFiles = ReadFilesActivity.getPreviouslyOpenedFiles(this@MorseCodeService)
            withContext(Dispatchers.Main) {
                files = previouslyOpenedFiles
                if(files.isNotEmpty()) {
                    current_file_index = 0
                } else {
                    current_file_index = -1
                }
            }
        }
    }

    private fun getNewMessages(vibrate_if_new: Boolean = false) {
        scope.launch(Dispatchers.Default) {
            try {
                val response: List<Message> = getMessagesApiService(this@MorseCodeService).getNewMessages()
                if (response.isNotEmpty()) {
                    for (message in response) {
                        withContext(Dispatchers.Main) {
                            if(vibrate_if_new) vibrate(message.message.toString())
                            saveMessage(message)
                            for(listener in socketListeners) {
                                listener.onNewMessage(message)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska getNewMessages " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    private fun saveMessage(message: Message) {
        try {
            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(message)
            Log.e("stjepan", "db uspjelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db nije uspjelo " + e.stackTraceToString() + e.message.toString())
        }
    }

    fun loadChat(){
        scope.launch(Dispatchers.Default) {
            try {
                val db = AppDatabase.getInstance(this@MorseCodeService)
                val messageDao: MessageDao = db.messageDao()
                val poruke: List<Message> = messageDao.getAllReceived(contacts[current_contact_index].id!!.toInt(), servicePostavke.userId)
                withContext(Dispatchers.Main){
                    messages = poruke as MutableList<Message>
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska getNewMessages " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    fun sendMessage2(text: String){
        scope.launch(Dispatchers.Default) {
            try {
                val id = getMessagesApiService(this@MorseCodeService).sendMessage(
                    to=contacts[current_contact_index].id!!.toInt(),
                    message=text
                )
                Log.d("stjepan", "poslana poruka? $id")
                withContext(Dispatchers.Main) {
                    if (id != -1L) {
                        Log.d("ingo", "poruka $id uspješno poslana")
                        val poruka =
                            Message(id, text, contacts[current_contact_index].id!!.toInt(), servicePostavke.userId, System.currentTimeMillis().toString(), false, false)
                        saveMessage(poruka)
                        messages.add(poruka)
                        emitMessage(poruka)
                        vibrate(VIBRATION_OK)
                    } else {
                        vibrate(VIBRATION_NOT_OK)
                        Log.d("ingo", "poruka nije poslana")
                    }
                }
            } catch (e: Exception) {
                vibrate(VIBRATION_NOT_OK)
                Log.e("Stjepan", "nije poslana poruka '$text' prema korisnikom s id-om " + contacts[current_contact_index].id!!.toInt())
                Log.e("stjepan", "greska performSendMessage " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    fun MAIN_MENU(poruka: String){
        when(poruka[0]){
            CONTACTS_KEY -> {
                commandIssued(MorseCodeServiceCommands.CONTACTS)
                if(contacts.isEmpty()){
                    Log.d("ingo", "Contacts empty")
                } else {
                    current_menu = MorseCodeServiceMenus.MENU_CHOOSE_CONTACTS
                    vibrate(contacts[current_contact_index].username)
                    current_message_index = -1
                }
            }
            FILES_KEY -> {
                current_menu = MorseCodeServiceMenus.MENU_READ_FROM_FILES
                commandIssued(MorseCodeServiceCommands.FILES)
                if(files.isEmpty()){
                    Log.d("ingo", "Files empty")
                } else {
                    current_file_index = 0
                    vibrate(files[current_file_index].filename)
                }
            }
            INTERNET_KEY -> {
                current_menu = MorseCodeServiceMenus.MENU_SEARCH_INTERNET
                commandIssued(MorseCodeServiceCommands.INTERNET)
            }
            else -> {
                current_menu = MorseCodeServiceMenus.MENU_MAIN
                commandIssued(MorseCodeServiceCommands.MAIN)
            }
        }
    }

    fun CONTACTS_MENU(poruka: String){
        when(poruka[0]){
            SHORTCUT_NEXT -> {
                current_contact_index++
                if(current_contact_index > contacts.size-1){
                    current_contact_index = 0
                }
                vibrate(contacts[current_contact_index].username)
                Log.d("ingo", "sljedeći kontakt - " + contacts[current_contact_index].username)
                commandIssued(MorseCodeServiceCommands.CONTACTS_NEXT)
            }
            SHORTCUT_PREVIOUS -> {
                current_contact_index--
                if(current_contact_index < 0){
                    current_contact_index = contacts.size-1
                }
                vibrate(contacts[current_contact_index].username)
                Log.d("ingo", "prijašnji kontakt - " + contacts[current_contact_index].username)
                commandIssued(MorseCodeServiceCommands.CONTACTS_PREVIOUS)
            }
            SHORTCUT_EXTRA -> {
                vibrate(contacts[current_contact_index].username)
                Log.d("ingo", "vibriram trenutnog kontakta - " + contacts[current_contact_index].username)
            }
            SHORTCUT_CONFIRM -> {
                Log.d("ingo", "učitavam chat s - " + contacts[current_contact_index].username)
                current_menu = MorseCodeServiceMenus.MENU_CONTACT_CHAT
                vibrate(VIBRATION_OK)
                loadChat()
                commandIssued(MorseCodeServiceCommands.CONTACTS_CHOOSE, " - " + contacts[current_contact_index].username)
            }
        }
    }

    fun MENU_CHOOSE_FILE(poruka: String){
        when(poruka[0]){
            SHORTCUT_NEXT -> {
                current_file_index++
                if(current_file_index > files.size-1){
                    current_file_index = 0
                }
                vibrate(files[current_file_index].filename)
                Log.d("ingo", "sljedeća datoteka - " + files[current_file_index].filename)
                commandIssued(MorseCodeServiceCommands.FILES_CHOOSE_NEXT)
            }
            SHORTCUT_PREVIOUS -> {
                current_file_index--
                if(current_file_index < 0){
                    current_file_index = files.size-1
                }
                vibrate(files[current_file_index].filename)
                Log.d("ingo", "prijašnja datoteka - " + files[current_file_index].filename)
                commandIssued(MorseCodeServiceCommands.FILES_CHOOSE_PREVIOUS)
            }
            SHORTCUT_EXTRA -> {
                vibrate(files[current_file_index].filename)
                Log.d("ingo", "vibriram trenutnu datoteka - " + files[current_file_index].filename)
            }
            SHORTCUT_CONFIRM -> {
                Log.d("ingo", "učitavam datoteku " + files[current_file_index].filename)
                current_menu = MorseCodeServiceMenus.MENU_READ_FROM_FILES
                vibrate(VIBRATION_OK)
                file_content = ReadFilesActivity.readFromFile(files[current_file_index].uri.toUri(), contentResolver).split("\n").toMutableList()
                Log.d("ingo", file_content.toString())
                commandIssued(MorseCodeServiceCommands.FILES_OPEN, " - " + files[current_file_index].filename)
            }
        }
    }

    fun READ_FROM_FILES_MENU(poruka: String){
        when(poruka[0]){
            SHORTCUT_NEXT -> {
                current_file_line_index++
                if(current_file_line_index > file_content.size-1){
                    current_file_line_index = 0
                }
                vibrate(file_content[current_file_line_index])
                Log.d("ingo", "sljedeća linija ${current_file_line_index} - ${file_content[current_file_line_index]}")
                commandIssued(MorseCodeServiceCommands.FILES_NEXT_LINE)
            }
            SHORTCUT_PREVIOUS -> {
                current_file_line_index--
                if(current_file_line_index < 0){
                    current_file_line_index = file_content.size-1
                }
                vibrate(file_content[current_file_line_index])
                Log.d("ingo", "sljedeća linija ${current_file_line_index} - ${file_content[current_file_line_index]}")
                commandIssued(MorseCodeServiceCommands.FILES_PREVIOUS_LINE)
            }
            'f' -> {
                // search in file
                current_menu = MorseCodeServiceMenus.MENU_SEARCH_IN_FILE
                search_query = poruka.drop(1).trim().lowercase()
                Log.d("ingo", "search in file - " + search_query)
            }
            SHORTCUT_EXTRA -> {
                // search in file
                current_menu = MorseCodeServiceMenus.MENU_CHOOSE_FILE
                commandIssued(MorseCodeServiceCommands.FILES_PICKER)
                Log.d("ingo", "to menu MENU_CHOOSE_FILE")
            }
            SHORTCUT_CONFIRM -> {
                // write to file
                Log.d("ingo", "učitavam datoteku " + files[current_file_index].filename)
                //current_menu = MorseCodeServiceMenus.MENU_OPEN_FILE
                vibrate(VIBRATION_OK)
                loadChat()

            }
        }
    }

    fun CONTACT_CHAT_MENU(poruka: String){
        when(poruka[0]){
            SHORTCUT_NEXT -> {
                if(current_message_index < messages.size-1){
                    current_message_index++
                    messages[current_message_index].message?.let { vibrate(it) }
                    Log.d("ingo", "skačem na poruku naprijed - " + messages[current_message_index].message)
                } else {
                    vibrate(VIBRATION_NOT_OK)
                }
                commandIssued(MorseCodeServiceCommands.CHAT_NEXT)
            }
            SHORTCUT_PREVIOUS -> {
                if(current_message_index > 0){
                    current_message_index--
                    messages[current_message_index].message?.let { vibrate(it) }
                    Log.d("ingo", "skačem na poruku prije - " + messages[current_message_index].message)
                } else {
                    vibrate(VIBRATION_NOT_OK)
                }
                commandIssued(MorseCodeServiceCommands.CHAT_PREVIOUS)
            }
            SHORTCUT_EXTRA -> {
                current_message_index = messages.size-1
                if(messages.size > 0) {
                    messages[current_message_index].message?.let { vibrate(it) }
                }
                Log.d("ingo", "vibriram zadnju poruku")
                commandIssued(MorseCodeServiceCommands.CHAT_LAST_MESSAGE)
            }
            SHORTCUT_CONFIRM -> {
                commandIssued(MorseCodeServiceCommands.CHAT_SEND_NEW_MESSAGE)
                if(poruka.length > 1) {
                    Log.d("ingo", "šaljem poruku...")
                    sendMessage2(poruka.drop(1).trim())
                } else {
                    Log.d("ingo", "poruka prekratka da bi se poslala")
                }
            }
        }
    }

    fun MENU_SEARCH_IN_FILE(poruka: String){
        when(poruka[0]){
            SHORTCUT_NEXT -> {
                if(current_file_line_index < file_content.size-1){
                    // traži
                    val foundLine = file_content.takeLast(file_content.size-1-current_file_line_index).find{ it -> it.lowercase().contains(search_query) }
                    if(foundLine != null) {
                        current_file_line_index = file_content.indexOf(foundLine)
                        vibrate(file_content[current_file_line_index])
                        Log.d("ingo", "skačem na query naprijed - " + file_content[current_file_line_index])
                    } else {
                        vibrate(VIBRATION_NOT_OK)
                    }
                } else {
                    vibrate(VIBRATION_NOT_OK)
                }
                commandIssued(MorseCodeServiceCommands.FILES_SEARCH_NEXT)
            }
            SHORTCUT_PREVIOUS -> {
                if(current_file_line_index > 0){
                    // traži
                    val foundLine = file_content.take(current_file_line_index).findLast{ it -> it.lowercase().contains(search_query) }
                    if(foundLine != null) {
                        current_file_line_index = file_content.indexOf(foundLine)
                        vibrate(file_content[current_file_line_index])
                        Log.d("ingo", "skačem na query prije - " + file_content[current_file_line_index])
                    } else {
                        vibrate(VIBRATION_NOT_OK)
                    }
                } else {
                    vibrate(VIBRATION_NOT_OK)
                }
                commandIssued(MorseCodeServiceCommands.FILES_SEARCH_PREVIOUS)
            }
            SHORTCUT_CONFIRM -> {
                current_menu = MorseCodeServiceMenus.MENU_READ_FROM_FILES
                commandIssued(MorseCodeServiceCommands.FILES)
                vibrate(VIBRATION_OK)
            }
        }
    }

    fun INTERNET_MENU(poruka: String){
        when(poruka[0]){
            'e' -> {} // change engine
            'l' -> {
                luck_follows_me = !luck_follows_me
                vibrate(luck_follows_me.toString())
            }
            SHORTCUT_CONFIRM -> {
                commandIssued(MorseCodeServiceCommands.INTERNET_SEARCH)
                if(poruka.length > 1) {
                    Log.d("ingo", "tražim internet... di je?")
                    searchInternet(poruka.drop(1))
                } else {
                    Log.d("ingo", "upit prekratak da bi se izvršio")
                }
            }
            else -> {}
        }
    }

    var client = OkHttpClient()

    fun searchInternet(query: String){
        //https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=Craig%20Noone&format=json
        val request: Request = Request.Builder().url(query).build()

        val call: Call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ingo", "on failure")
                Log.e("ingo", e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ingo", "on Success")
                Log.d("ingo", response.toString())
                System.out.println(response.toString())
            }

        })
    }

    fun regularInputsCheck(){
        if(dont_check_input) return
        val diff:Int = getTimeDifference()
        val porukaIzMorsea:String = getMessage()
        val timeDifferenceNeeded = if(current_menu == MorseCodeServiceMenus.MENU_CONTACT_CHAT && porukaIzMorsea[0] == SHORTCUT_CONFIRM) servicePostavke.oneTimeUnit*7 else servicePostavke.oneTimeUnit*3
        if(isCharacterFinished() && diff > timeDifferenceNeeded) {
            commandListener?.lastCharactersEntered(porukaIzMorsea)
            Log.d("ingo", "buttonHistory not empty, time difference is big enough")
            Log.d("ingo", porukaIzMorsea)
            Log.d("ingo", buttonHistory.toString())
            Log.d("ingo", getMorse().toString())
            if(porukaIzMorsea.isEmpty()) return
            buttonHistory.clear()
            if(isNumeric(porukaIzMorsea[0].toString())){
                Log.d("ingo", "Prvi znak je broj")
                //sendMessage(porukaIzMorsea)
                // specijalne naredbe koje ne rezultiraju slanjem na server
            } else if(porukaIzMorsea[0] == SHORTCUT_BACK) {
                Log.d("ingo", "current menu is $current_menu")
                commandListener?.bottomCommand(MorseCodeServiceCommands.GO_BACK)
                when(current_menu){
                    MorseCodeServiceMenus.MENU_CONTACT_CHAT -> {
                        MAIN_MENU(CONTACTS_KEY.toString())
                    }
                    else -> {
                        current_menu = MorseCodeServiceMenus.MENU_MAIN
                        commandIssued(MorseCodeServiceCommands.MAIN)
                    }
                }
                vibrate(VIBRATION_OK)
                Log.d("ingo", "back successfull, now current menu is $current_menu")
            } else if(porukaIzMorsea[0] == SHORTCUT_REPEAT) {
                commandListener?.bottomCommand(MorseCodeServiceCommands.REPEAT)
                vibrate(last_vibrated)
            } else {
                when(current_menu){
                    MorseCodeServiceMenus.MENU_MAIN -> MAIN_MENU(porukaIzMorsea)
                    MorseCodeServiceMenus.MENU_CHOOSE_CONTACTS -> CONTACTS_MENU(porukaIzMorsea)
                    MorseCodeServiceMenus.MENU_CONTACT_CHAT -> CONTACT_CHAT_MENU(porukaIzMorsea)
                    MorseCodeServiceMenus.MENU_READ_FROM_FILES -> READ_FROM_FILES_MENU(porukaIzMorsea)
                    MorseCodeServiceMenus.MENU_CHOOSE_FILE -> MENU_CHOOSE_FILE(porukaIzMorsea)
                    MorseCodeServiceMenus.MENU_SEARCH_IN_FILE -> MENU_SEARCH_IN_FILE(porukaIzMorsea)
                    else -> {}
                }
                Log.d("ingo", "looked into menu $current_menu...")
                //if(current_menu == MorseCodeServiceMenus.MENU_MAIN) MAIN_MENU(porukaIzMorsea)
                /*if(porukaIzMorsea[0] == 'e') {
                    vibrateWithPWM(makeWaveformFromText(lastMessage))
                } else if(porukaIzMorsea[0] == 'l') {
                    Log.d("ingo", "aaa " + servicePostavke.pwm_on.toString() + ", sss " + servicePostavke.pwm_off.toString())
                }*/
            }
        }
        return
        //Log.d("ingo", "maybeSendMessage")
        /*val diff:Int = getTimeDifference()
        if(buttonHistory.size > 0 && diff > servicePostavke.oneTimeUnit*7){
            Log.d("ingo", "buttonHistory not empty, time difference is big enough")
            val porukaIzMorsea:String = getMessage()
            Log.d("ingo", porukaIzMorsea)
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
        }*/
    }

    private suspend fun setText(){
        withContext(Dispatchers.IO){
            //textView.setText(MORSECODE_ON + ": neispravna naredba")
        }
    }

    fun isNumeric(str: String) = str.all { it in '0'..'9' }

    suspend fun sendTextToServer(stringZaPoslati:String){
        try {
            val response: VibrationMessage = VibrationMessage(0, "haha", "haha")//VibrationMessagesApi.retrofitService.sendMessage(stringZaPoslati)
            databaseAddNewPoruka(response)
            withContext(Dispatchers.Main){
                //textView.setText(MORSECODE_ON + ": received " + response.poruka)
                messageReceiveCallback?.let { it() }
            }
            if(response.vibrate != null && response.vibrate != ""){
                Log.d("ingo", "vibrira")
                last_vibrated = response.vibrate
                vibrateWithPWM(makeWaveformFromText(response.vibrate) )
            }
            if(response.poruka != null) Log.d("ingo", "poruka: " + response.poruka)
            buttonHistory.clear()
        } catch (e: Exception) {
            Log.d("ingo", "greska sendMessage " + e.stackTraceToString() + e.message.toString())
        }
    }

    suspend fun maybeSendMessageCoroutineLoop() { // this: CoroutineScope
        while(true) {
            delay(servicePostavke.oneTimeUnit) // non-blocking delay for one dot duration (default time unit is ms)
            regularInputsCheck()
            if(!mSocket.connected()){
                val current_time = System.currentTimeMillis()
                if(last_checked_for_new_messages+POLLING_WAIT_TIME < current_time){
                    last_checked_for_new_messages = current_time
                    getNewMessages(true)
                }
            }
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
            getContactsApiService(this@MorseCodeService).getContact(username)
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
                sb.append(".")
            } else {
                sb.append("-")
            }
        }
        //Log.d("ingo", "morse->" + sb.toString()) // print after delay
        return sb.toString()
    }



    fun setMessageFeedback(callback: KFunction0<Unit>?){
        messageReceiveCallback = callback
    }

    // funkcija vraća true ako je kod upisa morseovog koda nakon puštanja tipke prošlo vrijeme veće od servicePostavke.oneTimeUnit (znači počinje se gledati novi znak), bitno kod tutoriala da možemo odrediti je li korisnik pogrešno unio slovo ili samo još nije završio upisivanje pravog slova
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
        var morse = getMorse()
        val morse_split = morse.split(" ")
        for(by_space in morse_split){
            if(by_space == "") {
                message.append(" ")
            } else {
                val index:Int = MORSE.indexOfFirst { it == by_space }
                //Log.d("ingo", "$by_space je ")
                if(index != -1) {
                    message.append(ALPHANUM.get(index))
                    //Log.d("ingo", "je ${ALPHANUM.get(index)}")
                } else {
                    message.append("?")
                }
            }
        }
        //Log.d("ingo", "mess->" + message.toString()) // print after delay
        return message.toString()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        PhysicalButtonsService.getSharedInstance()?.addListener(this)
        Log.d("ingo", "service rebinded")
    }

    companion object{
        // Funkcije u companion objektu u Kotlinu su isto kao static funkcije u Javi, znači mogu se pozvati bez instanciranja
        // U ovom slučaju koristimo funkciju getSharedInstance da bismo iz neke druge klase statičkom funkcijom uzeli instancu servisa (MorseCodeService). Ako servis nije pokrenut, dobit ćemo null.
        // serviceSharedInstance se inicijalizira (postavlja vrijednost) kod pokretanja accessibility servisa, a uništava kod zaustavljanja accessibility servisa

        val MORSE = arrayOf(".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----")
        val ALPHANUM:String = "abcdefghijklmnopqrstuvwxyz1234567890"

        var serviceSharedInstance:MorseCodeService? = null
        fun getSharedInstance():MorseCodeService?{
            return serviceSharedInstance;
        }

        fun prettifyMorse(morse: String):String{
            var sb:StringBuilder = StringBuilder()
            morse.forEach{
                if(it == '.'){
                    sb.append('•')
                } else if(it == '-'){
                    sb.append('–')
                } else {
                    sb.append(' ')
                }
            }
            return sb.toString()
        }

        fun stringToMorse(text: String): String{
            var vrati = java.lang.StringBuilder()
            text.forEach{ slovo ->
                val index:Int = ALPHANUM.indexOfFirst { it == slovo }
                if(slovo == ' '){
                    vrati.append(" ")
                } else if(index == -1) {
                    vrati.append("?")
                } else{
                    val morse:String = MorseCodeService.MORSE.get(index)
                    vrati.append(morse)
                }
                vrati.append(" ")
            }
            return vrati.toString()
        }

        var SHORTCUT_NEXT = 'e'
        var SHORTCUT_PREVIOUS = 't'
        var SHORTCUT_CONFIRM = 'i'
        var SHORTCUT_EXTRA = 's'
        var SHORTCUT_REPEAT = 'h'
        var SHORTCUT_BACK = 'm'

        var VIBRATION_OK = "e"
        var VIBRATION_NOT_OK = "i"

        val CONTACTS_KEY = 'c'
        val FILES_KEY = 'r'
        val INTERNET_KEY = 'i'
    }

    fun getPostavke(): Postavke {
        return Postavke(servicePostavke.pwm_on, servicePostavke.pwm_off, servicePostavke.oneTimeUnit, award_interval = servicePostavke.award_interval)
    }

    fun savePostavke(){
        val editor = sharedPreferences.edit()
        editor.putLong(Constants.PWM_ON, servicePostavke.pwm_on)
        editor.putLong(Constants.PWM_OFF, servicePostavke.pwm_off)
        editor.putLong(Constants.ONE_TIME_UNIT, servicePostavke.oneTimeUnit)
        editor.putString(Constants.SOCKETIO_IP, servicePostavke.socketioIp)
        editor.putString(Constants.USER_NAME, servicePostavke.username)
        editor.putInt(Constants.USER_ID, servicePostavke.userId)
        editor.putString(Constants.USER_HASH, servicePostavke.userHash)
        editor.putBoolean(Constants.HANDS_FREE, servicePostavke.handsFreeOnChat)
        editor.putLong(Constants.LAST_LEG_PROFILE, servicePostavke.lastLegProfile)
        editor.putString(Constants.PHYSICAL_BUTTONS, Gson().toJson(servicePostavke.physicalButtons))
        editor.putInt(Constants.AWARD_INTERVAL, servicePostavke.award_interval)
        editor.apply()
    }

    fun loadPostavke(){
        servicePostavke = Postavke()
        servicePostavke.pwm_on = sharedPreferences.getLong(Constants.PWM_ON, 5)
        servicePostavke.pwm_off = sharedPreferences.getLong(Constants.PWM_OFF, 1)
        servicePostavke.oneTimeUnit = sharedPreferences.getLong(Constants.ONE_TIME_UNIT, 400)
        servicePostavke.socketioIp = sharedPreferences.getString(Constants.SOCKETIO_IP, Constants.DEFAULT_SOCKETIO_IP).toString()
        servicePostavke.username = sharedPreferences.getString(Constants.USER_NAME, "").toString()
        servicePostavke.userId = sharedPreferences.getInt(Constants.USER_ID, 0)
        servicePostavke.userHash = sharedPreferences.getString(Constants.USER_HASH, "").toString()
        servicePostavke.handsFreeOnChat = sharedPreferences.getBoolean(Constants.HANDS_FREE, Constants.HANDS_FREE_DEFAULT)
        servicePostavke.lastLegProfile = sharedPreferences.getLong(Constants.LAST_LEG_PROFILE, -1)
        servicePostavke.award_interval = sharedPreferences.getInt(Constants.AWARD_INTERVAL, 20)

        val tmpPhysicalButtons: String = sharedPreferences.getString(Constants.PHYSICAL_BUTTONS, "").toString()
        if(tmpPhysicalButtons != "") {
            Log.d("ingo", "tryingg $tmpPhysicalButtons")
            Log.d("ingo", (Gson().fromJson(tmpPhysicalButtons, MutableList::class.java)).toString())
            val buttonss = (Gson().fromJson<MutableList<Int>>(tmpPhysicalButtons, MutableList::class.java)).map{ t -> t.toInt()}
            Log.d("ingo", buttonss.toString())
            servicePostavke.physicalButtons = copyOf(buttonss).toMutableList()
        } else {
            servicePostavke.physicalButtons = mutableListOf()
        }
    }

    fun toggleTesting(testing: Boolean){
        testMode = testing
        Log.d("ingo", "testing: $testMode")
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
        if(::vibrating_queue_korutina.isInitialized) vibrating_queue_korutina.cancel()
        Log.d("ingo", "onUnbind")
        serviceSharedInstance = null;
        NotificationManagerCompat.from(this).cancelAll()
        PhysicalButtonsService.getSharedInstance()?.removeListener(this)
        super.onUnbind(intent)
        return true
    }

    private val onError =
        Emitter.Listener { args ->
            for (arg in args) {
                Log.d("ingo", arg.toString())
            }
        }

    private val onNewMessage =
        Emitter.Listener { args ->
            // bi li ovo trebalo biti u UI dretvi?
            val messageInJson: String = (args[0] as JSONObject).getString("message")
            val message: Message = Gson().fromJson(messageInJson, Message::class.java)
            for(listener in socketListeners) {
                listener.onNewMessage(message)
            }
            if(vibrate_new_messages) {
                var message_text = StringBuilder()
                if(last_contact_index_played != message.senderId){
                    message_text.append(contacts.find { contact -> contact.id == message.senderId?.toLong() })
                }
                message_text.append(message.message)
                // ako uređaj nije zauzet, odmah zavibriraj, inače zavibriraj kad bude slobodan
                vibrate(message_text.toString())
            }
            Log.d("ingo", "primljena poruka")
        }

    fun emitMessage(message: Message){
        val newMessage = JSONObject()
        newMessage.put("message", Gson().toJson(message).toString())
        mSocket.emit("new message", newMessage);
    }

    interface OnSocketNewMessageListener {
        fun onNewMessage(message: Message)
    }
    private var socketListeners: MutableList<OnSocketNewMessageListener> = mutableListOf()
    fun addListener(l: OnSocketNewMessageListener) {
        socketListeners.add(l)
    }
    fun removeListener(l: OnSocketNewMessageListener) {
        socketListeners.remove(l)
    }

    interface OnMorseProgressChangeListener {
        fun onMorseProgressChangeListener(progress: Int, up: Boolean)
    }
    private var morseListeners: MutableList<OnMorseProgressChangeListener> = mutableListOf()
    fun addListener(l: OnMorseProgressChangeListener) {
        morseListeners.add(l)
    }
    fun removeListener(l: OnMorseProgressChangeListener) {
        morseListeners.remove(l)
    }

    private var commandListener: CommandListener? = null
    fun setListener(l: CommandListener) {
        commandListener = l
    }
    fun removeListener() {
        commandListener = null
    }

    override fun onKey(pressed: Boolean) {
        onKeyPressed()
        Log.d("ingo", "key pressed")
    }

    override fun keyAddedOrRemoved() {

    }

}