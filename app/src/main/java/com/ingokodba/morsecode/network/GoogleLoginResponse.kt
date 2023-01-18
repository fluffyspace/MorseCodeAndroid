package com.ingokodba.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class GoogleLoginResponse(
    @Json(name = "success") val success:Boolean?,
    @Json(name = "hash") val hash:String?,
    @Json(name = "username") val username:String?,
    @Json(name = "id") val id:Int?,
    @Json(name = "error") val error:String?,

)
