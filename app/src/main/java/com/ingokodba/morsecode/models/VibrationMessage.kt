package com.ingokodba.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class VibrationMessage(
    @PrimaryKey(autoGenerate = true) val id:Long?,
    @Json(name = "poruka") val poruka:String?,
    @Json(name = "vibrate") val vibrate:String?,
)