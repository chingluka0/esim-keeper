package com.baohao.esimkeeper.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ESimCard::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eSimCardDao(): ESimCardDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "esim_keeper.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN reminderDaysBefore INTEGER")
            }
        }

        // v3: add the embedded per-card "number charges" (tariff_*) columns.
        // Existing rows default to empty strings so no data is lost.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN tariff_outgoingCall TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN tariff_incomingCall TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN tariff_outgoingSms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN tariff_incomingSms TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE esim_cards ADD COLUMN tariff_dataTraffic TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
