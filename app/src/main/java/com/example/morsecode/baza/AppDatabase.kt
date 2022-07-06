package com.example.morsecode.baza

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.morsecode.models.Contact
import com.example.morsecode.models.LegProfile
import com.example.morsecode.models.Message
import com.example.morsecode.models.VibrationMessage

@Database(entities = arrayOf(Message::class, VibrationMessage::class, Contact::class, LegProfile::class), version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun porukaDao(): PorukaDao
    abstract fun kontaktDao(): EntitetKontaktADao
    abstract fun messageDao(): MessageDao
    abstract fun legProfileDao(): LegProfileDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        fun replaceInstance(replace: AppDatabase){
            if(instance != null) {
                instance = replace
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "morsecode.db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }
}