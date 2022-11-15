package com.example.morsecode

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.network.LogInResponse
import com.example.morsecode.network.RegisterLoginRequest
import com.example.morsecode.network.getContactsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LogInActivity : AppCompatActivity() {

    private val sharedPreferencesFile = "MyPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        val intent = Intent(this, MorseCodeService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }

        val sharedPreferences: SharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        val sharedName: String = sharedPreferences.getString("username", "").toString()
        val sharedPassword = sharedPreferences.getString("password", "").toString()
        val sharedId = sharedPreferences.getInt("id", 0)

        Log.d("stjepan", "id$sharedId")

        if (sharedName != "" && sharedPassword != "" && sharedId != 0) {
            Log.d("stjepann", "id$sharedId")
            val editor = sharedPreferences.edit()
            editor.putBoolean(Constants.AUTO_LOGIN, true)
            editor.apply()
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
                Toast.makeText(applicationContext, "Username or password missing", Toast.LENGTH_SHORT).show()
            } else if (userName.isNotEmpty() || userPassword.isNotEmpty()) {
                val ctx = this
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        var user: LogInResponse = getContactsApiService(ctx).logInUser(
                            userName, userPasswordHash
                        )
                        Log.e("stjepan", "${user.hash} ${user.error}")
                        Log.e("stjepan", "$userName $userPasswordHash")

                        if (user.success == true){
                            val editor = sharedPreferences.edit()
                            editor.putString(Constants.USER_NAME, userName)
                            editor.putString(Constants.USER_PASSWORD, userPasswordHash)
                            user.id?.let { it1 -> editor.putInt(Constants.USER_ID, it1) }
                            editor.putString(Constants.USER_HASH, user.hash)
                            editor.apply()
                            editor.commit()

                            val intent = Intent(this@LogInActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            withContext(Dispatchers.Main){
                                Toast.makeText(applicationContext, "The username or password is incorrect", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("stjepan", "greska logInUser " + e.stackTraceToString() + e.message.toString())
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
