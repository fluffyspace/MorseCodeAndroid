package com.example.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class SendMessageRequest(
    @Json(name = "to") val to:Int,
    @Json(name = "message") val message:String,
)
