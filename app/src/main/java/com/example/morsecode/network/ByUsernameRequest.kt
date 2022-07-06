package com.example.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class ByUsernameRequest(
    @Json(name = "username") val username:String,
)
