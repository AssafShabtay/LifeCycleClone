package com.yourcompany.lifecycleclone.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.time.TimeRangeFilter
import com.yourcompany.lifecycleclone.core.db.SleepSessionDao
import com.yourcompany.lifecycleclone.core.model.SleepSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing the Health Connect API and persisting its data into Room.  It
 * currently fetches sleep sessions and stores them in [SleepSessionDao].  Additional data
 * types such as steps or mindfulness can be added similarly.
 */
class HealthConnectRepository(
    private val context: Context,
    private val sleepSessionDao: SleepSessionDao
) {
    private val healthClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Loads sleep sessions for the specified time range and saves them into the database.  If
     * Health Connect is not available on the device or permission has not been granted, this
     * method will return an empty list.
     */
    suspend fun refreshSleepSessions(fromEpochMillis: Long, toEpochMillis: Long): List<SleepSessionEntity> = withContext(Dispatchers.IO) {
        try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    java.time.Instant.ofEpochMilli(fromEpochMillis),
                    java.time.Instant.ofEpochMilli(toEpochMillis)
                )
            )
            val response = healthClient.readRecords(request)
            val sessions = response.records.map { record ->
                SleepSessionEntity(
                    sessionId = record.metadata.id,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    qualityScore = null // SleepSessionRecord does not include quality; derive if needed
                )
            }
            sleepSessionDao.insertAll(sessions)
            sessions
        } catch (e: Exception) {
            // Health Connect may not be available or permissions might be missing.
            emptyList()
        }
    }
}