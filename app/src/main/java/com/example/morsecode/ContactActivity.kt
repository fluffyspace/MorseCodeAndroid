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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.KontaktiAdapter
import com.example.morsecode.Adapters.OnLongClickListener
import com.example.morsecode.ChatActivity.Companion.USER_HASH
import com.example.morsecode.ChatActivity.Companion.sharedPreferencesFile
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.GetIdResponse
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactActivity : AppCompatActivity(), OnLongClickListener {

    private lateinit var kontakt: List<EntitetKontakt>
    private lateinit var accelerometer: Accelerometer
    private lateinit var handsFreeContact1: HandsFreeContact
    private lateinit var handsFree: HandsFree
    private var contactCounter = 0
    private var maxContactCounter: Int = 0

    private var handsFreeOnChat = false

    var mAccessibilityService: MorseCodeService? = null

    var userId: Int = 0
    var userLoginHash: String = ""

    lateinit var sharedPreferences: SharedPreferences
    lateinit var kontaktiRecyclerView: RecyclerView
    lateinit var kontaktiRecyclerViewAdapter: KontaktiAdapter

    lateinit var handsFreeIndicator: TextView

    lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        sharedPreferences = this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", 0)
        userLoginHash = sharedPreferences.getString(Constants.USER_HASH, "noHash").toString();
        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        handsFreeIndicator = findViewById(R.id.hands_free_indicator)
        kontaktiRecyclerView = findViewById(R.id.recycler)
        refreshContacts(userId, userLoginHash)

        accelerometer = Accelerometer(this)
        handsFreeContact1 = HandsFreeContact()
        handsFree = HandsFree()

        supportActionBar?.title = "Contacts"

        accelerometer.setListener { x, y, z, xG, yG, zG ->
            handsFreeContact1.follow(x, y, z, xG, yG, zG)
        }


        handsFreeContact1.setListener(object : HandsFreeContact.Listener {
            override fun onTranslation(tap: Int) {
                selectContact(tap)
            }
        })

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

                if (friendName != "") {
                    lifecycleScope.launch(Dispatchers.Default) {
                        try {
                            var friend: GetIdResponse

                            friend = ContactsApi.retrofitService.getUserByUsername(
                                userId,
                                userLoginHash, friendName
                            )

                            val friendId = friend?.id
                            var add = ContactsApi.retrofitService.addFriend(
                                userId,
                                userLoginHash, friendId
                            )
                            lifecycleScope.launch(Dispatchers.Main) {
                                refreshContacts(userId, userLoginHash)

                                Toast.makeText(
                                    applicationContext,
                                    friendName + "",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            //Toast.makeText(applicationContext, "There is no contact under that name", Toast.LENGTH_SHORT).show()
                        }
                    }


                } else {
                    Toast.makeText(applicationContext, "No name entered", Toast.LENGTH_SHORT).show()
                    fab.performClick()
                }

            }
            builder.show()
        }
    }

    fun refreshContacts(userId: Int, userHash: String) {
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
                    kontaktiRecyclerViewAdapter = KontaktiAdapter(context, kontakti, this@ContactActivity)
                    if(handsFreeOnChat) {
                        kontaktiRecyclerViewAdapter.selectContact(contactCounter)
                    }
                    kontaktiRecyclerView.adapter = kontaktiRecyclerViewAdapter
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
                    contactCounter = 0
                }
                vibrateName(contactCounter)
                kontaktiRecyclerViewAdapter.selectContact(contactCounter)
            }
            4 -> {
                if (contactCounter > 0) {
                    contactCounter--
                } else {
                    contactCounter = maxContactCounter
                }
                vibrateName(contactCounter)
                kontaktiRecyclerViewAdapter.selectContact(contactCounter)
            }
            1 -> {
                startContactChat(contactCounter)
            }
        }
    }

    private fun startContactChat(index: Int) {
        vibrator.vibrate(1)
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(Constants.USER_NAME, kontakt[index].username)
        intent.putExtra(Constants.USER_ID, kontakt[index].id!!.toInt())
        ContextCompat.startActivity(this, intent, null)
    }

    fun vibrateName(index: Int) {
        mAccessibilityService = MorseCodeService.getSharedInstance();
        mAccessibilityService?.vibrateWithPWM(mAccessibilityService!!.makeWaveformFromText(kontakt[index].username.toString()))
        /*Toast.makeText(
            applicationContext,
            "Current contact " + kontakt[index].username,
            Toast.LENGTH_LONG
        ).show()*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.contacts_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.hands_free -> {
                try {
                    vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

                    if (handsFreeOnChat) {
                        handsFreeOnChat = false
                        accelerometer.unregister()
                        handsFreeOnChatSet(false)
                        handsFreeIndicator.visibility = View.GONE
                        kontaktiRecyclerViewAdapter.selectContact(-1)
                    } else if(!handsFreeOnChat) {
                        handsFreeOnChat = true
                        turnHandsFreeOn()
                    }
                } catch (e: Exception) {
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun turnHandsFreeOn(){
        accelerometer.register()
        handsFreeOnChatSet(true)
        handsFreeIndicator.visibility = View.VISIBLE
        if(::kontaktiRecyclerViewAdapter.isInitialized) kontaktiRecyclerViewAdapter.selectContact(contactCounter)
    }

    override fun onResume() {
        Log.e("handsfree" , handsFreeOnChat.toString())

        if (handsFreeOnChat) {
            turnHandsFreeOn()
        }

        super.onResume()
    }

    private fun handsFreeOnChatSet(b: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("hands_free", b)
        editor.apply()
    }

    override fun onPause() {
        accelerometer.unregister()
        super.onPause()
    }

    override fun longHold(id: Int, username: String) {
        Log.d("ingo", "long hold ${id} ${username}")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete contact $username")
        builder.setNegativeButton("Close") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        builder.setPositiveButton("OK") { dialogInterface, i ->
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    var response = id?.let {
                        ContactsApi.retrofitService.removeFriend(
                            userId, userLoginHash.toString(),
                            it
                        )
                    }
                    withContext(Dispatchers.Main) {
                        refreshContacts(userId, userLoginHash.toString())
                    }

                } catch (e: Exception) {
                    Log.d("stjepan", "greska ")
                }
            }
        }
        builder.show()
    }


}