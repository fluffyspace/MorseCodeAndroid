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
import com.example.morsecode.models.RegisterResponse
import com.example.morsecode.network.ContactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
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

        if (sharedName.isNotEmpty() && sharedPassword.isNotEmpty()){
            val intent = Intent(this@LogInActivity, MainActivity::class.java)
            startActivity(intent)
        }

        val userNameEditText = findViewById<EditText>(R.id.editTextName)
        val userPasswordEditTet = findViewById<EditText>(R.id.editTextPassword)

        val but = findViewById<Button>(R.id.logInButton)

        but.setOnClickListener{
            val userName:String = userNameEditText.text.toString()
            val userPassword:String = userPasswordEditTet.text.toString()

            if (userName.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(applicationContext,"Error",Toast.LENGTH_LONG).show()
            }else if (userName.isNotEmpty() || userPassword.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Default){
                    try {
                        var user: RegisterResponse = ContactsApi.retrofitService.getContact(userName)

                        if (user.hash == userPassword){
                            val intent = Intent(this@LogInActivity, MainActivity::class.java)
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("stjepan", "greska " + e.stackTraceToString() + e.message.toString())
                    }
                }

            }
        }



            /*
        if (sharedName.isEmpty()) {
        }



        val text = findViewById<TextView>(R.id.textView)
        val userNameEditText = findViewById<EditText>(R.id.editTextName)
        val userPasswordEditTet = findViewById<EditText>(R.id.editTextPassword)

        but.setOnClickListener {

            val userName = userNameEditText.text
            val userPassword = userPasswordEditTet.text

            if (userName.isEmpty() && userPassword.isEmpty()) {



                //text.setText(" " + ip)

                val editor:SharedPreferences.Editor =  sharedPreferences.edit()
                editor.putString("user_name", userName.toString())
                editor.putString("user_password", userPassword.toString())
                editor.apply()
                editor.commit()



                lifecycleScope.launch(Dispatchers.Default){

                    //var user = ContactsApi.retrofitService.getContact("ingokodba")
                    //Log.d("ingo", user.toString())
                    //withContext(Dispatchers.Main){
                    //    userTextView.setText(" " + user.ip);
                    //}




                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)


            }


        }


 */


    }
}


/*
    fun checkAvailableConnection() {
        val connMgr = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val mobile = connMgr
            .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        if (wifi!!.isAvailable) {
            val myWifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
            val myWifiInfo = myWifiManager.connectionInfo
            val ipAddress = myWifiInfo.ipAddress
            println(
                "WiFi address is "
                        + formatIpAddress(ipAddress)
            )
        } else if (mobile!!.isAvailable) {
            GetLocalIpAddress()
            Toast.makeText(this, "3G Available", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "No Network Available", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun GetLocalIpAddress(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface
                .getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf
                    .getInetAddresses()
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            return "ERROR Obtaining IP"
        }
        return "No IP Available"
    }



 */
/*
        lifecycleScope.launch(Dispatchers.Default) {


            var user = ContactsApi.retrofitService.getContact("ingokodba")
            Log.d("ingo", user.toString())
            withContext(Dispatchers.Main) {
                userTextView.setText(" " + user.ip);
            }
        }

 */