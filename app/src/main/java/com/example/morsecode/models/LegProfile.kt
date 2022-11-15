package com.example.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "legprofiles")
data class LegProfile(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var downX:Float,
    var downY:Float,
    var downZ:Float,
    var upX:Float,
    var upY:Float,
    var upZ:Float,
    var name: String,
    var threshold: Float,
)

