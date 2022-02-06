package com.example.morsecode.moodel

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class Poruka(
    @PrimaryKey(autoGenerate = true) val id:Long?,
    @Json(name = "poruka") val poruka:String?,
    @Json(name = "vibrate") val vibrate:String?,
)
