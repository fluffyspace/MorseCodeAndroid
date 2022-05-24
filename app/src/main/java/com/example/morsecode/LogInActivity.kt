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
import com.example.morsecode.models.EntitetKontakt
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

        val intent = Intent(this@LogInActivity, MainActivity::class.java)
        startActivity(intent)

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        val sharedName: String = sharedPreferences.getString("user_name", "").toString()
        val sharedPassword = sharedPreferences.getString("user_password", "").toString()

        if (sharedName.isNotEmpty() && sharedPassword.isNotEmpty()) {
            val intent = Intent(this@LogInActivity, MainActivity::class.java)
            startActivity(intent)
        }

        val userNameEditText = findViewById<EditText>(R.id.editTextName)
        val userPasswordEditTet = findViewById<EditText>(R.id.editTextPassword)

        val but = findViewById<Button>(R.id.logInButton)

        //1a1dc91c907325c69271ddf0c944bc72

        but.setOnClickListener {
            val userName: String = userNameEditText.text.toString()
            val userPassword: String = userPasswordEditTet.text.toString()

            if (userName.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
            } else if (userName.isNotEmpty() || userPassword.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        var user: RegisterResponse =
                            ContactsApi.retrofitService.getContact(userName)
                        var user1: List<EntitetKontakt> =
                            ContactsApi.retrofitService.getAllContacts()

                        Log.e("stjepan", "$user")

                        if (user.hash.toString() == getMd5(userPassword)) {
                            Log.e(
                                "stjepan",
                                "uspjesna loginizacija $userName name $userPassword pass"
                            )
                            //val intent = Intent(this@LogInActivity, MainActivity::class.java)
                            //startActivity(intent)
                        } else {
                            val i = getMd5(userPassword)
                            val j = user
                            Log.e("stjepan", "uspjesna loginizacija $i hash $j pass")
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
