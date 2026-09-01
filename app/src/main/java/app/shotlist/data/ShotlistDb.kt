package app.shotlist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Shot::class, Finding::class, ShotFts::class,
        CycleEntry::class, Habit::class, HabitTick::class, Scan::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ShotlistDb : RoomDatabase() {
    abstract fun shots(): ShotDao
    abstract fun findings(): FindingDao
    abstract fun cycle(): CycleDao
    abstract fun habits(): HabitDao
    abstract fun scans(): ScanDao

    companion object {
        @Volatile private var instance: ShotlistDb? = null

        /** v1 phones (build-14 and earlier) must upgrade without data loss. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE findings ADD COLUMN vaulted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cycle_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "day INTEGER NOT NULL, flow TEXT NOT NULL, " +
                        "symptoms TEXT NOT NULL, notes TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_cycle_entries_day " +
                        "ON cycle_entries(day)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS habits (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, emoji TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, archived INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS habit_ticks (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "habitId INTEGER NOT NULL, day INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_ticks_habitId_day " +
                        "ON habit_ticks(habitId, day)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS scans (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "shotId INTEGER NOT NULL, mode TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }
        }

        /**
         * External-content FTS does not copy historical rows merely because its
         * virtual table exists. The rebuild command deterministically indexes
         * every already-processed screenshot; Room recreates sync triggers after
         * migrations complete.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `shots_fts` USING FTS4(" +
                        "`ocrText` TEXT NOT NULL, tokenize=unicode61, content=`shots`)"
                )
                db.execSQL("INSERT INTO `shots_fts`(`shots_fts`) VALUES('rebuild')")
            }
        }

        fun get(context: Context): ShotlistDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShotlistDb::class.java,
                    "shotlist.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
