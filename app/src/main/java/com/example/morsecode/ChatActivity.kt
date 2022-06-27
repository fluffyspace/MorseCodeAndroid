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
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    companion object {
        val USER_NAME = "username"
        val USER_ID = "id"
        val USER_PASSWORD = "password"
        val USER_HASH = "logInHash"
        val sharedPreferencesFile = "MyPrefs"

    }

    lateinit var tapButton: Button
    lateinit var sendButton: Button
    lateinit var morseButton: Button
    lateinit var recyclerView: RecyclerView
    lateinit var textEditMessage: EditText
    private val sharedPreferencesFile = "MyPrefs"

    private var chatAdapter: CustomAdapter? = null

    lateinit var visual_feedback_container: VisualFeedbackFragment

    private lateinit var accelerometer: Accelerometer
    private lateinit var gyroscope: Gyroscope
    //private lateinit var magnetometer: Magnetometer

    private lateinit var handsFree: HandsFree
    private var morseOn = false

    private var start = true

    var context = this


    private var handsFreeOnChat = false
    private var morseFragmentOn = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val contactName = intent.getStringExtra(USER_NAME).toString()
        val contactId = intent.getLongExtra(USER_ID, -1).toInt()
        supportActionBar?.title = "$contactName ($contactId)"

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        //magnetometer = Magnetometer(this)
        handsFree = HandsFree()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val prefUserName: String = sharedPreferences.getString(USER_NAME, "").toString()
        val prefUserId = sharedPreferences.getInt("id", 0)
        val prefUserPassword = sharedPreferences.getString(USER_PASSWORD, "")
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

        //(userId, userHash, contactId)
        getNewMessages(prefUserId,userHash)
        populateData(context, recyclerView, prefUserId, contactId)

        //message listeners
        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                visual_feedback_container.setMessage(changeText)
                textEditMessage.setText(changeText)
                Log.e("Stjepan " , visual_feedback_container.getMessage())
            }

            override fun finish(gotovo: Boolean) {
                if (gotovo){
                    sendButton.performClick()
                    vibrator.vibrate(100)

                }
            }
        })

        sendButton.setOnClickListener {
            Log.e("stjepan", "sendButton" + visual_feedback_container.getMessage())
            performSendMessage(prefUserId, userHash, contactId)
            val poruka = Message(0, textEditMessage.text.toString(), contactId, prefUserId)
            saveMessage(poruka)
            textEditMessage.setText("")
            visual_feedback_container.setMessage("")
        }

        morseButton.setOnClickListener {
            val fra: FragmentContainerView = findViewById(R.id.visual_feedback_container)

            morseOn = !morseOn
            fra.isVisible = morseOn

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

        accelerometer.setListener { x, y, z, xG, yG, zG ->
            //supportActionBar?.title = z.toString()
            handsFree.followAccelerometer(x, y, z, xG, yG, zG)
        }

        gyroscope.setListener { rx, ry, rz ->
            supportActionBar?.title = rx.toString()
            handsFree.followGyroscope(rx, ry, rz)
        }


        handsFree.setListener(object : HandsFree.Listener {
            override fun onTranslation(tap: Int) {
                if (tap == 1) {
                    visual_feedback_container.down()
                } else if (tap == 2) {
                    visual_feedback_container.up()
                } else if(tap == 3){
                    visual_feedback_container.reset()
                } else if(tap == 4){
                    onBackPressed()
                }
            }
        })



    }

    private fun saveMessage(message: Message) {
        try {
            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(message)

            chatAdapter!!.list.add(message)
            chatAdapter!!.notifyDataSetChanged()

            Log.e("stjepan", "db uspelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db neje uspelo" + e.stackTraceToString() + e.message.toString())
        }
    }

    private fun populateData(
        context: Context,
        recyclerView: RecyclerView,
        userId: Int,
        contactId: Int
    ) {
        val db = AppDatabase.getInstance(this)
        val messageDao: MessageDao = db.messageDao()
        val poruke: List<Message>

        if (contactId == userId) {
            poruke = messageDao.getAllSender(userId);
        } else {
            poruke = messageDao.getAllReceived(contactId, userId)
        }
        Log.d("ingo", "broj poruka -> " + poruke.size)
        this@ChatActivity.runOnUiThread(java.lang.Runnable {
            chatAdapter = CustomAdapter(
                context,
                poruke,
                userId.toInt(),
                contactId.toInt()
            )
            recyclerView.adapter = chatAdapter
            recyclerView.scrollToPosition(poruke.size - 1)
        })
    }

    private fun performSendMessage(id: Int, sharedPassword: String?, contactId: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val response: Boolean = MessagesApi.retrofitService.sendMessage(
                    id.toLong(),
                    sharedPassword,
                    contactId,
                    textEditMessage.text.toString()
                )
                Log.e("stjepan", "poslana poruka$response")
            } catch (e: Exception) {
                Log.e("Stjepan", "poslana poruka $id + $sharedPassword + $contactId ")
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
                        withContext(Dispatchers.Main) {
                            saveMessage(message)
                        }
                    }
                }

            } catch (e: Exception) {
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

                    if (handsFreeOnChat) {
                        handsFreeOnChat = false
                        accelerometer.unregister()
                        gyroscope.unregister()
                    } else if(!handsFreeOnChat) {
                        handsFreeOnChat = true
                        accelerometer.register()
                        gyroscope.register()
                    }

                    Toast.makeText(
                        this,
                        "vibration" + Toast.LENGTH_SHORT.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {

                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        if (handsFreeOnChat) {
            accelerometer.register()
            gyroscope.register()
        }
        super.onResume()
    }

    override fun onPause() {
        gyroscope.unregister()
        accelerometer.unregister()
        super.onPause()
    }
}