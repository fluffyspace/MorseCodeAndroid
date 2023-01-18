package com.ingokodba.morsecode.models

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json

@Entity(tableName = "contacts") //
data class Contact     // Konstruktor
    (
    @PrimaryKey(autoGenerate = true) val id:Long?,
    @Json(name = "username") var username: String,
    @Json(name = "ip") var ip: String,
    @Json(name = "lastUpdated") var lastUpdated: String?,

)