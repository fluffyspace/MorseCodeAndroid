package com.ingokodba.morsecode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class PlaygroundInstructions : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground_instructions)
        findViewById<Button>(R.id.close).setOnClickListener {
            finish()
        }
    }
}