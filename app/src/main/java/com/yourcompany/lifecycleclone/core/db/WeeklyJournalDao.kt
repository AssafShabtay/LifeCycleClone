package com.yourcompany.lifecycleclone.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.lifecycleclone.core.model.WeeklyJournalEntity

/**
 * DAO for managing cached weekly journal summaries.  The journal screen can quickly read the
 * summary for a given week start epoch day.  When new summaries are generated they replace
 * existing ones via REPLACE conflict strategy.
 */
@Dao
interface WeeklyJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: WeeklyJournalEntity)

    @Query("SELECT * FROM weekly_journal WHERE weekStartEpochDay = :weekStartEpochDay")
    suspend fun getJournalForWeek(weekStartEpochDay: Long): WeeklyJournalEntity?
}