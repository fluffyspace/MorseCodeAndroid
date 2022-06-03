package com.example.morsecode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val userName = intent.getStringExtra("username").toString()
        val id = intent.getStringExtra("id").toString()

        supportActionBar?.title = "$userName id $id"
    }
}