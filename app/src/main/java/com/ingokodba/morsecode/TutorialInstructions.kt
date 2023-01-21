package com.ingokodba.morsecode

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class TutorialInstructions : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_instructions)
        findViewById<Button>(R.id.close).setOnClickListener {
            finish()
        }
    }
}