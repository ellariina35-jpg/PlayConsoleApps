package com.example

import android.app.Application
import com.example.data.local.WorkdayDatabase
import com.example.data.repository.WorkdayRepository
import com.example.data.sync.GoogleCalendarSyncManager

class WorkdayApp : Application() {
    val database by lazy { WorkdayDatabase.getDatabase(this) }
    val syncManager by lazy { GoogleCalendarSyncManager(this) }
    val repository by lazy { WorkdayRepository(database.workdayDao(), syncManager, this) }

    override fun onCreate() {
        super.onCreate()
    }
}
