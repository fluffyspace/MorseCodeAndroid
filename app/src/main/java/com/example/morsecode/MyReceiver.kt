package com.example.morsecode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        //TODO("MyReceiver.onReceive() is not implemented")
        Log.d("ingo", "onreceive")
        val intentAction = intent.action
        var event: KeyEvent? = null
        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            event = intent
                .getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent?
        }
        if (event == null) {
            return
        }
        val keycode: Int = event.getKeyCode()
        val action: Int = event.getAction()
        val eventtime: Long = event.getEventTime()

        if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keycode == KeyEvent.KEYCODE_HEADSETHOOK || keycode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                // Start your app here!

                // ...
                Log.e("event/////", "Trigerd")
                if (isOrderedBroadcast) {
                    abortBroadcast()
                }
            }
        }
    }
}