package com.yourcompany.lifecycleclone.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representation of a sleep session imported from Health Connect or another health data provider.
 * Each session stores start/end times and optionally a quality score if provided by the source.
 */
@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val qualityScore: Int? = null
)