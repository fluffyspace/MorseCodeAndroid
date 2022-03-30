package com.example.morsecode

import android.content.Intent
import android.content.pm.ShortcutInfo
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
import com.example.morsecode.moodel.Poruka
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var lista_poruka:TextView
    var mAccessibilityService:GlobalActionBarService? = null
    lateinit var service_not_started:TextView

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        service_not_started = findViewById(R.id.service_not_started)
        service_not_started.setOnClickListener {
            checkService();
        }

        Log.d("ingo", mAccessibilityService.toString())
        fetchPostavkeFromService()
        checkService();

        findViewById<Button>(R.id.reload_from_service).setOnClickListener(){
            mAccessibilityService = GlobalActionBarService.getSharedInstance();
            fetchPostavkeFromService()
            Toast.makeText(this, "Messages reloaded.", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.playground).setOnClickListener(){
            val intent = Intent(this, Playground::class.java)
            startActivity(intent)
        }
        lista_poruka = findViewById<TextView>(R.id.lista_poruka)
        reloadListaPoruka()
    }

    fun reloadListaPoruka(){
        lifecycleScope.launch(Dispatchers.IO) {
            val poruke = databaseGetAll()
            withContext(Dispatchers.Main){
                refreshMessages(poruke)
            }
        }
    }

    fun databaseGetAll(): List<Poruka> {
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

    fun refreshMessages(poruke: List<Poruka>){
        //lista_poruka.setText("Povijest poruka: " + poruke.map { poruka -> poruka.poruka }.toString())
        val shortcutList: RecyclerView = findViewById(R.id.messagesList)
        shortcutList.adapter = MessagesAdapter(this, poruke)
        shortcutList.layoutManager = LinearLayoutManager(this)
    }

    fun fetchPostavkeFromService(){
        reloadListaPoruka()
    }

    fun checkService(){
        mAccessibilityService = GlobalActionBarService.getSharedInstance();
        if(mAccessibilityService == null) {
            service_not_started.visibility = View.VISIBLE
        } else {
            service_not_started.visibility = View.GONE
        }
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}