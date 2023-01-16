package com.example.morsecode

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class AwardForWins : AppCompatActivity() {
    lateinit var close_button: Button
    lateinit var big_award_text:TextView
    lateinit var small_award_text:TextView
    lateinit var award_image:ImageView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_award)
        close_button = findViewById(R.id.close)
        big_award_text = findViewById(R.id.big_award_text)
        small_award_text = findViewById(R.id.small_award_text)
        award_image = findViewById(R.id.award_image)

        close_button.setOnClickListener {
            this.finish()
        }

        big_award_text.text = intent.getStringExtra("bigText")
        small_award_text.text = intent.getStringExtra("smallText")
        //award_image.setImageResource(intent.getIntExtra("award_image_id", R.drawable.trophy))
        award_image.setImageResource(R.drawable.trophy)

    }

}