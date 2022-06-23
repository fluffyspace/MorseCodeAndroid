package com.example.morsecode

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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.KontaktiAdapter
import com.example.morsecode.ChatActivity.Companion.handsFreeOn
import com.example.morsecode.ChatActivity.Companion.sharedPreferencesFile
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactActivity : AppCompatActivity() {

    private var found = false
    private lateinit var kontakt: List<EntitetKontakt>
    private lateinit var accelerometer: Accelerometer
    private lateinit var handsFreeContact: HandsFreeContact
    private lateinit var handsFree: HandsFree
    private var contactCounter = 0
    private var maxContactCounter = 0

    private var start = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("id", 0)

        refreshContacts(userId)

        accelerometer = Accelerometer(this)
        handsFreeContact = HandsFreeContact()
        handsFree = HandsFree()

        accelerometer.setListener{ x, y, z ->
            supportActionBar?.title = x.toString()
            handsFreeContact.follow(x, z)
        }

        handsFreeContact.setListener(object : HandsFreeContact.Listener {
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
            val editText  = dialogLayout.findViewById<EditText>(R.id.editText)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { dialogInterface, i ->

                //val found = checkNewUser(editText.text.toString())
                Log.e("new User" , editText.text.toString())

                Toast.makeText(applicationContext, "EditText is $found", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }

        if (handsFreeOn){
            onResume()
        }
        //Log.e("kontakti " , " $kontakt")

    }
/*
    private fun checkNewUser(newUser: String): Boolean {
        found = false

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                kontakt = ContactsApi.retrofitService.getAllContacts()

                for (x in kontakt){
                    Log.e("finduser", " " + x.username)
                    if (x.username == newUser){
                        found = true
                        return@launch
                    }
                }

            } catch (e: Exception) {
                Log.d("stjepan", "greska ")
            }
        }
        return found
    }


 */
    fun refreshContacts(userId: Int) {
        val kontaktiRecyclerView: RecyclerView = findViewById(R.id.recycler)
        kontaktiRecyclerView.layoutManager = LinearLayoutManager(this)
        val context = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val kontakti: List<EntitetKontakt> = ContactsApi.retrofitService.getAllContacts()
                kontakt = kontakti
                maxContactCounter = kontakti.size-1
                Log.e("max ", " $maxContactCounter")
                withContext(Dispatchers.Main) {
                    kontaktiRecyclerView.adapter = KontaktiAdapter(context, kontakti)
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska ")
            }
        }
    }

    fun selectContact(command: Int){

        when(command){
            3 -> {
                if (contactCounter < maxContactCounter){
                    contactCounter++
                }else{
                    contactCounter = 1
                }
            }
            4 -> {
                if (contactCounter > 0){
                    contactCounter--
                }else{
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

    fun vibrateName(index: Int){
        //TODO vibrate contact name
        Toast.makeText(applicationContext, "vibrate Name " + kontakt[index].username + " id " + kontakt[index].id, Toast.LENGTH_LONG).show()
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