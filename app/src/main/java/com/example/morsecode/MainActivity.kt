package com.example.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.PorukaDao
import com.example.morsecode.models.VibrationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    var mAccessibilityService:MorseCodeService? = null

    private lateinit var sharedPreferences: SharedPreferences
    private val sharedPreferencesFile = "MyPrefs"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("ingo", mAccessibilityService.toString())
        fetchPostavkeFromService()

        mAccessibilityService = MorseCodeService.getSharedInstance();

        findViewById<LinearLayout>(R.id.contacts).setOnClickListener(){
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.reload_from_service).setOnClickListener(){
            mAccessibilityService = MorseCodeService.getSharedInstance()
            fetchPostavkeFromService()
            Toast.makeText(this, "Messages reloaded.", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.tutorial).setOnClickListener(){
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.playground).setOnClickListener(){
            val intent = Intent(this, PlaygroundActivity::class.java)
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.morse_in_action).setOnClickListener(){
            val intent = Intent(this, SendMorseActivity::class.java)
            startActivity(intent)
        }
        reloadListaPoruka()

        val intent = Intent(this, MorseCodeService::class.java) // Build the intent for the service
        applicationContext.startForegroundService(intent)
    }

    fun reloadListaPoruka(){
        lifecycleScope.launch(Dispatchers.IO) {
            val poruke = databaseGetAll()
            withContext(Dispatchers.Main){
                refreshMessages(poruke)
            }
        }
    }

    fun databaseGetAll(): List<VibrationMessage> {
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        Log.d("ingo", "databaseGetAll")
        return porukaDao.getAll()
    }

    fun databaseClearMessages() {
        val db = AppDatabase.getInstance(this)
        val porukaDao: PorukaDao = db.porukaDao()
        Log.d("ingo", "databaseClearMessages")
        porukaDao.deleteAll()
    }

    fun clearMessages() {
        lifecycleScope.launch(Dispatchers.IO) {
            databaseClearMessages()
            withContext(Dispatchers.Main){
                refreshMessages(listOf())
            }
        }
    }

    fun refreshMessages(poruke: List<VibrationMessage>){
        val shortcutList: RecyclerView = findViewById(R.id.messagesList)
        shortcutList.adapter = MessagesRecyclerViewAdapter(this, poruke)
        shortcutList.layoutManager = LinearLayoutManager(this)
    }

    fun fetchPostavkeFromService(){
        reloadListaPoruka()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.settings_button -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.open_accessibility_settings -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.clear_messages -> {
                clearMessages()
                true
            }
            R.id.accelerometer_controls -> {
                sharedPreferences = this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
                val hands_free = sharedPreferences.getBoolean("hands_free", false)
                val editor = sharedPreferences.edit()
                editor.putBoolean("hands_free", !hands_free)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}