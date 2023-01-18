package com.ingokodba.morsecode.network

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json

data class ContactIdRequest(
    @Json(name = "contactId") val id: Long,
)