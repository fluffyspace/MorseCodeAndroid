package com.ingokodba.morsecode.baza

import androidx.room.*
import com.ingokodba.morsecode.models.Contact
import com.ingokodba.morsecode.models.OpenedFile
import com.ingokodba.morsecode.models.VibrationMessage

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

@Dao
interface PreviouslyOpenedFilesDao {
    @Query("SELECT * FROM files")
    fun getAll(): List<OpenedFile>

    @Query("SELECT * FROM files WHERE id = :id")
    fun getById(id: Int): List<OpenedFile>

    @Query("SELECT * FROM files WHERE uri = :uri")
    fun getByUri(uri: String): List<OpenedFile>

    @Insert
    fun insertAll(vararg polja: OpenedFile): List<Long>

    @Update
    fun update(polje: OpenedFile)

    @Delete
    fun delete(polje: OpenedFile)

    @Query("DELETE FROM files")
    fun deleteAll()
}