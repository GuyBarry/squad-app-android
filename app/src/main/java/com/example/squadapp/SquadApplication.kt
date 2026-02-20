package com.example.squadapp

import android.app.Application
import com.example.squadapp.database.AppDatabase

class SquadApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    companion object {
        lateinit var instance: SquadApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

