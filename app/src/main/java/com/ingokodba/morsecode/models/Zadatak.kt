package com.ingokodba.morsecode.models

import androidx.room.Entity
import com.squareup.moshi.Json

@Entity
data class Zadatak(
    @Json(name = "zadatak") val zadatak:String?,
    @Json(name = "pitanje") val pitanje:String?,
    @Json(name = "odgovor") val odgovor:String?,
)
