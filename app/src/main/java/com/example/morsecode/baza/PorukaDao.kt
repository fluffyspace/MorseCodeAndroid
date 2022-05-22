package com.example.morsecode.baza

import androidx.room.*
import com.example.morsecode.models.EntitetKontakt
import com.example.morsecode.models.Poruka

@Dao
interface PorukaDao {
    @Query("SELECT * FROM Poruka")
    fun getAll(): List<Poruka>

    @Insert
    fun insertAll(vararg polja: Poruka): List<Long>

    @Update
    fun update(polje: Poruka)

    @Delete
    fun delete(polje: Poruka)

    @Query("DELETE FROM Poruka")
    fun deleteAll()
}

@Dao
interface EntitetKontaktADao {
    @Query("SELECT * FROM contacts")
    fun getAll(): List<EntitetKontakt>

    @Insert
    fun insertAll(vararg polja: EntitetKontakt): List<Long>

    @Update
    fun update(polje: EntitetKontakt)

    @Delete
    fun delete(polje: EntitetKontakt)

    @Query("DELETE FROM contacts")
    fun deleteAll()
}