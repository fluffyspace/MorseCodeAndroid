package com.example.morsecode

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.morsecode.models.EntitetKontakt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.withContext

class ContactActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        refreshContacts()
    }

    fun refreshContacts() {
        val kontaktiRecyclerView: RecyclerView = findViewById(R.id.recycler)
        kontaktiRecyclerView.layoutManager = LinearLayoutManager(this)
        val context = this
        lifecycleScope.launch(Dispatchers.Default){
            try {
                val kontakti: List<EntitetKontakt> = ContactsApi.retrofitService.getAllContacts()
                withContext(Dispatchers.Main){
                    kontaktiRecyclerView.adapter = KontaktiAdapter(context, kontakti)
                }
            }   catch (e: Exception) {
                Log.d("stjepan", "greska " )
            }
        }
    }
}