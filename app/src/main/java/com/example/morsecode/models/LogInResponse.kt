package com.example.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class LogInResponse(
    @Json(name = "success") val success:Boolean?,
    @Json(name = "hash") val hash:String?,
    @Json(name = "id") val id:Integer,
    @Json(name = "error") val error:String?,

)
