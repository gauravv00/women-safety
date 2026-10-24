package com.womensafety.sos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.womensafety.sos.data.entity.IncidentLog
import com.womensafety.sos.data.entity.PairedWard
import com.womensafety.sos.data.entity.TrustedContact
import com.womensafety.sos.data.local.dao.IncidentLogDao
import com.womensafety.sos.data.local.dao.PairedWardDao
import com.womensafety.sos.data.local.dao.TrustedContactDao

@Database(
    entities = [TrustedContact::class, IncidentLog::class, PairedWard::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun incidentLogDao(): IncidentLogDao
    abstract fun pairedWardDao(): PairedWardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `paired_wards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `wardPairingCode` TEXT NOT NULL,
                        `wardName` TEXT NOT NULL,
                        `wardUid` TEXT NOT NULL DEFAULT '',
                        `pairedAt` INTEGER NOT NULL,
                        `lastKnownLat` REAL NOT NULL,
                        `lastKnownLng` REAL NOT NULL,
                        `lastStatus` TEXT NOT NULL,
                        `lastActiveTimestamp` INTEGER NOT NULL,
                        `activeIncidentId` INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `paired_wards` ADD COLUMN `wardUid` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "women_safety_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
