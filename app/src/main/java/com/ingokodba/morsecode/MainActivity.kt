package com.ingokodba.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.ingokodba.morsecode.baza.AppDatabase
import com.ingokodba.morsecode.baza.MessageDao
import com.ingokodba.morsecode.models.Message
import com.ingokodba.morsecode.network.getMessagesApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    var mAccessibilityService:MorseCodeService? = null

    private lateinit var sharedPreferences: SharedPreferences
    private val sharedPreferencesFile = "MyPrefs"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(intent.extras != null && intent.extras!!.getBoolean("logged_in")){
            getMessages()
        }

        val intent = Intent(this, MorseCodeService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }

        mAccessibilityService = MorseCodeService.getSharedInstance();
        Log.d("ingo", mAccessibilityService.toString())

        mAccessibilityService = MorseCodeService.getSharedInstance();

        findViewById<CardView>(R.id.contacts).setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
        }


        findViewById<Button>(R.id.visualise_accesibility).setOnClickListener {
            val intent = Intent(this, MorseServiceVisualised::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.tutorial).setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.playground).setOnClickListener {
            val intent = Intent(this, PlaygroundActivity::class.java)
            startActivity(intent)
        }
        findViewById<CardView>(R.id.files).setOnClickListener {
            val intent = Intent(this, ReadFilesActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        val sharedName: String = sharedPreferences.getString("username", "").toString()
        val userHash = sharedPreferences.getString(Constants.USER_HASH, "")
        val prefUserId = sharedPreferences.getInt(Constants.USER_ID, 0)
        val autoLogIn = sharedPreferences.getBoolean(Constants.AUTO_LOGIN, false)
        findViewById<TextView>(R.id.welcome_message).text = StringBuilder("${getString(R.string.welcome)}, ${sharedName}!").toString()
        /*findViewById<LinearLayout>(R.id.morse_in_action).setOnClickListener(){
            val intent = Intent(this, SendMorseActivity::class.java)
            startActivity(intent)
        }*/

        if (!autoLogIn)
            getFriends(prefUserId, userHash)
    }

    private fun getFriends(prefUserId: Int, userHash: String?) {
        /*lifecycleScope.launch(Dispatchers.Default) {
            try {
                val kontakti: List<Contact> =
                    getContactsApiService(this@MainActivity).getMyFriends()

                for (friends in kontakti) {
                    getMessages(prefUserId, userHash, friends.id!!.toInt() )
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska getFriends $e")
            }
        }*/
    }

    private fun getMessages() {
        val db = AppDatabase.getInstance(this)
        val messageDao: MessageDao = db.messageDao()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val messages: List<Message> =
                    getMessagesApiService(this@MainActivity).getMessages()

                messageDao.insertAll(*messages.toTypedArray())
                for (poruka in messages) {
                    Log.d("ingo", poruka.toString())
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska getMessages" + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    private fun saveMessage(message: Message) {
        try {
            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(message)

            Log.e("stjepan", "db uspelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db neje uspelo" + e.stackTraceToString() + e.message.toString())
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
            /*R.id.accelerometer_controls -> {
                sharedPreferences =
                    this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
                val hands_free = sharedPreferences.getBoolean("hands_free", false)
                val editor = sharedPreferences.edit()
                editor.putBoolean("hands_free", !hands_free)
                editor.apply()
                true
            }*/
            R.id.log_out -> {
                mAccessibilityService?.stopSelf()

                val db = AppDatabase.getInstance(this)
                val messageDao: MessageDao = db.messageDao()
                messageDao.deleteAll()

                val intent = Intent(this, LogInActivity::class.java)
                intent.putExtra("logout", true)
                startActivity(intent)
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}