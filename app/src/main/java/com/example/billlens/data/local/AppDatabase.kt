package com.example.billlens.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.billlens.data.model.Expense
import com.example.billlens.utils.Converters
import com.example.billlens.data.model.UserEntity

@Database(entities = [Expense::class, UserEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase(){
    abstract fun expenseDao(): ExpenseDao
    abstract fun userDao(): UserDao

    companion object {
        // La keyword 'volatile' assicura che il valore di INSTANCE sia sempre aggiornato
        // e visibile a tutti i thread.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Usa l'operatore elvis per ritornare l'istanza se esiste,
            // altrimenti la crea in un blocco synchronized per evitare race conditions.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bill_lens_database"
                )
                    .fallbackToDestructiveMigration() // Semplice strategia di migrazione per ora
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

}