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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.ChatAdapter
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao
import com.example.morsecode.models.Message
import com.example.morsecode.network.ContactIdRequest
import com.example.morsecode.network.SendMessageRequest
import com.example.morsecode.network.getMessagesApiService
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChatActivity : AppCompatActivity() {

    lateinit var tapButton: Button
    lateinit var sendButton: Button
    lateinit var morseButton: Button
    lateinit var recyclerView: RecyclerView
    lateinit var textEditMessage: EditText
    lateinit var handsFreeIndicator: TextView
    lateinit var layoutBottom: ConstraintLayout


    private var chatAdapter: ChatAdapter? = null
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
    var lastPosition: Int = HandsFree.DOWN

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        mAccessibilityService = MorseCodeService.getSharedInstance()
        fragmentContainerView = findViewById(R.id.visual_feedback_container)
        val contactName = intent.getStringExtra(Constants.USER_NAME).toString()
        contactId = intent.getLongExtra(Constants.USER_ID, -1).toInt()
        prefUserId = mAccessibilityService?.servicePostavke?.userId!!

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        supportActionBar?.title = "$contactName"

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        handsFree = HandsFree()
        handsFree.profile = mAccessibilityService?.profile

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)
        handsFreeIndicator = findViewById(R.id.hands_free_indicator)
        layoutBottom = findViewById(R.id.layoutBottom)

        handsFreeOnChat = mAccessibilityService?.servicePostavke?.handsFreeOnChat ?: Constants.HANDS_FREE_DEFAULT//sharedPreferences.getBoolean("hands_free", false)

        mAccessibilityService?.addListener(object : MorseCodeService.OnSocketNewMessageListener {
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


        getNewMessages()
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
                if(lastPosition == tap) return
                lastPosition = tap
                if (tap == HandsFree.UP) {
                    visual_feedback_container.down()
                } else if (tap == HandsFree.DOWN) {
                    visual_feedback_container.up()
                } else if (tap == 3) {
                    visual_feedback_container.reset()
                    getNewMessages()
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

            override fun onNewData(x: Float, y: Float, z: Float) {
                //TODO("Not yet implemented")
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
        val messages = messageDao.getLastReceived(prefUserId,contactId)
        if(!messages.isEmpty()) vibrateMessage(messages[0].message.toString())
    }

    fun onNewMessageReceived(message: Message) {
        if (sync) {
            vibrateMessage(message.message.toString())
            Log.d("Stjepan ", "vibrate message ${message.message.toString()}")
        }
        /*saveMessage(message)
        addMessageToView(message)*/
        Log.d("Stjepan ", "${Gson().toJson(message)} $prefUserId $contactId")
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
        soundPool.play(message_received_sound, 1F, 1F, 1, 0, 1f)
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
            chatAdapter = ChatAdapter(
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
        val ctx = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val id = getMessagesApiService(ctx).sendMessage(
                        to=contactId,
                        message=textEditMessage.text.toString()
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
                Log.e("stjepan", "greska performSendMessage " + e.stackTraceToString() + e.message.toString())
            }
        }
    }

    private fun getNewMessages() {
        val ctx = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val response: List<Message> = getMessagesApiService(ctx).getNewMessages()
                if (response.isNotEmpty()) {

                    for (message in response) {
                        withContext(Dispatchers.Main) {
                            saveMessage(message)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("stjepan", "greska getNewMessages " + e.stackTraceToString() + e.message.toString())
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
                    getMessagesApiService(ctx).deleteMessages(contactId.toLong())
                if(deleted){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, "Messages deleted successfuly", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska deleteMessages " + e)
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
                //Toast.makeText(this, "sync $sync", Toast.LENGTH_LONG).show()
                Snackbar.make(layoutBottom, "Synchronous vibration is " + if(sync) "enabled" else "disabled", Snackbar.LENGTH_SHORT)
                    .setAnchorView(layoutBottom)
                    .show()
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
        Toast.makeText(this, "Hands free " + if(handsFreeOnChat) "ON" else "OFF", Toast.LENGTH_SHORT).show()

        fragmentContainerView.isVisible = handsFreeOnChat
    }

    fun turnHandsFreeOn(){
        accelerometer.register()
        gyroscope.register()
        handsFreeOnChatSet(true)
        handsFreeIndicator.visibility = View.VISIBLE
        fragmentContainerView.isVisible = true
    }

    private fun handsFreeOnChatSet(b: Boolean) {
        mAccessibilityService?.servicePostavke?.handsFreeOnChat = b
        mAccessibilityService?.savePostavke()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d("ingo", "key pressed")
            if(lastPosition == HandsFree.UP) return true
            lastPosition = HandsFree.UP
            visual_feedback_container.down()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d("ingo", "key released")
            if(lastPosition == HandsFree.DOWN) return true
            lastPosition = HandsFree.DOWN
            visual_feedback_container.up()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}