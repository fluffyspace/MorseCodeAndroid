package com.example.morsecode.baza

import androidx.room.*
import com.example.morsecode.models.Message

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message WHERE (receiverId = :receiverId AND senderId = :senderId) OR (senderId = :receiverId AND receiverId = :senderId)")
    fun getAllReceived(receiverId :Int, senderId: Int): List<Message>

    @Query("SELECT * FROM message WHERE receiverId = :senderId AND senderId = :senderId")
    fun getAllSender(senderId :Int): List<Message>

    @Insert
    fun insertAll(vararg polja: Message): List<Long>

    @Update
    fun update(polje: Message)

    @Delete
    fun delete(polje: Message)

    @Query("DELETE FROM message")
    fun deleteAll()
}