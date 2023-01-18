package com.ingokodba.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.lifecycleScope
import com.ingokodba.morsecode.baza.AppDatabase
import com.ingokodba.morsecode.baza.PorukaDao
import com.ingokodba.morsecode.models.VibrationMessage
import com.ingokodba.morsecode.models.Postavke
import com.ingokodba.morsecode.sockets.TestActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.List.copyOf
import kotlin.random.Random

class SettingsActivity : AppCompatActivity(), PhysicalButtonsService.OnKeyListener {

    var aaa:Long = 0
    var sss:Long = 0
    var award_interval:Int = 20
    var oneTimeUnit:Long = 0
    var device_uuid:String = ""
    lateinit var awardStatus:TextView
    lateinit var pwmOnStatus:TextView
    lateinit var socketioip:EditText
    lateinit var pwmOffStatus:TextView
    lateinit var oneTimeUnitStatus:TextView
    lateinit var timing_status:TextView
    lateinit var physicalKeys:TextView
    var mAccessibilityService:MorseCodeService? = null
    var physicalButtonsService:PhysicalButtonsService? = null
    lateinit var sharedPreferences: SharedPreferences


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = this.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)

        aaa = sharedPreferences.getLong("pwm_on", 5)
        sss = sharedPreferences.getLong("pwm_off", 1)
        oneTimeUnit = sharedPreferences.getLong("oneTimeUnit", 400)
        award_interval = sharedPreferences.getInt(Constants.AWARD_INTERVAL, 20)
        val socketioipPref = sharedPreferences.getString(Constants.SOCKETIO_IP, Constants.DEFAULT_SOCKETIO_IP).toString()
        device_uuid = sharedPreferences.getString("device_uuid", "").toString()
        if(device_uuid == "") setDeviceUuid()

        mAccessibilityService = MorseCodeService.getSharedInstance();
        physicalButtonsService = PhysicalButtonsService.getSharedInstance();
        Log.d("ingo", mAccessibilityService.toString())
        pwmOnStatus = findViewById<TextView>(R.id.pwm_on_status)
        pwmOffStatus = findViewById<TextView>(R.id.pwm_off_status)
        oneTimeUnitStatus = findViewById<TextView>(R.id.otu_status)
        awardStatus = findViewById<TextView>(R.id.award_status)
        timing_status = findViewById(R.id.morse_timing)
        physicalKeys = findViewById<TextView>(R.id.registered_physical_keys)
        findViewById<Button>(R.id.add_physical_key).setOnClickListener {
            physicalButtonsService = PhysicalButtonsService.getSharedInstance()
            PhysicalButtonsService.addListener(this)
            physicalButtonsService?.detectingNewPhysicalKey = true
            physicalButtonsService?.physicalKeyAdding = true
            Log.d("ingo", "adding new key")
            Log.d("ingo", physicalButtonsService?.physicalButtons.toString())
        }
        findViewById<Button>(R.id.remove_physical_key).setOnClickListener {
            physicalButtonsService = PhysicalButtonsService.getSharedInstance()
            PhysicalButtonsService.addListener(this)
            physicalButtonsService?.detectingNewPhysicalKey = true
            physicalButtonsService?.physicalKeyAdding = false
        }
        keyAddedOrRemoved()


        findViewById<Button>(R.id.foot_calibration)?.setOnClickListener() {
            val intent = Intent(this, TestActivity::class.java)
            startActivity(intent)
        }

        socketioip = findViewById<EditText>(R.id.socketioip)
        socketioip.setText(socketioipPref)
        val text = findViewById<EditText>(R.id.vibrate_letters)
        findViewById<Button>(R.id.save_settings).setOnClickListener{


            Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show()
            //finish()
        }
        findViewById<Button>(R.id.reset_settings).setOnClickListener{
            aaa = 10
            sss = 2
            oneTimeUnit = 400
            setPostavke(Postavke(aaa, sss, oneTimeUnit, socketioip.text.toString()))
            Toast.makeText(this, "Settings reset.", Toast.LENGTH_SHORT).show()
            refreshStatus()
            //finish()
        }
        val button = findViewById<Button>(R.id.vibrate_button)
        button.setOnClickListener{
            mAccessibilityService?.vibrateWithPWM(mAccessibilityService!!.makeWaveformFromText(text.text.toString()))
        }
        findViewById<Button>(R.id.vibrate_stop_button).setOnClickListener(){
            mAccessibilityService?.vibrator?.cancel()
        }
        findViewById<AppCompatImageButton>(R.id.award_up).setOnClickListener{
            award_interval++
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.award_down).setOnClickListener{
            award_interval--
            if(award_interval < 1) award_interval = 1
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.aaa_up).setOnClickListener{
            aaa++
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.aaa_down).setOnClickListener{
            aaa--
            if(aaa < 0) aaa = 0
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.sss_up).setOnClickListener{
            sss++
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.sss_down).setOnClickListener{
            sss--
            if(sss < 0) sss = 0
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.otu_up).setOnClickListener{
            oneTimeUnit += 50
            refreshStatus()
        }
        findViewById<AppCompatImageButton>(R.id.otu_down).setOnClickListener{
            oneTimeUnit -= 50
            if(oneTimeUnit < 0) oneTimeUnit = 0
            refreshStatus()
        }

        refreshStatus()
    }

    fun generateToken(token_view:EditText){
        var token = StringBuilder()
        for (i in 1..10) {
            val random_char = Random.nextInt(97, 123)
            token.append(random_char.toChar())
        }
        token_view.setText(token)
    }

    fun setDeviceUuid(){
        val editor = sharedPreferences.edit()
        editor.putString("device_uuid", UUID.randomUUID().toString())
        editor.apply()
    }

    override fun onKey() {
    }

    override fun keyAddedOrRemoved() {
        Log.d("ingo", "keyAddedOrRemoved")
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                var tipkice = mAccessibilityService?.servicePostavke?.physicalButtons!!.map{ keycodeInt -> KeyEvent.keyCodeToString(keycodeInt)}
                physicalKeys.setText("Registered physical keys for typing morse code:" + tipkice)

            }
        }
    }

    override fun onPause() {
        super.onPause()
        setPostavke(Postavke(aaa, sss, oneTimeUnit, socketioip.text.toString(), award_interval=award_interval))
    }

    override fun onDestroy() {
        super.onDestroy()
        PhysicalButtonsService.removeListener(this)
    }

    fun setPostavke(postavke:Postavke){
        mAccessibilityService?.servicePostavke?.pwm_on = postavke.pwm_on
        mAccessibilityService?.servicePostavke?.pwm_off = postavke.pwm_off
        mAccessibilityService?.servicePostavke?.oneTimeUnit = postavke.oneTimeUnit
        mAccessibilityService?.servicePostavke?.socketioIp = postavke.socketioIp
        mAccessibilityService?.servicePostavke?.award_interval = postavke.award_interval
        /*val editor = sharedPreferences.edit()
        editor.putLong(Constants.PWM_ON, postavke.pwm_on)
        editor.putLong(Constants.PWM_OFF, postavke.pwm_off)
        editor.putLong(Constants.ONE_TIME_UNIT, postavke.oneTimeUnit)
        editor.putString(Constants.SOCKETIO_IP, postavke.socketioIp)
        editor.apply()*/
        mAccessibilityService?.savePostavke()
    }

    fun databaseGetAll(): List<VibrationMessage> {
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        Log.d("ingo", "databaseGetAll")
        return porukaDao.getAll()
    }

    fun refreshStatus(){
        setPostavke(Postavke(aaa, sss, oneTimeUnit, socketioip.text.toString(), award_interval=award_interval))
        pwmOnStatus.text = getString(R.string.pwm_on) + aaa.toString()
        pwmOffStatus.text = getString(R.string.pwm_off) + sss.toString()
        oneTimeUnitStatus.text = getString(R.string.otu) + oneTimeUnit.toString()
        awardStatus.text = getString(R.string.award_status) + award_interval.toString()
        /*timing_status.text = "Dot: up to 1 unit (<" + (oneTimeUnit).toString() + " ms)\n" +
                "Dash: from 1 unit up (>" + (oneTimeUnit).toString() + " ms)\n" +
                "Intra-character space (the gap between dots and dashs within a character): up to 1 unit (" + (oneTimeUnit).toString() + " ms)\n" +
                "Inter-character space (the gap between the characters of a word): from 1 unit up to 3 units (" + (oneTimeUnit).toString() + " - " + (oneTimeUnit * 3).toString() + " ms)\n" +
                "Word space (the gap between two words): from 3 units up (>" + (oneTimeUnit * 3).toString() + " ms)"*/
        timing_status.text = "Točka: < " + (oneTimeUnit).toString() + " ms\n" +
                "Crta: > " + (oneTimeUnit).toString() + " ms\n" +
                "Razmak između znakova u slovu: < " + (oneTimeUnit).toString() + " ms\n" +
                "Razmak između slova u riječi: " + (oneTimeUnit).toString() + " - " + (oneTimeUnit * 3).toString() + " ms\n" +
                "Razmak između riječi: >" + (oneTimeUnit * 3).toString() + " ms"
    }
}