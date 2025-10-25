package com.yourcompany.lifecycleclone.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached summary of a week's activities and selected photos.  The app generates these on a
 * periodic basis to allow the journal screen to load instantly without recomputing every time.
 */
@Entity(tableName = "weekly_journal")
data class WeeklyJournalEntity(
    @PrimaryKey val weekStartEpochDay: Long,
    val summaryJson: String,
    val photoUrisJson: String
)