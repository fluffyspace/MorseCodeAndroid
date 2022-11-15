package com.example.morsecode.models

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.squareup.moshi.Json

@Entity(tableName = "files")
data class OpenedFile
    (
    @PrimaryKey(autoGenerate = true) val id:Long,
    @Json(name = "uri") var uri: String,
    @Json(name = "filename") var filename: String,
    @Json(name = "lastOpened") var lastOpened: String?,
)