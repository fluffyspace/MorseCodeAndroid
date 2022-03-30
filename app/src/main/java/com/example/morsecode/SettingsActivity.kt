package com.example.morsecode

import android.content.Intent
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.PorukaDao
import com.example.morsecode.moodel.Poruka
import com.example.morsecode.moodel.Postavke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text

class SettingsActivity : AppCompatActivity() {

    var aaa:Long = 0
    var sss:Long = 0
    var oneTimeUnit:Long = 0
    lateinit var aaa_status:TextView
    lateinit var sss_status:TextView
    lateinit var otu_status:TextView
    lateinit var timing_status:TextView
    var mAccessibilityService:GlobalActionBarService? = null
    lateinit var service_not_started:TextView

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        aaa = PreferenceManager.getDefaultSharedPreferences(this).getLong("aaa", 5)
        sss = PreferenceManager.getDefaultSharedPreferences(this).getLong("sss", 1)
        oneTimeUnit = PreferenceManager.getDefaultSharedPreferences(this).getLong("oneTimeUnit", 400)
        val token_value = PreferenceManager.getDefaultSharedPreferences(this).getString("token", "").toString()

        mAccessibilityService = GlobalActionBarService.getSharedInstance();
        Log.d("ingo", mAccessibilityService.toString())
        aaa_status = findViewById<TextView>(R.id.aaa_status)
        sss_status = findViewById<TextView>(R.id.sss_status)
        otu_status = findViewById<TextView>(R.id.otu_status)
        timing_status = findViewById(R.id.morse_timing)
        service_not_started = findViewById(R.id.service_not_started)

        refreshStatus()

        val token = findViewById<EditText>(R.id.token)
        token.setText(token_value)
        val text = findViewById<EditText>(R.id.vibrate_letters)
        findViewById<Button>(R.id.save_settings).setOnClickListener{
            setPostavke(Postavke(aaa, sss, oneTimeUnit, token.text.toString()))
            Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show()
            //finish()
        }
        findViewById<Button>(R.id.reset_settings).setOnClickListener{
            aaa = 10
            sss = 2
            oneTimeUnit = 400
            setPostavke(Postavke(aaa, sss, oneTimeUnit, token.text.toString()))
            Toast.makeText(this, "Settings reset.", Toast.LENGTH_SHORT).show()
            refreshStatus()
            //finish()
        }
        val button = findViewById<Button>(R.id.vibrate_button)
        button.setOnClickListener{
            mAccessibilityService?.vibrateee(mAccessibilityService!!.makeWaveformFromText(text.text.toString()))
        }
        findViewById<Button>(R.id.vibrate_stop_button).setOnClickListener(){
            mAccessibilityService?.vibrator?.cancel()
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
    }

    fun setPostavke(postavke:Postavke){
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putLong("aaa", postavke.aaa)
        editor.putLong("sss", postavke.sss)
        editor.putLong("oneTimeUnit", postavke.oneTimeUnit)
        editor.putString("token", postavke.token)
        editor.apply()
    }

    fun databaseGetAll(): List<Poruka> {
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        Log.d("ingo", "databaseGetAll")
        return porukaDao.getAll()
    }

    fun refreshStatus(){
        if(mAccessibilityService == null) {
            service_not_started.visibility = View.VISIBLE
        } else {
            aaa_status.text = getString(R.string.aaa) + aaa.toString()
            sss_status.text = getString(R.string.sss) + sss.toString()
            otu_status.text = getString(R.string.otu) + oneTimeUnit.toString()
            timing_status.text = "Dit: up to 1 unit (<" + (oneTimeUnit).toString() + " ms)\n" +
                    "Dah: from 1 unit up (>" + (oneTimeUnit).toString() + " ms)\n" +
                    "Intra-character space (the gap between dits and dahs within a character): up to 1 unit (" + (oneTimeUnit).toString() + " ms)\n" +
                    "Inter-character space (the gap between the characters of a word): from 1 unit up to 3 units (" + (oneTimeUnit).toString() + " - " + (oneTimeUnit * 3).toString() + " ms)\n" +
                    "Word space (the gap between two words): from 3 units up (>" + (oneTimeUnit * 3).toString() + " ms)"
        }
    }
}