package com.example.kiddoabc

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.kiddoabc.data.local.database.AppDatabase

class MyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d("MyApplication", "Initializing application...")

            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "kiddo_abc_db"
            )
                .fallbackToDestructiveMigration()
                .build()

            Log.d("MyApplication", "Database initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "Error initializing application", e)
            e.printStackTrace()
            // Ne pas throw ici, cela crashera l'app
        }
    }
}