package com.ingokodba.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class RegisterResponse(
    @Json(name = "success") val success:Boolean?,
    @Json(name = "error") val error:String?,
    @Json(name = "hash") val hash:String?,
    @Json(name = "id") val id:String?,
    @Json(name = "password") val password:String?,
    @Json(name = "username") val username:String?,


)
