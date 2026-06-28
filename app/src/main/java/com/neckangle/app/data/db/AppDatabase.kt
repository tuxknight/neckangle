package com.neckangle.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neckangle.app.data.model.AppSettings
import com.neckangle.app.data.model.PostureRecord

@Database(
    entities = [PostureRecord::class, AppSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postureRecordDao(): PostureRecordDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neckangle.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
