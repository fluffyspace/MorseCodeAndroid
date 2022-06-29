package com.example.morsecode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val service = Intent(context, MorseCodeService::class.java)
        Log.d("ingo", "StopServiceReceiver")
        context.stopService(service)
    }

    companion object {
        const val REQUEST_CODE = 333
    }
}