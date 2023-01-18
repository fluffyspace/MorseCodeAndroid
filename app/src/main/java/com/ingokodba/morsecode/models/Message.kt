package com.ingokodba.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import java.sql.Timestamp

@Entity(tableName = "message")
data class Message(
    @PrimaryKey val id: Long,
    @Json(name = "message") val message:String,
    @Json(name = "receiverId") val receiverId:Int,
    @Json(name = "senderId") val senderId:Int,
    @Json(name = "sentAt") val sentAt: String,
    @Json(name = "seen") val seen:Boolean,
    @Json(name = "received") val received:Boolean,
){
    //constructor(message:String, receiverId:Integer, senderId:Integer) : this(0,message,receiverId,senderId)

}

