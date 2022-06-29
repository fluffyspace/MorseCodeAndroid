package com.example.morsecode

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.baza.AppDatabase
import com.example.morsecode.baza.MessageDao

class ReadFilesActivity : AppCompatActivity() {

    lateinit var tapButton: Button
    lateinit var sendButton: Button
    lateinit var morseButton: Button
    lateinit var recyclerView: RecyclerView
    lateinit var textEditMessage: EditText
    lateinit var textEditFile: EditText
    lateinit var textEditFileView: TextView

    lateinit var visual_feedback_container: VisualFeedbackFragment
    private lateinit var accelerometer: Accelerometer

    private lateinit var gyroscope: Gyroscope

    private lateinit var handsFree: HandsFreeFile

    var context = this

    private lateinit var sharedPreferences: SharedPreferences

    private var handsFreeOnChat = false

    lateinit var string: String

    var index:Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_files)

        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        accelerometer = Accelerometer(this)
        gyroscope = Gyroscope(this)
        handsFree = HandsFreeFile()

        sendButton = findViewById(R.id.sendButton)
        morseButton = findViewById(R.id.sendMorseButton)
        textEditMessage = findViewById(R.id.enter_message_edittext)
        textEditFile = findViewById(R.id.read_file)
        textEditFileView = findViewById(R.id.read_file)

        sharedPreferences =
            this.getSharedPreferences(ChatActivity.sharedPreferencesFile, Context.MODE_PRIVATE)
        val file = sharedPreferences.getString("file", "")
        string = file.toString()

        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        textEditFileView.text = file

        visual_feedback_container = VisualFeedbackFragment()
        visual_feedback_container.testing = true
        visual_feedback_container.layout1 = true
        supportFragmentManager
            .beginTransaction()
            .add(R.id.visual_feedback_container, visual_feedback_container, "main")
            .commitNow()

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
            val editor = sharedPreferences.edit()
            editor.putString("file", textEditFile.text.toString())
            editor.apply()
            Log.e("Stjepan ", textEditFile.text.toString())

            if (textEditMessage.text.isNotEmpty()) {
                var lines =
                    searchFile(textEditFile.text.toString(), textEditMessage.text.toString())
                Log.d("ingo", lines.toString())
                Log.d("ingo", getLines(textEditFile.text.toString()).toString())
            }
            //searchFileLine()
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
            handsFree.followGyroscope(rx, ry, rz)
        }
        handsFree.setListener(object : HandsFreeFile.Listener {
            override fun onTranslation(tap: Int) {
                if (tap == 1) {
                    visual_feedback_container.down()
                } else if (tap == 2) {
                    visual_feedback_container.up()
                } else if (tap == 3) {
                    visual_feedback_container.reset()
                    //searchFileLine()
                } else if (tap == 4) {
                    onBackPressed()
                } else if (tap == 6) {

                }
            }
        })
    }

    private fun getLines(file: String): MutableList<String>{
        val lines = mutableListOf<String>()
        var pocetak = 0
        for (i in 0 until file.length) {
            if (file[i] == '\n') {
                lines.add(file.substring(pocetak, i))
                pocetak = i+1
            }
        }
        lines.add(file.substring(pocetak, file.length))
        return lines
    }

    private fun getLineByIndex(file: String, index: Int): String {
        var pocetak = 0
        var kraj = file.length
        for (i in index downTo 0) {
            if (file[i] == '\n') {
                pocetak = i+1
                break
            }
        }
        for (i in index until file.length) {
            if (file[i] == '\n') {
                kraj = i
                break
            }
        }
        Log.e("stjepan", "$pocetak, $kraj")
        return file.substring(pocetak, kraj)
    }

    private fun searchFile(file: String, query: String): List<String> {
        val matches = query.toRegex().findAll(file).toList()
        val lines = mutableListOf<String>()
        for(match in matches) {

            val linja = getLineByIndex(file, match.range.first)

            Log.e("Stjepan", "$linja")
            lines.add(linja)
        }
        return lines.distinct()
    }


    private fun searchFile(string: String) {

        val st = string.toRegex()
        val match = st.find(this.string)
        if (match != null) {
            Log.e("Stjepan", match.range.first.toString())
            Log.e("Stjepan", match.range.last.toString())

            var pocetak = 0
            var kraj = 0
            for (i in match.range.first downTo 0) {
                if (this.string[i] == '\n' || i == 0) {
                    pocetak = i
                    break
                }

            }
            for (i in match.range.last until this.string.length) {
                if (this.string[i] == '\n') {
                    kraj = i
                    break
                }
            }
            Log.e("Stjepan",this.string.substring(pocetak, kraj))
            vibrate(this.string.substring(pocetak, kraj))

        }
    }

    private fun vibrate(str: String) {
        var mAccessibilityService = MorseCodeService.getSharedInstance();

        mAccessibilityService?.vibrateWithPWM(
            mAccessibilityService!!.makeWaveformFromText(
                str
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

                    morseButton.performClick()

                    if (handsFreeOnChat) {
                        handsFreeOnChat = false
                        accelerometer.unregister()
                        gyroscope.unregister()
                        handsFreeOnChatSet(false)
                    } else if (!handsFreeOnChat) {
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
}