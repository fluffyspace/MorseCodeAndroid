package com.example.morsecode

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.provider.Settings
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
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.PorukaDao
import com.example.morsecode.models.VibrationMessage
import com.example.morsecode.models.Postavke
import kotlinx.coroutines.*
import java.util.List.copyOf
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction0

private val VIBRATE_PATTERN: List<Long> = listOf(500, 500)
private val MORSE = arrayOf(".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----")
private val ALPHANUM:String = "abcdefghijklmnopqrstuvwxyz1234567890"
// ABCDEFGHIJKLMNOPQRSTUVWXYZ

class PhysicalButtonsService: AccessibilityService(){

    interface OnKeyListener {
        fun onKey()
        fun keyAddedOrRemoved()
    }
    lateinit var korutina: Job
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    var last_command: MorseCodeServiceCommands = MorseCodeServiceCommands.MAIN
    var new_command: MorseCodeServiceCommands = MorseCodeServiceCommands.MAIN

    val textMap = HashMap<MorseCodeServiceCommands, String>()

    var physicalButtons: MutableList<Int> = mutableListOf()
    var detectingNewPhysicalKey: Boolean = false
    var physicalKeyAdding: Boolean = false
    var morseCodeService: MorseCodeService? = null
    var textView: TextView? = null
    var sufix: String = ""

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onServiceConnected() {

        textMap.apply {
            put(MorseCodeServiceCommands.MAIN, getString(R.string.main_menu))
            put(MorseCodeServiceCommands.CONTACTS, getString(R.string.contacts))
            put(MorseCodeServiceCommands.CONTACTS_NEXT, getString(R.string.next_contact))
            put(MorseCodeServiceCommands.CONTACTS_PREVIOUS, getString(R.string.previous_contact))
            put(MorseCodeServiceCommands.CONTACTS_CHOOSE, getString(R.string.open_contact))
            put(MorseCodeServiceCommands.CHAT_LAST_MESSAGE, getString(R.string.listen_to_last_received_message))
            put(MorseCodeServiceCommands.CHAT_PREVIOUS, getString(R.string.listen_to_message_before))
            put(MorseCodeServiceCommands.CHAT_NEXT, getString(R.string.listen_to_message_after))
            put(MorseCodeServiceCommands.CHAT_SEND_NEW_MESSAGE, getString(R.string.send_a_new_message))
            put(MorseCodeServiceCommands.FILES, getString(R.string.choose_file))
            put(MorseCodeServiceCommands.FILES_CHOOSE_FILE, getString(R.string.next_file))
            put(MorseCodeServiceCommands.FILES_CHOOSE_NEXT, getString(R.string.next_file))
            put(MorseCodeServiceCommands.FILES_CHOOSE_PREVIOUS, getString(R.string.previous_file))
            put(MorseCodeServiceCommands.FILES_OPEN, getString(R.string.open_file))
            put(MorseCodeServiceCommands.FILES_SEARCH, getString(R.string.search_in_file))
            put(MorseCodeServiceCommands.FILES_SEARCH_NEXT, getString(R.string.next_occurence))
            put(MorseCodeServiceCommands.FILES_SEARCH_PREVIOUS, getString(R.string.previous_occurence))
            put(MorseCodeServiceCommands.FILES_NEW, getString(R.string.new_file))
            put(MorseCodeServiceCommands.FILES_SAVE, getString(R.string.save_file))
            put(MorseCodeServiceCommands.FILES_WRITE, getString(R.string.write_to_file))
            put(MorseCodeServiceCommands.INTERNET, getString(R.string.search_internet))
            put(MorseCodeServiceCommands.INTERNET_SWITCH_ENGINE, getString(R.string.switch_search_engine))
            put(MorseCodeServiceCommands.INTERNET_FEELING_LUCKY, getString(R.string.feeling_lucky_on_off))
            put(MorseCodeServiceCommands.INTERNET_SEARCH, getString(R.string.search))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_NEXT, getString(R.string.next_result))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_PREVIOUS, getString(R.string.previous_result))
            put(MorseCodeServiceCommands.INTERNET_SEARCH_SKIP_SENTENCE, getString(R.string.skip_sentence))
            put(MorseCodeServiceCommands.GO_BACK, getString(R.string.go_back))
            put(MorseCodeServiceCommands.REPEAT, getString(R.string.repeat))
        }

        morseCodeService = MorseCodeService.getSharedInstance()

        if(morseCodeService?.showOverlay == true){
            createOverlay()
        }

        morseCodeService = MorseCodeService.getSharedInstance()
        morseCodeService?.accesibilityServiceOn()
        if(morseCodeService != null){
            physicalButtons = copyOf(morseCodeService!!.servicePostavke.physicalButtons).toMutableList()
        }
        serviceSharedInstance = this

        korutina = scope.launch(Dispatchers.Default) {
            while(true) {
                delay(1000) // non-blocking delay for one dot duration (default time unit is ms)
                withContext(Dispatchers.Main){
                    if(last_command != new_command) {
                        textView?.text = textMap[new_command] + sufix
                        last_command = new_command
                    }
                }
            }
        }
    }

    fun createOverlay(){
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val mLayout = FrameLayout(this)
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
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onKeyEvent(event: KeyEvent): Boolean {
        /*
            KEYCODE_HEADSETHOOK = middle button on phone headset 79
            KEYCODE_VOLUME_UP = volume up button on phone
            KEYCODE_VOLUME_DOWN = volume down button on phone 25
         */
        //MorseCodeService.getSharedInstance()?.onKeyEvent(event)
        if(detectingNewPhysicalKey){
            if(event.action == KeyEvent.ACTION_UP){
                detectingNewPhysicalKey = false
                return true
            }
            var madeChanges = false
            Log.d("ingo", "detectingNewPhysicalKey ${event.keyCode}")
            if(physicalKeyAdding) {
                Log.d("ingo", "physicalKeyAdding")
                if(!physicalButtons.contains(event.keyCode)) {
                    Log.d("ingo", "adding")
                    physicalButtons.add(event.keyCode)
                    morseCodeService?.servicePostavke?.physicalButtons?.add(event.keyCode)
                }
            } else {
                Log.d("ingo", "physicalKeyRemoving")
                Log.d("ingo", physicalButtons.toString())
                Log.d("ingo", event.keyCode.toString())
                if(physicalButtons.contains(event.keyCode)) {
                    Log.d("ingo", "removing")
                    physicalButtons.remove(event.keyCode)
                    morseCodeService?.servicePostavke?.physicalButtons?.remove(event.keyCode)
                }
            }
            morseCodeService?.savePostavke()
            if(morseCodeService != null) physicalButtons = copyOf(morseCodeService!!.servicePostavke.physicalButtons).toMutableList()
            for(listener in listeners) {
                listener.keyAddedOrRemoved()
            }
            return true
        } else if(physicalButtons.contains(event.keyCode)) {
            for(listener in listeners) {
                listener.onKey()
            }
            return true
        }
        //This allows the key pressed to function normally after it has been used by your app.
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        Log.d("ingo", "oncancel")
        korutina.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("ingo", "service rebinded")
    }

    companion object{
        // Funkcije u companion objektu u Kotlinu su isto kao static funkcije u Javi, znači mogu se pozvati bez instanciranja
        // U ovom slučaju koristimo funkciju getSharedInstance da bismo iz neke druge klase statičkom funkcijom uzeli instancu servisa (MorseCodeService). Ako servis nije pokrenut, dobit ćemo null.
        // serviceSharedInstance se inicijalizira (postavlja vrijednost) kod pokretanja accessibility servisa, a uništava kod zaustavljanja

        var serviceSharedInstance:PhysicalButtonsService? = null
        fun getSharedInstance():PhysicalButtonsService?{
            return serviceSharedInstance;
        }

        private var listeners: MutableList<OnKeyListener> = mutableListOf()
        fun addListener(l: OnKeyListener) {
            if(!listeners.contains(l)) listeners.add(l)
        }
        fun removeListener(l: OnKeyListener) {
            listeners.remove(l)
        }
    }

    fun changeActionBarText(command: MorseCodeServiceCommands, sufix: String = ""){
        new_command = command
        this@PhysicalButtonsService.sufix = sufix
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("ingo", "onUnbind")
        serviceSharedInstance = null;
        MorseCodeService.getSharedInstance()?.accesibilityServiceOff()
        super.onUnbind(intent)
        return true
    }

}