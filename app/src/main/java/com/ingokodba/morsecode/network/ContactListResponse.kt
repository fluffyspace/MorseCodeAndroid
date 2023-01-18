package com.ingokodba.morsecode.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

data class ContactListResponse(
    @Json(name = "id") val contactsId:Int,
)
