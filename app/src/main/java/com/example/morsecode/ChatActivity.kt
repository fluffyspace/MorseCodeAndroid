package com.example.morsecode

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.CustomAdapter
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao
import com.example.morsecode.models.Message
import com.example.morsecode.network.MessagesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChatActivity : AppCompatActivity() {

    lateinit var tap_button: Button
    private val sharedPreferencesFile = "MyPrefs"
    lateinit var visual_feedback_container: VisualFeedbackFragment

    private var morseOn = false

    companion object {
        val USER_NAME = "username"
        val USER_ID = "id"
        val USER_PASSWORD = "password"
        val sharedPreferencesFile = "MyPrefs"
    }

    @SuppressLint("ClickableViewAccessibility")
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


        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = false
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        populateData(context, recyclerView, userId, contactId)

        val sendButton = findViewById<Button>(R.id.sendButton)
        val morseButton = findViewById<Button>(R.id.sendMorseButton)

        val lay = findViewById<LinearLayout>(R.id.topLayout)
        val layBottom = findViewById<LinearLayout>(R.id.layoutBottom)

        val textEditMessage = findViewById<TextView>(R.id.playground_text)

        //message listeners
        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                textEditMessage.text = changeText+ ""
            }
        })

        sendButton.setOnClickListener {
            Log.e("stjepan", "sendButton" + visual_feedback_container.getMessage())
            //performSendMessage(id.toLong(), sharedPassword, contactId)
            saveMessage(userId, contactId)
        }



        morseButton.setOnClickListener {
            var fra: FragmentContainerView = findViewById(R.id.visual_feedback_container)
            if (!morseOn) {
                val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.6f
                )
                lay.layoutParams = param
                val param1 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.4f
                )
                layBottom.layoutParams = param1
                morseOn = true

                fra.isVisible = true
            } else {
                val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.75f
                )
                lay.layoutParams = param
                val param1 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.25f
                )
                layBottom.layoutParams = param1
                morseOn = false
                fra.isVisible = false
            }

/*
            LargeBanner.make(it, "Ceci est une snackbar LARGE", LargeBanner.LENGTH_INDEFINITE)
                .setAction()
                .show()

 */
        }

        tap_button = findViewById(R.id.tap)

        tap_button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    visual_feedback_container.down()//Do Something
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    v.performClick()
                }
            }
            true
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
            poruke = messageDao.getAllSender(userId);

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

            val textMessage = findViewById<EditText>(R.id.playground_text)
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

        val textMessage = findViewById<EditText>(R.id.playground_text)
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