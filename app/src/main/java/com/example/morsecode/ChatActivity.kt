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
import com.example.morsecode.models.Message
import com.example.morsecode.network.MessagesApi
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
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

    private var chatAdapter: CustomAdapter? = null
    lateinit var visual_feedback_container: VisualFeedbackFragment
    private lateinit var accelerometer: Accelerometer

    private lateinit var gyroscope: Gyroscope

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var handsFree: HandsFree
    private var handsFreeOnChat = false

    var context = this
    lateinit var mSocket: Socket
    var prefUserId = -1

    lateinit var soundPool: SoundPool
    var message_received_sound: Int = -1
    var contactId: Int = -1

        @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val contactName = intent.getStringExtra(Constants.USER_NAME).toString()
        contactId = intent.getLongExtra(Constants.USER_ID, -1).toInt()

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        supportActionBar?.title = "$contactName ($contactId)"

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        handsFree = HandsFree()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)

        sharedPreferences = this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val prefUserName: String = sharedPreferences.getString(Constants.USER_NAME, "").toString()
        prefUserId = sharedPreferences.getInt("id", 0)
        val userHash = sharedPreferences.getString(Constants.USER_HASH, "")
        val socketioIp = sharedPreferences.getString(Constants.SOCKETIO_IP, Constants.DEFAULT_SOCKETIO_IP)
        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        try{
            mSocket = IO.socket(socketioIp);
            Log.d("connect0", "not error");
        }catch (e: URISyntaxException) {
            Log.e("connect0", "error");
        }
        mSocket.on("new message", onNewMessage);
        mSocket.on("chat message", onChatMessage);
        mSocket.on("test message", onTestMessage);
        mSocket.on(Socket.EVENT_CONNECT) { Log.d("ingo", "socket connected $it") }
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onError)
        mSocket.disconnect().connect()
        Log.d("ingo", "connected? " + mSocket.connected())

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = true
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

        getNewMessages(prefUserId,userHash)
        populateData(context, recyclerView, prefUserId, contactId)

        //message listeners
        visual_feedback_container.setListener(object : VisualFeedbackFragment.Listener {
            override fun onTranslation(changeText: String) {
                visual_feedback_container.setMessage(changeText)
                textEditMessage.setText(changeText)
                //Log.e("Stjepan " , visual_feedback_container.getMessage())
            }

            override fun finish(gotovo: Boolean) {
                if (gotovo){
                    sendButton.performClick()
                    vibrator.vibrate(100)

                }
            }
        })

        sendButton.setOnClickListener {
            Log.d("stjepan", "sendButton " + visual_feedback_container.getMessage())
            performSendMessage(prefUserId, userHash, contactId)
        }

        morseButton.setOnClickListener {
            val fra: FragmentContainerView = findViewById(R.id.visual_feedback_container)
            fra.isVisible = !(fra.isVisible)
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

    private val onNewMessage =
        Emitter.Listener { args ->
            this.runOnUiThread(Runnable {
                val messageInJson: String = (args[0] as JSONObject).getString("message")
                val message: Message = Gson().fromJson(messageInJson, Message::class.java)
                saveMessage(message)
                addMessageToView(message)
            })
        }

    private val onChatMessage =
        Emitter.Listener { args ->
            this.runOnUiThread(Runnable {
                val messageText: String = args[0] as String
                val message = Message(0, messageText, 0, 0)

                Log.d("ingo", "new message received -> ${message}")
                // provjeriti ako je za ovaj chat i ako nisam ja pošiljatelj
                if(message.receiverId == prefUserId) {
                    Log.d("ingo", "message is for me :)")
                    saveMessage(message)
                    if(message.senderId == contactId) {
                        Log.d("ingo", "and message is for this currently opened chat")
                        // add the message to view
                        addMessageToView(message)
                    }
                }
            })
        }

    private val onTestMessage =
        Emitter.Listener { args ->
            this.runOnUiThread(Runnable {
                val message = args[0] as String

                // add the message to view
                Toast.makeText(this, message, Toast.LENGTH_SHORT)
            })
        }

    private val onError =
        Emitter.Listener { args ->
            for(arg in args) {
                Log.d("ingo", arg.toString())
            }
        }

    private fun addMessageToView(message: Message) {
        chatAdapter!!.list.add(message)
        chatAdapter!!.notifyDataSetChanged()
        recyclerView.scrollToPosition(chatAdapter!!.list.size-1)
        soundPool.play(message_received_sound, 1F, 1F, 1, 0, 1f)
    }

    private fun saveMessage(message: Message) {
        try {
            val db = AppDatabase.getInstance(this)
            val messageDao: MessageDao = db.messageDao()
            messageDao.insertAll(message)
            Log.d("stjepan", "db uspelo")
        } catch (e: android.database.sqlite.SQLiteConstraintException){
            
        } catch (e: Exception) {
            Log.e("stjepan", "db neje uspelo " + e.stackTraceToString() + e.message.toString())
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

    private fun performSendMessage(id: Int, sharedPassword: String?, contactId: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val id = MessagesApi.retrofitService.sendMessage(
                    id.toLong(),
                    sharedPassword,
                    contactId,
                    textEditMessage.text.toString()
                )
                Log.d("stjepan", "poslana poruka? $id")
                withContext(Dispatchers.Main){
                    if(id != -1L) {
                        Log.d("ingo", "poruka $id uspješno poslana")
                        val poruka = Message(id, textEditMessage.text.toString(), contactId, prefUserId)
                        saveMessage(poruka)
                        addMessageToView(poruka)
                        textEditMessage.setText("")
                        visual_feedback_container.setMessage("")
                        val newMessage = JSONObject()
                        newMessage.put("message", Gson().toJson(poruka).toString())
                        mSocket.emit("new message", newMessage);
                    } else {
                        Log.d("ingo", "poruka nije poslana")
                    }
                }

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
                        handsFreeOnChatSet(false)
                    } else if(!handsFreeOnChat) {
                        handsFreeOnChat = true
                        accelerometer.register()
                        gyroscope.register()
                        handsFreeOnChatSet(true)
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

    private fun handsFreeOnChatSet(b: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("hands_free", b)
        editor.apply()
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

    override fun onDestroy() {
        mSocket.disconnect();
        mSocket.off("new message", onNewMessage);

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            // calibrate
            Log.d("ingo", "calibration started")

            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}