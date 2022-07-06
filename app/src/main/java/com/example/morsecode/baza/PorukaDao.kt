package com.example.morsecode.baza

import androidx.room.*
import com.example.morsecode.models.Contact
import com.example.morsecode.models.VibrationMessage

@Dao
interface PorukaDao {
    @Query("SELECT * FROM VibrationMessage")
    fun getAll(): List<VibrationMessage>

    @Insert
    fun insertAll(vararg polja: VibrationMessage): List<Long>

    @Update
    fun update(polje: VibrationMessage)

    @Delete
    fun delete(polje: VibrationMessage)

    @Query("DELETE FROM VibrationMessage")
    fun deleteAll()
}

@Dao
interface EntitetKontaktADao {
    @Query("SELECT * FROM contacts")
    fun getAll(): List<Contact>

    //@Query("SELECT * FROM contacts WHERE id ")
    //fun get(): List<EntitetKontakt>

    @Query("SELECT * FROM contacts WHERE id != :userId")
    fun getAllNoUser(userId: Int): List<Contact>

    @Insert
    fun insertAll(vararg polja: Contact): List<Long>

    @Update
    fun update(polje: Contact)

    @Delete
    fun delete(polje: Contact)

    @Query("DELETE FROM contacts")
    fun deleteAll()
}

