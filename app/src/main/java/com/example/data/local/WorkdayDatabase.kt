package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.WorkdayEntity

@Database(entities = [WorkdayEntity::class], version = 1, exportSchema = false)
abstract class WorkdayDatabase : RoomDatabase() {
    abstract fun workdayDao(): WorkdayDao

    companion object {
        @Volatile
        private var INSTANCE: WorkdayDatabase? = null

        fun getDatabase(context: Context): WorkdayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkdayDatabase::class.java,
                    "workday_database"
                )
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
