package com.example.morsecode.moodel

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class Zadatak(
    @Json(name = "zadatak") val zadatak:String?,
    @Json(name = "pitanje") val pitanje:String?,
    @Json(name = "odgovor") val odgovor:String?,
)
