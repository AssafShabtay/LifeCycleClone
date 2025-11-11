package com.yourcompany.lifecycleclone.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.lifecycleclone.core.model.SleepSessionEntity

/**
 * DAO for storing sleep sessions imported from Health Connect. Sleep sessions have a unique
 * identifier provided by the health API; on conflict we replace the existing entry to ensure the
 * latest information is available.
 */
@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SleepSessionEntity>)

    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :from AND endTime <= :to")
    suspend fun getSessionsInRange(from: Long, to: Long): List<SleepSessionEntity>

    @Query("SELECT COUNT(*) FROM sleep_sessions WHERE NOT (:end <= startTime OR :start >= endTime)")
    suspend fun countOverlappingSessions(start: Long, end: Long): Int
}

