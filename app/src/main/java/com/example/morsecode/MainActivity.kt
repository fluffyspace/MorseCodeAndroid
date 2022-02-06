package com.example.morsecode

import android.content.Intent
import android.opengl.Visibility
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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

class MainActivity : AppCompatActivity() {

    var aaa:Long = 0
    var sss:Long = 0
    var oneTimeUnit:Long = 0
    lateinit var aaa_status:TextView
    lateinit var sss_status:TextView
    lateinit var otu_status:TextView
    lateinit var timing_status:TextView
    lateinit var lista_poruka:TextView
    var mAccessibilityService:GlobalActionBarService? = null
    lateinit var vibration_settings:LinearLayout
    lateinit var service_not_started:TextView

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAccessibilityService = GlobalActionBarService.getSharedInstance();
        Log.d("ingo", mAccessibilityService.toString())
        fetchPostavkeFromService()
        aaa_status = findViewById<TextView>(R.id.aaa_status)
        sss_status = findViewById<TextView>(R.id.sss_status)
        otu_status = findViewById<TextView>(R.id.otu_status)
        timing_status = findViewById(R.id.morse_timing)
        vibration_settings = findViewById(R.id.vibration_settings)
        service_not_started = findViewById(R.id.service_not_started)

        refreshStatus()

        val text = findViewById<EditText>(R.id.vibrate_letters)
        findViewById<Button>(R.id.save_settings).setOnClickListener{
            mAccessibilityService?.setPostavke(Postavke(aaa, sss, oneTimeUnit))
        }
        val button = findViewById<Button>(R.id.vibrate_button)
        button.setOnClickListener{
            mAccessibilityService?.vibrateee(mAccessibilityService!!.makeWaveformFromText(text.text.toString()))
        }
        findViewById<Button>(R.id.reload_from_service).setOnClickListener(){
            mAccessibilityService = GlobalActionBarService.getSharedInstance();
            fetchPostavkeFromService()
            refreshStatus()
        }
        val timing_configuration = findViewById<LinearLayout>(R.id.timing_configuration)
        findViewById<Button>(R.id.toggle_timing_configuration).setOnClickListener{
            timing_configuration.visibility = if(timing_configuration.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        findViewById<Button>(R.id.vibrate_stop_button).setOnClickListener(){
            mAccessibilityService?.vibrator?.cancel()
        }
        findViewById<Button>(R.id.playground).setOnClickListener(){
            val intent = Intent(this, Playground::class.java).apply {
                putExtra("oneTimeUnit", oneTimeUnit.toString())
            }
            startActivity(intent)
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
        lista_poruka = findViewById<TextView>(R.id.lista_poruka)
        reloadListaPoruka()
    }

    fun reloadListaPoruka(){
        lifecycleScope.launch(Dispatchers.IO) {
            val poruke = databaseGetAll()
            withContext(Dispatchers.Main){
                lista_poruka.setText("Povijest poruka: " + poruke.map { poruka -> poruka.poruka }.toString())
            }
        }
    }

    fun databaseGetAll(): List<Poruka> {
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        Log.d("ingo", "databaseGetAll")
        return porukaDao.getAll()
    }

    fun refreshStatus(){
        if(mAccessibilityService == null) {
            vibration_settings.visibility = View.GONE
            service_not_started.visibility = View.VISIBLE
        } else {
            vibration_settings.visibility = View.VISIBLE
            service_not_started.visibility = View.GONE
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

    fun fetchPostavkeFromService(){
        val (aaaa, sssa, oneTimeUnita) = mAccessibilityService?.getPostavke() ?: Postavke(-1, -1, -1)
        aaa = aaaa
        sss = sssa
        oneTimeUnit = oneTimeUnita
        reloadListaPoruka()
    }
}