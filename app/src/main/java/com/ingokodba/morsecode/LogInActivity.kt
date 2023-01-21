package com.ingokodba.morsecode

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.ingokodba.morsecode.Constants.Companion.USER_HASH
import com.ingokodba.morsecode.Constants.Companion.USER_ID
import com.ingokodba.morsecode.Constants.Companion.USER_NAME
import com.ingokodba.morsecode.Constants.Companion.USER_PASSWORD
import com.ingokodba.morsecode.network.LogInResponse
import com.ingokodba.morsecode.network.RegisterResponse
import com.ingokodba.morsecode.network.getContactsApiService
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.ingokodba.morsecode.Constants.Companion.GOOGLE_LOGIN
import com.ingokodba.morsecode.Constants.Companion.GUEST_LOGIN
import com.ingokodba.morsecode.network.GoogleLoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class LogInActivity : AppCompatActivity() {

    private val sharedPreferencesFile = "MyPrefs"
    var sharedUsername: String = ""
    var sharedPassword: String = ""
    var sharedId: Int = -1
    var sharedHash: String = ""
    lateinit var sharedPreferences: SharedPreferences
    lateinit var loadingIcon: ProgressBar
    private var oneTapClient: SignInClient? = null
    private var signInRequest: BeginSignInRequest? = null
    val TAG = "googletag"
    val REQ_ONE_TAP = 156435
    private var showOneTapUI = true
    private var guestLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT

        supportActionBar?.hide()

        loadingIcon = findViewById(R.id.loadingIcon)



        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        intent.extras?.getBoolean("logout")?.let {
            if (it) {
                oneTapClient!!.signOut()
                sharedPreferences = getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()
            }
        }

        sharedPreferences =
            this.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE)

        sharedUsername = sharedPreferences.getString(USER_NAME, "").toString()
        sharedPassword = sharedPreferences.getString(USER_PASSWORD, "").toString()
        sharedId = sharedPreferences.getInt(USER_ID, -1)
        showOneTapUI = sharedPreferences.getBoolean(GOOGLE_LOGIN, true)
        guestLogin = sharedPreferences.getBoolean(GUEST_LOGIN, false)
        sharedHash = sharedPreferences.getString(USER_HASH, "").toString()

        if(guestLogin) loginAsGuest()

        findViewById<Button>(R.id.withoutLogin).setOnClickListener {
            loginAsGuest()
        }

        val userNameEditText = findViewById<EditText>(R.id.editTextName)
        val userPasswordEditTet = findViewById<EditText>(R.id.editTextPassword)

        if(sharedUsername != ""){
            userNameEditText.setText(sharedUsername)
        }

        if(sharedPassword != ""){
            userPasswordEditTet.setText(sharedPassword)
        }

        Log.d("stjepan", "id$sharedId")

        if (sharedHash != "") {
            Log.d("stjepann", "id $sharedId")
            loginWithHash()
            loadingIcon.visibility = View.VISIBLE
        }else{
            Log.e("stjepan", "no user")
            //if(showOneTapUI) beginGoogleSignIn()
        }

        findViewById<ConstraintLayout>(R.id.googleSignInButton).setOnClickListener {
            beginGoogleSignIn()
        }

        findViewById<Button>(R.id.logInButton).setOnClickListener {
            val userName: String = userNameEditText.text.toString()
            val userPassword: String = userPasswordEditTet.text.toString()
            val userPasswordHash = getMd5(userPassword)

            if (userName.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(applicationContext, "Username or password missing", Toast.LENGTH_SHORT).show()
            } else if (userName.isNotEmpty() || userPassword.isNotEmpty()) {
                loadingIcon.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        var user: LogInResponse = getContactsApiService(this@LogInActivity).logInUser(
                            userName, userPasswordHash, ""
                        )
                        Log.e("stjepan", "${user.hash} ${user.error}")
                        //Log.e("stjepan", "$userName $userPasswordHash")

                        if (user.success == true){
                            val editor = sharedPreferences.edit()
                            editor.putString(USER_NAME, userName)
                            editor.putString(USER_PASSWORD, userPassword)
                            user.id?.let { it1 -> editor.putInt(USER_ID, it1) }
                            editor.putString(USER_HASH, user.hash)
                            editor.apply()
                            editor.commit()

                            val intent = Intent(this@LogInActivity, MainActivity::class.java)
                            intent.putExtra("loggedInForFirstTime", true)
                            intent.putExtra("loggedIn", true)
                            startActivity(intent)
                        } else {
                            withContext(Dispatchers.Main){
                                Toast.makeText(applicationContext, "The username or password is incorrect", Toast.LENGTH_SHORT).show()
                                loadingIcon.visibility = View.GONE
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("stjepan", "greska logInUser " + e.stackTraceToString() + e.message.toString())
                        withContext(Dispatchers.Main){
                            Toast.makeText(applicationContext, "greska logInUser", Toast.LENGTH_SHORT).show()
                            loadingIcon.visibility = View.GONE
                        }
                    }
                }

            }
        }

        findViewById<Button>(R.id.registerButton).setOnClickListener {
            val userName: String = userNameEditText.text.toString()
            val userPassword: String = userPasswordEditTet.text.toString()
            if (userName.isNotEmpty() && userPassword.isNotEmpty()) {
                val userPasswordHash = getMd5(userPassword)
                loadingIcon.visibility = View.VISIBLE
                var passed = registerUser(userName, userPasswordHash)
                Log.e("Stjepan", "user registriran? $passed")
                return@setOnClickListener
            }
            Toast.makeText(this, "Username or password missing", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginAsGuest(){
        val editor = sharedPreferences.edit()
        editor.putBoolean(GUEST_LOGIN, true)
        editor.apply()
        editor.commit()

        val intent = Intent(this@LogInActivity, MainActivity::class.java)
        intent.putExtra("loggedInForFirstTime", true)
        intent.putExtra("loggedIn", false)
        startActivity(intent)
    }

    fun beginGoogleSignIn(){
        oneTapClient!!.beginSignIn(signInRequest!!)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient!!.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            lifecycleScope.launch(Dispatchers.Default) {
                                try {
                                    var user: GoogleLoginResponse = getContactsApiService(this@LogInActivity).googleCheckUser(idToken)
                                    Log.e("ingo", "google login -  ${user.hash} ${user.username} ${user.error}")
                                    if (user.success == true) {
                                        val editor = sharedPreferences.edit()
                                        editor.putString(USER_HASH, user.hash)
                                        editor.putString(USER_NAME, user.username)
                                        user.id?.let { it1 -> editor.putInt(USER_ID, it1) }
                                        editor.apply()

                                        val intent = Intent(this@LogInActivity, MainActivity::class.java)
                                        intent.putExtra("loggedInForFirstTime", true)
                                        intent.putExtra("loggedIn", true)
                                        startActivity(intent)
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                applicationContext,
                                                "Login hash has timed out",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            loadingIcon.visibility = View.GONE
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "stjepan",
                                        "greska googleCheckUser " + e.stackTraceToString() + e.message.toString()
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            applicationContext,
                                            "greska googleCheckUser",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        loadingIcon.visibility = View.GONE
                                    }
                                }
                            }
                            Log.d(TAG, "Got ID token from Google: $idToken")
                        }
                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                            val editor = sharedPreferences.edit()
                            editor.putBoolean(GOOGLE_LOGIN, false)
                            editor.apply()
                            editor.commit()
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }
                        else -> {
                            Log.d(TAG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})")
                        }
                    }
                }
            }
        }
    }

    fun loginWithHash(){
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val user: LogInResponse = getContactsApiService(this@LogInActivity).logInUser(
                    sharedUsername, "", sharedHash
                )
                Log.e("stjepan", "${user.hash} ${user.error}")
                Log.e("stjepan", "hash login - $sharedUsername $sharedHash")
                if (user.success == true) {
                    val editor = sharedPreferences.edit()
                    editor.putString(USER_HASH, user.hash)
                    editor.apply()
                    editor.commit()

                    val intent = Intent(this@LogInActivity, MainActivity::class.java)
                    intent.putExtra("loggedIn", true)
                    startActivity(intent)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "Login hash has timed out",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingIcon.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "stjepan",
                    "greska loginWithHash " + e.stackTraceToString() + e.message.toString()
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "greska loginWithHash",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadingIcon.visibility = View.GONE
                }
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
                    getContactsApiService(this@LogInActivity).registerContact(
                        name, hash)
                Log.d("ingo", passed.toString())
                flag = passed.success == true

                val editor = sharedPreferences.edit()
                editor.putString(USER_NAME, name)
                editor.putString(USER_PASSWORD, hash)
                editor.putInt(USER_ID, passed.id!!.toInt())
                editor.putString(USER_HASH, passed.hash)
                editor.apply()
                editor.commit()

                val intent = Intent(this@LogInActivity, MainActivity::class.java)
                intent.putExtra("loggedIn", true)
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("stjepan", "greska registerUser " + e.stackTraceToString() + e.message.toString())
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "greska registracija",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    loadingIcon.visibility = View.GONE
                }
            }
        }
        return flag
    }
}
