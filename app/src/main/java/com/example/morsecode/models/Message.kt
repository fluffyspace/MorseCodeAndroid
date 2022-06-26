package com.example.morsecode.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "message")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @Json(name = "message") val message:String?,
    @Json(name = "receiverId") val receiverId:Integer?,
    @Json(name = "senderId") val senderId:Integer?,
    //@Json(name = "sentAT") val sentAt:Timestamp?,
    //@Json(name = "seen") val seen:Boolean?,
    //@Json(name = "received") val received:Boolean?,
){
    //constructor(message:String, receiverId:Integer, senderId:Integer) : this(0,message,receiverId,senderId)

}

