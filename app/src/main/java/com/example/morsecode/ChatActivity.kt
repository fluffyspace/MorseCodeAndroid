package com.example.morsecode

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.CustomAdapter
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao
import com.example.morsecode.models.Message
import com.example.morsecode.network.MessagesApi
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private val sharedPreferencesFile = "MyPrefs"

    companion object {
        val USER_NAME = "username"
        val USER_ID = "id"
        val USER_PASSWORD = "password"
        val sharedPreferencesFile = "MyPrefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val context = this

        val contactName = intent.getStringExtra(USER_NAME).toString()
        val contactId = Integer(intent.getStringExtra(USER_ID))

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val prefUserName: String = sharedPreferences.getString(USER_NAME, "").toString()
        val prefUserId = sharedPreferences.getInt("id", 0)
        val prefUserPassword = sharedPreferences.getString(USER_PASSWORD, "")
        val userId: Integer = Integer(prefUserId)
        supportActionBar?.title = "$contactName id $contactId"



        populateData(context, recyclerView, userId, contactId)

        val sendButton = findViewById<Button>(R.id.sendButton)
        val sendMorseButton = findViewById<Button>(R.id.sendMorseButton)

        sendButton.setOnClickListener {
            Log.e("stjepan", "sendButton")
            //performSendMessage(id.toLong(), sharedPassword, contactId)

            saveMessage(userId, contactId)
        }
        sendMorseButton.setOnClickListener {
            Log.e("Stjepan", "send Morse code button clicked")

            val snack = Snackbar.make(it,"xhack bar", Snackbar.LENGTH_LONG)
            snack.show()
        }

    }

    private fun populateData(
        context: Context,
        recyclerView: RecyclerView,
        userId: Integer,
        contactId: Integer
    ) {
        val db = AppDatabase.getInstance(this)
        val messageDao: MessageDao = db.messageDao()
        val poruke: List<Message>

        if (contactId == userId) {
            poruke= messageDao.getAllSender(userId);

        } else {
            poruke = messageDao.getAllReceived(contactId, userId)
        }
        recyclerView.adapter = CustomAdapter(
            context,
            poruke,
            userId.toInt(),
            contactId.toInt()
        )
    }

    private fun saveMessage(userId: Integer, contactId: Integer) {
        try {

            val textMessage = findViewById<EditText>(R.id.editTextMessage)
            val poruka = Message(textMessage.text.toString(), contactId, userId)

            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(poruka)

            Log.e("stjepan", "db uspelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db neje uspelo" + e.stackTraceToString() + e.message.toString())
        }
    }

    private fun performSendMessage(id: Long, sharedPassword: String?, contactId: Integer) {

        val textMessage = findViewById<EditText>(R.id.editTextMessage)
        lifecycleScope.launch(Dispatchers.Default) {

            try {
                val response: Boolean = MessagesApi.retrofitService.sendMessage(
                    id,
                    sharedPassword,
                    contactId,
                    textMessage.text.toString()
                )
                Log.e("stjepan", "poslana poruka$response")
            } catch (e: Exception) {
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }

        }


    }


}