package com.example.morsecode

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.*
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

    companion object {
        val USER_NAME = "username"
        val USER_ID = "id"
        val USER_PASSWORD = "password"
        val USER_HASH = "logInHash"
        val sharedPreferencesFile = "MyPrefs"
        var handsFreeOn = false
    }

    lateinit var tapButton: Button
    lateinit var sendButton: Button
    lateinit var morseButton: Button
    lateinit var recyclerView: RecyclerView
    lateinit var textEditMessage: EditText
    private val sharedPreferencesFile = "MyPrefs"

    lateinit var visual_feedback_container: VisualFeedbackFragment
    private lateinit var accelerometer: Accelerometer

    private lateinit var handsFree: HandsFree
    private var morseOn = false

    private var start = true

    var context = this

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val contactName = intent.getStringExtra(USER_NAME).toString()
        val contactId = Integer(intent.getStringExtra(USER_ID))
        supportActionBar?.title = "$contactName"

        accelerometer = Accelerometer(this)
        handsFree = HandsFree()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val prefUserName: String = sharedPreferences.getString(USER_NAME, "").toString()
        val prefUserId = sharedPreferences.getInt("id", 0)
        val prefUserPassword = sharedPreferences.getString(USER_PASSWORD, "")
        val userId = Integer(prefUserId)
        val userHash = sharedPreferences.getString(USER_HASH, "")

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        context = this

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = true
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        getNewMessages(userId, userHash, contactId)
        //populateData(context, recyclerView, userId, contactId)

        //message listeners
        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                visual_feedback_container.setMessage(changeText)
            }
        })

        sendButton.setOnClickListener {
            Log.e("stjepan", "sendButton" + visual_feedback_container.getMessage())
            performSendMessage(userId, userHash, contactId)
            val poruka = Message(textEditMessage.text.toString(), contactId, userId)
            saveMessage(userId, contactId, listOf(poruka))
            visual_feedback_container.setMessage("")
        }

        morseButton.setOnClickListener {
            val fra: FragmentContainerView = findViewById(R.id.visual_feedback_container)

            morseOn = !morseOn
            fra.isVisible = morseOn

            /*if (!morseOn) {
                val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.6f
                )
                layoutTop.layoutParams = param
                val param1 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.4f
                )
                layBottom.layoutParams = param1

            } else {
                val param = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.75f
                )
                layoutTop.layoutParams = param
                val param1 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    0.25f
                )
                layBottom.layoutParams = param1
                morseOn = false
                fra.isVisible = false
            }*/

/*
            LargeBanner.make(it, "Ceci est une snackbar LARGE", LargeBanner.LENGTH_INDEFINITE)
                .setAction()
                .show()
*/

        }

        tapButton = findViewById(R.id.tap)
        tapButton.setOnTouchListener { v, event ->
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
/*
        accelerometer.setListener { x, y, z ->
            supportActionBar?.title = x.toString()
            handsFree.follow(x, z)
        }
*/
        handsFree.setListener(object : HandsFree.Listener {
            override fun onTranslation(tap: Int) {
                if (tap == 1) {
                   // visual_feedback_container.down()
                } else if (tap == 2) {
                  //  visual_feedback_container.up()
                } else if(tap == 3){
                    onBackPressed()
                }
            }
        })

        if (handsFreeOn){
            onResume()
        }
    }

    private fun getNewMessages(id: Integer, userHash: String?, contactId: Integer) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val response: List<Message> = MessagesApi.retrofitService.getNewMessages(
                    id.toInt(),
                    userHash
                )
                Log.e("stjepan", "poslana poruka$response")
                if (response.isNotEmpty()) {
                    saveMessage(id, contactId, response)
                }

            } catch (e: Exception) {
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    private fun saveMessage(userId: Integer, contactId: Integer, list: List<Message>) {
        if (list.isEmpty()) {
            return
        }
        try {

            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            for (x in list) {
                val poruka = Message(x.message, x.receiverId, x.senderId)
                messageDao.insertAll(poruka)
            }

            populateData(context, recyclerView, userId, contactId)

            Log.e("stjepan", "db uspelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db neje uspelo" + e.stackTraceToString() + e.message.toString())
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
        this@ChatActivity.runOnUiThread(java.lang.Runnable {
            recyclerView.adapter = CustomAdapter(
                context,
                poruke,
                userId.toInt(),
                contactId.toInt()
            )
            recyclerView.scrollToPosition(poruke.size - 1)
        })
    }

    private fun performSendMessage(id: Integer, sharedPassword: String?, contactId: Integer) {

        val textMessage = findViewById<EditText>(R.id.enter_message_edittext)
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val response: Boolean = MessagesApi.retrofitService.sendMessage(
                    id.toLong(),
                    sharedPassword,
                    contactId,
                    textMessage.text.toString()
                )
                Log.e("stjepan", "poslana poruka$response")
            } catch (e: Exception) {
                Log.e("Stjepan", "poslana poruka $id + $sharedPassword + $contactId ")
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }
/*
    private fun getMessageContacts(id: Long, sharedPassword: String?){
        lifecycleScope.launch(Dispatchers.Default) {

            try {
                val response = MessagesApi.retrofitService.getMessages(
                    id,
                    sharedPassword

                )

                Log.e("stjepan", "poslana poruka$response")
            } catch (e: Exception) {
                Log.e("Stjepan", "poslana poruka $id + $sharedPassword + ")
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }

        }
    }


 */

    private fun getMessages(id: Long, sharedPassword: String?, contactId: Integer) {
        lifecycleScope.launch(Dispatchers.Default) {

            try {
                val response = MessagesApi.retrofitService.getMessages(
                    id,
                    sharedPassword,
                    contactId
                )
                Log.e("stjepan", "poslana poruka$response")
            } catch (e: Exception) {
                Log.e("Stjepan", "poslana poruka $id + $sharedPassword + ")
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.hands_free -> {
                try {
                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        vibrator.vibrate(200)
                    }
                    morseButton.performClick()

                    Toast.makeText(
                        this,
                        "vibration" + Toast.LENGTH_SHORT.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {

                }
/*
                if (accelerometer.on) {
                    onPause()
                } else {
                    onResume()
                }
*/
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        if (!start) {
            accelerometer.register()
        } else {
            start = false
        }
        super.onResume()
    }

    override fun onPause() {
        accelerometer.unregister()
        super.onPause()
    }
}