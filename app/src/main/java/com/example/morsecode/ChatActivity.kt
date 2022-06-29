package com.example.morsecode

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.CustomAdapter
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.Message
import com.example.morsecode.network.ContactsApi
import com.example.morsecode.network.MessagesApi
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URISyntaxException


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
    lateinit var handsFreeIndicator: TextView

    private var chatAdapter: CustomAdapter? = null
    lateinit var visual_feedback_container: VisualFeedbackFragment
    private lateinit var accelerometer: Accelerometer

    private lateinit var gyroscope: Gyroscope

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handsFree: HandsFree
    private var handsFreeOnChat = false
    lateinit var fragmentContainerView: FragmentContainerView

    var context = this
    var prefUserId = -1
    var userHash = ""

    lateinit var soundPool: SoundPool
    var message_received_sound: Int = -1
    var contactId: Int = -1

    var mAccessibilityService: MorseCodeService? = null

    var sync = false
    var lastMessage: String = "e"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        mAccessibilityService = MorseCodeService.getSharedInstance();
        fragmentContainerView = findViewById(R.id.visual_feedback_container)
        val contactName = intent.getStringExtra(Constants.USER_NAME).toString()
        contactId = intent.getLongExtra(Constants.USER_ID, -1).toInt()

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        supportActionBar?.title = "$contactName"

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        handsFree = HandsFree()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)
        handsFreeIndicator = findViewById(R.id.hands_free_indicator)

        sharedPreferences = this.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)
        val prefUserName: String = sharedPreferences.getString(Constants.USER_NAME, "").toString()
        prefUserId = sharedPreferences.getInt("id", 0)
        userHash = sharedPreferences.getString(Constants.USER_HASH, "").toString()

        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        mAccessibilityService?.setListener(object : MorseCodeService.OnSocketNewMessageListener {
            override fun onNewMessage(message: Message) {
                Log.e("ingo", "poruka bi kao trebala dojti")
                this@ChatActivity.runOnUiThread {
                    onNewMessageReceived(message)
                }
            }
        })

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = true
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()


        getNewMessages(prefUserId, userHash, false)
        populateData(context, recyclerView, prefUserId, contactId)

        //message listeners
        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                visual_feedback_container.setMessage(changeText)
                textEditMessage.setText(changeText)
            }

            override fun finish(gotovo: Boolean) {
                if (gotovo) {
                    sendButton.performClick()
                    vibrator.vibrate(100)
                } else {
                    vibrator.vibrate(100)
                }
            }
        })

        sendButton.setOnClickListener {
            performSendMessage(prefUserId, userHash, contactId)
        }

        morseButton.setOnClickListener {
            fragmentContainerView.isVisible = !(fragmentContainerView.isVisible)
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
            handsFree.followAccelerometer(x, y, z, xG, yG, zG)
        }

        gyroscope.setListener { rx, ry, rz ->
            handsFree.followGyroscope(rx, ry, rz)
        }

        handsFree.setListener(object : HandsFree.Listener {
            override fun onTranslation(tap: Int) {
                if (tap == 1) {
                    visual_feedback_container.down()
                } else if (tap == 2) {
                    visual_feedback_container.up()
                } else if (tap == 3) {
                    visual_feedback_container.reset()

                    getNewMessages(prefUserId, userHash, false)

                    vibrateLastMessage(prefUserId, contactId)

                } else if (tap == 4) {
                    onBackPressed()
                } else if (tap == 5) {
                    visual_feedback_container.reset()

                    mAccessibilityService?.vibrateWithPWM(
                        mAccessibilityService!!.makeWaveformFromText(
                            "e"
                        )
                    )
                }
            }
        })

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        message_received_sound = soundPool.load(
            this,
            R.raw.message_received,
            1
        )
    }

    private fun vibrateLastMessage(prefUserId: Int, contactId: Int) {
        val db = AppDatabase.getInstance(this)
        val messageDao: MessageDao = db.messageDao()
        val message = messageDao.getLastReceived(prefUserId,contactId)

        vibrateMessage(message[0].message.toString())
    }

    fun onNewMessageReceived(message: Message) {
        if (sync) {
            vibrateMessage(message.message.toString())
            Log.d("Stjepan ", "vibrate message ${message.message.toString()}")
        }
        /*saveMessage(message)
        addMessageToView(message)*/

        if (message.receiverId == prefUserId) {
            Log.d("ingo", "message is for me :)")
            saveMessage(message)
            if (message.senderId == contactId) {
                Log.d("ingo", "and message is for this currently opened chat")
                // add the message to view
                addMessageToView(message)
            }
        }
    }

    private fun addMessageToView(message: Message) {
        chatAdapter!!.list.add(message)
        chatAdapter!!.notifyDataSetChanged()
        recyclerView.scrollToPosition(chatAdapter!!.list.size - 1)
        //soundPool.play(message_received_sound, 1F, 1F, 1, 0, 1f)
    }

    private fun saveMessage(message: Message) {
        try {
            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(message)

            Log.e("stjepan", "db uspjelo")
        } catch (e: Exception) {
            Log.e("stjepan", "db nije uspjelo " + e.stackTraceToString() + e.message.toString())
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
            val layoutManager = LinearLayoutManager(context)
            layoutManager.stackFromEnd = true
            recyclerView.layoutManager = layoutManager;
            recyclerView.adapter = chatAdapter
            recyclerView.scrollToPosition(poruke.size - 1)
        })
    }

    private fun performSendMessage(userId: Int, sharedPassword: String?, contactId: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val id = MessagesApi.retrofitService.sendMessage(
                    userId.toLong(),
                    sharedPassword,
                    contactId,
                    textEditMessage.text.toString()
                )
                Log.d("stjepan", "poslana poruka? $id")
                withContext(Dispatchers.Main) {
                    if (id != -1L) {
                        Log.d("ingo", "poruka $id uspješno poslana")
                        val poruka =
                            Message(id, textEditMessage.text.toString(), contactId, prefUserId)
                        saveMessage(poruka)
                        addMessageToView(poruka)

                        textEditMessage.setText("")
                        visual_feedback_container.setMessage("")

                        mAccessibilityService?.emitMessage(poruka)
                    } else {
                        Log.d("ingo", "poruka nije poslana")
                    }
                }

            } catch (e: Exception) {
                Log.e("Stjepan", "poslana poruka $contactId + $sharedPassword + $contactId ")
                Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    private fun getNewMessages(id: Int, userHash: String?, b: Boolean) {
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

    fun deleteMessages(){
        val ctx: Context = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val db = AppDatabase.getInstance(ctx)
                val messageDao: MessageDao = db.messageDao()
                messageDao.deleteAll()
                withContext(Dispatchers.Main){
                    chatAdapter?.list = listOf()
                    chatAdapter?.notifyDataSetChanged()
                }

                Log.d("ingo", "$prefUserId, $userHash, $contactId")
                val deleted: Boolean =
                    MessagesApi.retrofitService.deleteMessages(prefUserId, userHash, contactId)
                if(deleted){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, "Messages deleted successfuly", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska " + e)
            }
        }
    }

    private fun vibrateMessage(lastMessage: String) {
        mAccessibilityService = MorseCodeService.getSharedInstance();

        mAccessibilityService?.vibrateWithPWM(
            mAccessibilityService!!.makeWaveformFromText(
                lastMessage
            )
        )

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
                    toggleHandsFree()

                } catch (e: Exception) {

                }
                true
            }
            R.id.sync -> {
                sync = !sync

                Toast.makeText(this, "sync $sync", Toast.LENGTH_LONG).show()
                Log.e("Stjepan ", "sync $sync")
                true
            }
            R.id.clear_messages -> {
                deleteMessages()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun toggleHandsFree(){
        if (handsFreeOnChat) {
            accelerometer.unregister()
            gyroscope.unregister()
            handsFreeOnChatSet(false)
            handsFreeIndicator.visibility = View.GONE
        } else if(!handsFreeOnChat) {
            turnHandsFreeOn()
        }
        handsFreeOnChat = !handsFreeOnChat

        fragmentContainerView.isVisible = handsFreeOnChat
    }

    fun turnHandsFreeOn(){
        accelerometer.register()
        gyroscope.register()
        handsFreeOnChatSet(true)
        handsFreeIndicator.visibility = View.VISIBLE
    }

    private fun handsFreeOnChatSet(b: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("hands_free", b)
        editor.apply()
    }

    override fun onResume() {
        if (handsFreeOnChat) {
            turnHandsFreeOn()
        }
        super.onResume()
    }

    override fun onPause() {
        gyroscope.unregister()
        accelerometer.unregister()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // calibrate
            Log.d("ingo", "calibration started")

            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}