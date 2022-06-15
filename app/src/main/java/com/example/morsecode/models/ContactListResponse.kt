package com.example.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class ContactListResponse(
    @Json(name = "id") val contactsId:Int,
)
