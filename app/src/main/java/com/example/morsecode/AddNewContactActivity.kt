package com.example.morsecode

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.morsecode.network.GetIdResponse
import com.example.morsecode.network.getContactsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddNewContactActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_contact)

        val editText = findViewById<EditText>(R.id.add_new_contact_edittext)
        findViewById<Button>(R.id.add_new_contact_button).setOnClickListener {
            val friendName = editText.text.toString()
            if (friendName != "") {
                val ctx = this
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        var friend: GetIdResponse = getContactsApiService(ctx).getUserByUsername(friendName)
                        val friendId = friend.id
                        var add = getContactsApiService(ctx).addFriend(friendId!!)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AddNewContactActivity,
                                friendName + " added as friend!",
                                Toast.LENGTH_SHORT
                            ).show()

                            val resultCode: Int = Activity.RESULT_OK;
                            val resultIntent: Intent = Intent();
                            setResult(resultCode, resultIntent);
                            finish();
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("ingo", e.toString())
                            Toast.makeText(
                                this@AddNewContactActivity,
                                "There is no contact under username $friendName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(applicationContext, "No username entered.", Toast.LENGTH_SHORT).show()
            }
        }

    }
}