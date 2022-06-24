package com.example.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.ChatActivity.Companion.USER_HASH
import com.example.morsecode.ChatActivity.Companion.USER_ID
import com.example.morsecode.ChatActivity.Companion.USER_NAME
import com.example.morsecode.ChatActivity.Companion.USER_PASSWORD
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.LogInResponse
import com.example.morsecode.models.RegisterResponse
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.math.BigInteger
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LogInActivity : AppCompatActivity() {

    private val sharedPreferencesFile = "MyPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        val sharedName: String = sharedPreferences.getString("username", "").toString()
        val sharedPassword = sharedPreferences.getString("password", "").toString()
        val sharedId = sharedPreferences.getInt("id", 0)

        Log.e("stjepan", "id$sharedId")

        if (sharedName != "" && sharedPassword != "" && sharedId != 0) {
            Log.e("stjepann", "id$sharedId")
            val intent = Intent(this@LogInActivity, MainActivity::class.java)
            startActivity(intent)
        }else{
            Log.e("stjepan", "no user")
        }

        val userNameEditText = findViewById<EditText>(R.id.editTextName)
        val userPasswordEditTet = findViewById<EditText>(R.id.editTextPassword)

        findViewById<Button>(R.id.logInButton).setOnClickListener {
            val userName: String = userNameEditText.text.toString()
            val userPassword: String = userPasswordEditTet.text.toString()
            val userPasswordHash = getMd5(userPassword)

            if (userName.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
            } else if (userName.isNotEmpty() || userPassword.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        var user: LogInResponse = ContactsApi.retrofitService.logInUser(userName, userPasswordHash)

                        Log.e("stjepan", "${user.hash}")
                        Log.e("stjepan", "$userPasswordHash")

                        if (user.success == true){
                            val editor = sharedPreferences.edit()
                            editor.putString(USER_NAME, userName)
                            editor.putString(USER_PASSWORD, userPasswordHash)
                            editor.putInt(USER_ID, user.id.toInt())
                            editor.putString(USER_HASH, user.hash)
                            editor.apply()
                            editor.commit()

                            val intent = Intent(this@LogInActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(applicationContext,"Log in error",Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
                    }
                }

            }
        }

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            val intent = Intent(this@LogInActivity, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    private fun getMd5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            hashtext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }
}
