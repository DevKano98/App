package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AppRule
import com.example.data.model.LockSession
import com.example.data.model.Schedule
import com.example.data.model.WebsiteRule

@Database(
    entities = [
        AppRule::class,
        WebsiteRule::class,
        Schedule::class,
        LockSession::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FocusLockTypeConverters::class)
abstract class FocusLockDatabase : RoomDatabase() {

    abstract fun appRuleDao(): AppRuleDao
    abstract fun websiteRuleDao(): WebsiteRuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun lockSessionDao(): LockSessionDao

    companion object {
        @Volatile
        private var INSTANCE: FocusLockDatabase? = null

        fun getInstance(context: Context): FocusLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusLockDatabase::class.java,
                    "focus_lock_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
