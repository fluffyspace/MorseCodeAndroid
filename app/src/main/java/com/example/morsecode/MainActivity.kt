package com.example.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.Message
import com.example.morsecode.network.ContactsApi
import com.example.morsecode.network.MessagesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    var mAccessibilityService: MorseCodeService? = null

    private lateinit var sharedPreferences: SharedPreferences
    private val sharedPreferencesFile = "MyPrefs"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("ingo", mAccessibilityService.toString())

        mAccessibilityService = MorseCodeService.getSharedInstance();

        findViewById<LinearLayout>(R.id.contacts).setOnClickListener() {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }


        findViewById<LinearLayout>(R.id.tutorial).setOnClickListener() {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
        findViewById<LinearLayout>(R.id.playground).setOnClickListener() {
            val intent = Intent(this, PlaygroundActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        val sharedName: String = sharedPreferences.getString("username", "").toString()
        val userHash = sharedPreferences.getString(ChatActivity.USER_HASH, "")
        val prefUserId = sharedPreferences.getInt("id", 0)
        val autoLogIn = sharedPreferences.getBoolean("autoLogIn", false)
        findViewById<TextView>(R.id.welcome_message).text = "Welcome, ${sharedName}"
        /*findViewById<LinearLayout>(R.id.morse_in_action).setOnClickListener(){
            val intent = Intent(this, SendMorseActivity::class.java)
            startActivity(intent)
        }*/

        if (!autoLogIn)
            getFriends(prefUserId, userHash)

        getNewMessages(prefUserId, userHash)


        val intent = Intent(this, MorseCodeService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private fun getFriends(prefUserId: Int, userHash: String?) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val kontakti: List<EntitetKontakt> =
                    ContactsApi.retrofitService.getMyFriends(prefUserId, userHash.toString())

                for (friends in kontakti) {

                    getMessages(prefUserId, userHash, friends.id!!.toInt() )
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska ")
            }
        }
    }

    private fun getMessages(prefUserId: Int, userHash: String?, idContact: Int?) {
        val db = AppDatabase.getInstance(this)
        val messageDao: MessageDao = db.messageDao()

        lifecycleScope.launch(Dispatchers.Default) {
            try {

                val response: List<Message> =
                    idContact?.let {
                        MessagesApi.retrofitService.getMessages(prefUserId.toLong(), userHash,
                            it
                        )
                    }!!

                for (poruka in response) {
                    messageDao.insertAll(poruka)
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }


    private fun getNewMessages(id: Int, userHash: String?) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val response: List<Message> = MessagesApi.retrofitService.getNewMessages(
                    id,
                    userHash
                )
                if (response.isNotEmpty()) {
                    for (message in response) {
                        saveMessage(message)
                    }
                }
            } catch (e: Exception) {
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
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
            R.id.accelerometer_controls -> {
                sharedPreferences =
                    this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
                val hands_free = sharedPreferences.getBoolean("hands_free", false)
                val editor = sharedPreferences.edit()
                editor.putBoolean("hands_free", !hands_free)
                true
            }
            R.id.log_out -> {
                sharedPreferences =
                    this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()
                editor.commit()

                val db = AppDatabase.getInstance(this)
                val messageDao: MessageDao = db.messageDao()
                messageDao.deleteAll()

                val intent = Intent(this, LogInActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}