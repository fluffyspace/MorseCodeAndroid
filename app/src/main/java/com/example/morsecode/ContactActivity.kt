package com.example.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.KontaktiAdapter
import com.example.morsecode.ChatActivity.Companion.USER_HASH
import com.example.morsecode.ChatActivity.Companion.handsFreeOn
import com.example.morsecode.ChatActivity.Companion.sharedPreferencesFile
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.GetIdResponse
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactActivity : AppCompatActivity() {

    private lateinit var kontakt: List<EntitetKontakt>
    private lateinit var accelerometer: Accelerometer
    private lateinit var handsFreeContact1: HandsFreeContact1
    private lateinit var handsFree: HandsFree
    private var contactCounter = 0
    private var maxContactCounter: Int = 0

    private var start = true

    var mAccessibilityService:MorseCodeService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("id", 0)
        val userLoginHash = sharedPreferences.getString(USER_HASH, "noHash");

        refreshContacts(userId, userLoginHash.toString())

        accelerometer = Accelerometer(this)
        handsFreeContact1 = HandsFreeContact1()
        handsFree = HandsFree()

        accelerometer.setListener { x, y, z, xG, yG, zG ->
            supportActionBar?.title = z.toString()
            handsFreeContact1.follow(x, y, z, xG, yG, zG)
        }


        handsFreeContact1.setListener(object : HandsFreeContact1.Listener {
            override fun onTranslation(tap: Int) {
                //Log.e(" contact tap", "$tap")
                selectContact(tap)
            }
        })

        var jeste: Boolean = false

        val fab: View = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener { view ->
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            builder.setTitle("Add New Contact")
            val dialogLayout = inflater.inflate(R.layout.add_contact_dialog, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)
            builder.setView(dialogLayout)
            builder.setNegativeButton("Close") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            builder.setPositiveButton("OK") { dialogInterface, i ->
                val friendName = editText.text.toString()
                //Log.e("text", friendName)

                if (friendName != "") {
                    lifecycleScope.launch(Dispatchers.Default) {
                        try {
                            var friend: GetIdResponse


                            friend = ContactsApi.retrofitService.getUserByUsername(
                                userId,
                                userLoginHash.toString(), friendName
                            )


                            val friendId = friend?.id


                            var add = ContactsApi.retrofitService.addFriend(
                                userId,
                                userLoginHash.toString(), friendId
                            )

                            if (!add) {
                                jeste = true
                            }
                        } catch (e: Exception) {
                            //Toast.makeText(applicationContext, "There is no contact under that name", Toast.LENGTH_SHORT).show()
                        }
                    }
                    refreshContacts(userId, userLoginHash.toString())
                } else {
                    Toast.makeText(applicationContext, "No name entered", Toast.LENGTH_SHORT).show()
                    fab.performClick()
                }

            }
            builder.show()
        }

        if (handsFreeOn) {
            onResume()
        }

        val friendNameDelete = intent.getStringExtra("nameFriend")
        var friendIdDelete = intent.getStringExtra("idFriend")
        val idFriendDelete = friendIdDelete?.toInt()

        if (friendIdDelete != null && idFriendDelete != -1) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete contact $friendNameDelete")
            builder.setNegativeButton("Close") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            builder.setPositiveButton("OK") { dialogInterface, i ->
                lifecycleScope.launch(Dispatchers.Default) {
                    try {

                        var response = idFriendDelete?.let {
                            ContactsApi.retrofitService.removeFriend(
                                userId, userLoginHash.toString(),
                                it
                            )
                        }
                        refreshContacts(userId, userLoginHash.toString())

                    } catch (e: Exception) {
                        Log.d("stjepan", "greska ")
                    }
                }
            }
            builder.show()
        }
    }

    private fun contactAdded() {
        Toast.makeText(
            applicationContext,
            "Contact added",
            Toast.LENGTH_SHORT
        ).show()
        //Log.e("added", " added")
        val intent = Intent(this@ContactActivity, ContactActivity::class.java)
        startActivity(intent)
    }

    fun refreshContacts(userId: Int, userHash: String) {
        val kontaktiRecyclerView: RecyclerView = findViewById(R.id.recycler)
        kontaktiRecyclerView.layoutManager = LinearLayoutManager(this)
        val context = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val kontakti: List<EntitetKontakt> =
                    ContactsApi.retrofitService.getMyFriends(userId, userHash)
                kontakt = kontakti
                maxContactCounter = kontakti.size - 1

                //Log.e("max ", " $maxContactCounter")
                withContext(Dispatchers.Main) {
                    kontaktiRecyclerView.adapter = KontaktiAdapter(context, kontakti)
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska ")
            }
        }
    }

    fun selectContact(command: Int) {
        when (command) {
            3 -> {
                if (contactCounter < maxContactCounter) {
                    contactCounter++
                } else {
                    contactCounter = 1
                }
                vibrateName(contactCounter)
            }
            4 -> {
                if (contactCounter > 0) {
                    contactCounter--
                } else {
                    contactCounter = maxContactCounter
                }
                vibrateName(contactCounter)
            }
            1 -> {
                startContactChat(contactCounter)
            }
        }
    }

    private fun startContactChat(index: Int) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("username", kontakt[index].username)
        intent.putExtra("id", kontakt[index].id.toString())
        ContextCompat.startActivity(this, intent, null)
    }

    fun vibrateName(index: Int) {
        //TODO vibrate contact name

        mAccessibilityService = MorseCodeService.getSharedInstance();

        Log.e("stejpan " , kontakt[index].username.toString())

        var a = mAccessibilityService?.makeWaveformFromText(kontakt[index].username)
        Log.e("stejpan " , a.toString())

        mAccessibilityService?.vibrateWithPWM(mAccessibilityService!!.makeWaveformFromText(kontakt[index].username.toString()))
        Toast.makeText(
            applicationContext,
            "Current contact " + kontakt[index].username,
            Toast.LENGTH_LONG
        ).show()
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
                    handsFreeOn = true

                    Toast.makeText(
                        this,
                        "vibration" + Toast.LENGTH_SHORT.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                }

                if (accelerometer.on) {
                    onPause()
                } else {
                    onResume()
                }
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