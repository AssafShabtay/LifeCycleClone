package com.yourcompany.lifecycleclone.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yourcompany.lifecycleclone.core.model.PlaceEntity
import com.yourcompany.lifecycleclone.core.model.VisitEntity
import com.yourcompany.lifecycleclone.core.model.VisitTagEntity
import com.yourcompany.lifecycleclone.core.model.SleepSessionEntity
import com.yourcompany.lifecycleclone.core.model.WeeklyJournalEntity

/**
 * The central Room database for the Life Cycle clone.  It defines all entities used in the
 * application and exposes DAOs for reading and writing them.  The database is a singleton and
 * should be injected where needed.
 */
@Database(
    entities = [
        PlaceEntity::class,
        VisitEntity::class,
        VisitTagEntity::class,
        SleepSessionEntity::class,
        WeeklyJournalEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
    abstract fun placeDao(): PlaceDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun weeklyJournalDao(): WeeklyJournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the database. If the database has not been created
         * previously it will be built using the supplied [context].  Note: this method will
         * migrate destructively if schema changes occur; adjust migration strategy as needed.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_cycle_clone.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}