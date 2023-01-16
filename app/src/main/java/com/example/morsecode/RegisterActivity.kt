package com.example.morsecode

import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.network.RegisterLoginRequest
import com.example.morsecode.network.RegisterResponse
import com.example.morsecode.network.getContactsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class RegisterActivity : AppCompatActivity() {

    val MyPREFERENCES = "MyPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        var logIn: Button = findViewById(R.id.logInButton)
        var registerButton: Button = findViewById(R.id.registerButton)
        var userNameText: EditText = findViewById(R.id.editTextName)
        var userPasswordText: EditText = findViewById(R.id.editTextPassword)

        logIn.isClickable = false
        logIn.visibility = View.GONE

        registerButton.setOnClickListener {
            val username = userNameText.text.toString()
            val pass = userPasswordText.text.toString()
            Log.e("Stjepan", "password - $pass");
            val MD5pass = getMd5(pass)
            Log.e("stjepan", "hash - $MD5pass");

            if (userNameText.text.isNotEmpty()) {
                var passed = registerUser(username, MD5pass)
                Log.e("Stjepan", "user registriran? $passed")
            }

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

    private fun registerUser(name: String, hash: String): Boolean {
        var flag = false
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val passed: RegisterResponse =
                    getContactsApiService(this@RegisterActivity).registerContact(
                        name, hash)
                flag = passed.success == true

                val sharedPreferences: SharedPreferences =
                    getSharedPreferences(MyPREFERENCES, MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.putString("username", name)
                editor.putString("password", hash)
                editor.apply()
                editor.commit()

            } catch (e: Exception) {
                Log.e("stjepan", "greska registerUser " + e.stackTraceToString() + e.message.toString())
            }
        }
        return flag
    }
}