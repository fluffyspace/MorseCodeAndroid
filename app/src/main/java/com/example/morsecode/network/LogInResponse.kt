package com.example.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class LogInResponse(
    @Json(name = "success") val success:Boolean?,
    @Json(name = "hash") val hash:String?,
    @Json(name = "id") val id:Int?,
    @Json(name = "error") val error:String?,

)
