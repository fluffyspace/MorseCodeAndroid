package com.example.morsecode

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.Adapters.KontaktiAdapter
import com.example.morsecode.ChatActivity.Companion.sharedPreferencesFile
import com.example.morsecode.models.EntitetKontakt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.withContext

class ContactActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("id", 0)

        refreshContacts(userId)
    }

    fun refreshContacts(userId: Int) {
        val kontaktiRecyclerView: RecyclerView = findViewById(R.id.recycler)
        kontaktiRecyclerView.layoutManager = LinearLayoutManager(this)
        val context = this
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val kontakti: List<EntitetKontakt> = ContactsApi.retrofitService.getAllContacts()
                withContext(Dispatchers.Main) {
                    kontaktiRecyclerView.adapter = KontaktiAdapter(context, kontakti)
                }
            } catch (e: Exception) {
                Log.d("stjepan", "greska ")
            }
        }
    }
}