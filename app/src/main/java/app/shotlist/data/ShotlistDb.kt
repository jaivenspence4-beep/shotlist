package app.shotlist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Shot::class, Finding::class], version = 1, exportSchema = false)
abstract class ShotlistDb : RoomDatabase() {
    abstract fun shots(): ShotDao
    abstract fun findings(): FindingDao

    companion object {
        @Volatile private var instance: ShotlistDb? = null

        fun get(context: Context): ShotlistDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShotlistDb::class.java,
                    "shotlist.db",
                ).build().also { instance = it }
            }
    }
}
