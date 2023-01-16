package com.example.morsecode.baza

import androidx.room.*
import com.example.morsecode.models.LegProfile
import com.example.morsecode.models.Message

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message WHERE (receiverId = :receiverId AND senderId = :senderId) OR (senderId = :receiverId AND receiverId = :senderId)")
    fun getAllReceived(receiverId: Int, senderId: Int): List<Message>

    @Query("SELECT * FROM message WHERE (receiverId = :receiverId AND senderId = :senderId) OR (senderId = :receiverId AND receiverId = :senderId) ORDER BY id DESC LIMIT 1")
    fun getLastReceived(receiverId: Int, senderId: Int): Message?

    @Query("SELECT * FROM message WHERE receiverId = :senderId AND senderId = :senderId")
    fun getAllSender(senderId: Int): List<Message>

    @Insert
    fun insertAll(vararg polja: Message): List<Long>

    @Update
    fun update(polje: Message)

    @Delete
    fun delete(polje: Message)

    @Query("DELETE FROM message")
    fun deleteAll()
}

@Dao
interface LegProfileDao {
    @Query("SELECT * FROM legprofiles")
    fun getAll(): List<LegProfile>

    @Query("SELECT * FROM legprofiles WHERE id = :id")
    fun getById(id: Long): List<LegProfile>

    @Insert
    fun insertAll(vararg profile: LegProfile): List<Long>

    @Update
    fun update(profile: LegProfile)

    @Delete
    fun delete(profile: LegProfile): Int

    @Query("DELETE FROM legprofiles")
    fun deleteAll()
}