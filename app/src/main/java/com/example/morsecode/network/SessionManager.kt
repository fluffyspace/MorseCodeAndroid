package com.example.morsecode.network

import android.content.Context
import android.content.SharedPreferences
import com.example.morsecode.Constants

/**
 * Session manager to save and fetch data from SharedPreferences
 */
class SessionManager (context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(Constants.sharedPreferencesFile, Context.MODE_PRIVATE)

    /**
     * Function to save auth token
     */
    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(Constants.USER_HASH, token)
        editor.apply()
    }

    /**
     * Function to fetch auth token
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(Constants.USER_HASH, null)
    }
}