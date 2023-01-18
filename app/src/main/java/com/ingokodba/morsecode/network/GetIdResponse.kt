package com.ingokodba.morsecode.network

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json

@Entity(tableName = "contacts") //
data class GetIdResponse     // Konstruktor
    (
    @Json(name = "id") val id:Long?,
    @Json(name = "username") var username: String,
    @Json(name = "ip") var ip: String,
    @Json(name = "lastUpdated") var lastUpdated: String?,

)