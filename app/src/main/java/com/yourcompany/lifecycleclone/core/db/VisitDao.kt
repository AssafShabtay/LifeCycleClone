package com.yourcompany.lifecycleclone.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yourcompany.lifecycleclone.core.model.VisitEntity

/**
 * Data Access Object for reading and writing visit information. The DAO defines suspend
 * functions for inserting new visits, closing visits, and querying visits within a time range.
 */
@Dao
interface VisitDao {
    @Insert
    suspend fun insertVisit(visit: VisitEntity): Long

    @Query(
        """
        UPDATE visits SET endTime = :endTime
        WHERE visitId = :id AND endTime IS NULL
        """
    )
    suspend fun endVisit(id: Long, endTime: Long)

    @Query("DELETE FROM visits WHERE visitId = :id")
    suspend fun deleteVisit(id: Long)

    @Query(
        """
        SELECT v.visitId, v.placeOwnerId, v.startTime, v.endTime, v.confidence,
               p.label AS placeLabel, p.category AS placeCategory, p.colorArgb AS placeColor
        FROM visits v
        INNER JOIN places p ON v.placeOwnerId = p.placeId
        WHERE v.startTime <= :to AND (v.endTime >= :from OR v.endTime IS NULL)
        ORDER BY v.startTime ASC
        """
    )
    suspend fun getVisitsInRange(from: Long, to: Long): List<VisitWithPlace>

    /**
     * Returns the most recent visit (including its associated place) that ended on or before
     * [time]. This is useful for correlating activities immediately preceding an event such as
     * sleep. If no visit has ended before the given time, `null` is returned.
     */
    @Query(
        """
        SELECT v.visitId, v.placeOwnerId, v.startTime, v.endTime, v.confidence,
               p.label AS placeLabel, p.category AS placeCategory, p.colorArgb AS placeColor
        FROM visits v
        INNER JOIN places p ON v.placeOwnerId = p.placeId
        WHERE v.endTime IS NOT NULL AND v.endTime <= :time
        ORDER BY v.endTime DESC
        LIMIT 1
        """
    )
    suspend fun getLastVisitBefore(time: Long): VisitWithPlace?
}

/**
 * Projection class combining a [VisitEntity] with associated [PlaceEntity] fields. Used primarily
 * by timeline queries.
 */
data class VisitWithPlace(
    val visitId: Long,
    val placeOwnerId: Long,
    val startTime: Long,
    val endTime: Long?,
    val confidence: Int,
    val placeLabel: String,
    val placeCategory: String,
    val placeColor: Long
)
