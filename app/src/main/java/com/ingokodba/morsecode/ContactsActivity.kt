package com.ingokodba.morsecode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ingokodba.morsecode.Adapters.KontaktiAdapter
import com.ingokodba.morsecode.Adapters.OnLongClickListener
import com.ingokodba.morsecode.baza.AppDatabase
import com.ingokodba.morsecode.baza.MessageDao
import com.ingokodba.morsecode.models.Contact
import com.ingokodba.morsecode.models.Message
import com.ingokodba.morsecode.network.getContactsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsActivity : AppCompatActivity(), OnLongClickListener {

    private lateinit var kontakti: List<Contact>
    private lateinit var last_messages: MutableList<Message?>
    private lateinit var accelerometer: Accelerometer
    private lateinit var handsFreeContact1: HandsFreeContact
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

        sharedPreferences = this.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", 0)
        userLoginHash = sharedPreferences.getString(Constants.USER_HASH, "noHash").toString();
        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)

        handsFreeIndicator = findViewById(R.id.hands_free_indicator)
        kontaktiRecyclerView = findViewById(R.id.recycler)
        kontaktiRecyclerViewAdapter = KontaktiAdapter(this, listOf(), listOf(), userId, this)
        refreshContacts(userId)

        accelerometer = Accelerometer(this)
        handsFreeContact1 = HandsFreeContact()

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
            //buildUsernameDialog()
            val intent = Intent(this, AddNewContactActivity::class.java)
            startActivityForResult(intent, 46)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 46 && resultCode == Activity.RESULT_OK){
            refreshContacts(userId)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun refreshContacts(userId: Int) {
        kontaktiRecyclerView.layoutManager = LinearLayoutManager(this)
        val ctx = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                kontakti =
                    getContactsApiService(ctx).getMyFriends().filter { it.id != userId.toLong() }

                val db = AppDatabase.getInstance(this@ContactsActivity)
                val messageDao: MessageDao = db.messageDao()
                last_messages = mutableListOf()
                for(kontakt in kontakti){
                    if(kontakt.id != null) {
                        val poruka: Message? =
                            messageDao.getLastReceived(kontakt.id.toInt(), userId)
                        if(poruka != null) {
                            last_messages.add(poruka)
                        } else {
                            last_messages.add(null)
                        }
                    }
                }
                maxContactCounter = kontakti.size - 1

                //Log.e("max ", " $maxContactCounter")
                withContext(Dispatchers.Main) {
                    kontaktiRecyclerViewAdapter.contacts = this@ContactsActivity.kontakti
                    kontaktiRecyclerViewAdapter.messages = this@ContactsActivity.last_messages
                    if(handsFreeOnChat) {
                        kontaktiRecyclerViewAdapter.selectContact(contactCounter)
                    }
                    kontaktiRecyclerView.adapter = kontaktiRecyclerViewAdapter
                }
            } catch (e: Exception) {
                Log.d("stjepan", "contacts greska $e")
            }
        }
    }

    fun selectContact(command: Int) {
        when (command) {
            4 -> {
                if (contactCounter < maxContactCounter) {
                    contactCounter++
                } else {
                    contactCounter = 0
                }
                vibrateName(contactCounter)
                kontaktiRecyclerViewAdapter.selectContact(contactCounter)
            }
            3 -> {
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
        intent.putExtra(Constants.USER_NAME, kontakti[index].username)
        intent.putExtra(Constants.USER_ID, kontakti[index].id!!.toInt())
        ContextCompat.startActivity(this, intent, null)
    }

    fun vibrateName(index: Int) {
        mAccessibilityService = MorseCodeService.getSharedInstance();
        mAccessibilityService?.vibrateWithPWM(mAccessibilityService!!.makeWaveformFromText(kontakti[index].username.toString()))
        /*Toast.makeText(
            applicationContext,
            "Current contact " + kontakt[index].username,
            Toast.LENGTH_LONG
        ).show()*/
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.contacts_menu, menu)
        return true
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                        turnHandsFreeOff()
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
    }*/

    fun turnHandsFreeOff(){
        handsFreeOnChat = false
        accelerometer.unregister()
        handsFreeOnChatSet(false)
        handsFreeIndicator.visibility = View.GONE
        kontaktiRecyclerViewAdapter.selectContact(-1)
    }

    fun turnHandsFreeOn(){
        accelerometer.register()
        handsFreeOnChatSet(true)
        handsFreeIndicator.visibility = View.VISIBLE
        if(::kontaktiRecyclerViewAdapter.isInitialized) kontaktiRecyclerViewAdapter.selectContact(contactCounter)
    }

    override fun onResume() {
        handsFreeOnChat = sharedPreferences.getBoolean("hands_free", false)
        Log.e("handsfree" , handsFreeOnChat.toString())

        if (handsFreeOnChat) {
            turnHandsFreeOn()
        } else {
            turnHandsFreeOff()
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

    fun refreshAdapter(){
        kontaktiRecyclerViewAdapter.contacts = this@ContactsActivity.kontakti
        if (handsFreeOnChat) {
            kontaktiRecyclerViewAdapter.selectContact(contactCounter)
        }
        kontaktiRecyclerView.adapter = kontaktiRecyclerViewAdapter
    }

    override fun longHold(id: Int, username: String) {
        Log.d("ingo", "long hold ${id} ${username}")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unfriend $username?")
        builder.setNegativeButton("No") { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        builder.setPositiveButton("Yes") { dialogInterface, i ->
            val ctx = this
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    var response = id.let {
                        getContactsApiService(ctx).removeFriend(it.toLong())
                    }
                    withContext(Dispatchers.Main) {
                        maxContactCounter--
                        this@ContactsActivity.kontakti = this@ContactsActivity.kontakti.filter{ it.id != id.toLong()}
                        if (contactCounter >= maxContactCounter) {
                            contactCounter = 0
                            vibrateName(contactCounter)
                            kontaktiRecyclerViewAdapter.selectContact(contactCounter)
                        }
                        refreshAdapter()
                    }

                } catch (e: Exception) {
                    Log.d("stjepan", "greska removeFriend $e")
                }
            }
        }
        builder.show()
    }


}