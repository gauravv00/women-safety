package com.womensafety.sos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.data.local.dao.IncidentLogDao
import com.womensafety.sos.data.local.dao.TrustedContactDao

@Database(
    entities = [TrustedContact::class, IncidentLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun incidentLogDao(): IncidentLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "women_safety_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
