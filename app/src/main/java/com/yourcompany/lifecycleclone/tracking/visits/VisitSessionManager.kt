package com.yourcompany.lifecycleclone.tracking.visits

import android.location.Location
import com.yourcompany.lifecycleclone.core.db.PlaceDao
import com.yourcompany.lifecycleclone.core.db.VisitDao
import com.yourcompany.lifecycleclone.core.model.PlaceEntity
import com.yourcompany.lifecycleclone.core.model.VisitEntity
import com.yourcompany.lifecycleclone.tracking.service.SleepDetectionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import kotlin.math.abs

/**
 * Coordinates ongoing visit sessions and writes them to the database. The manager keeps track of
 * the currently active visit (if any) and exposes helpers to start still visits, movement sessions
 * and geofence-based visits. When a still visit finishes it forwards the summary to
 * [SleepDetectionManager] so potential sleep can be captured.
 */
class VisitSessionManager(
    private val visitDao: VisitDao,
    private val placeDao: PlaceDao,
    private val sleepDetectionManager: SleepDetectionManager
) {

    enum class SessionType {
        STILL,
        MOVEMENT,
        GEOFENCE;

        val isStillLike: Boolean
            get() = this == STILL || this == GEOFENCE
    }

    data class VisitSummary(
        val visitId: Long,
        val placeId: Long,
        val category: String,
        val type: SessionType,
        val startTime: Long,
        val endTime: Long?,
        val anchorLatitude: Double?,
        val anchorLongitude: Double?
    ) {
        val isStillLike: Boolean get() = type.isStillLike
    }

    private data class ActiveVisitState(
        val visitId: Long,
        val placeId: Long,
        val type: SessionType,
        val placeCategory: String,
        val startTime: Long,
        var anchorLat: Double? = null,
        var anchorLon: Double? = null
    )

    private val mutex = Mutex()
    private var activeState: ActiveVisitState? = null

    suspend fun beginStillVisit(location: Location, startTime: Long) {
        val place = resolvePlaceForLocation(location)
        mutex.withLock {
            val visitId = visitDao.insertVisit(
                VisitEntity(
                    placeOwnerId = place.placeId,
                    startTime = startTime,
                    confidence = 95
                )
            )
            activeState = ActiveVisitState(
                visitId = visitId,
                placeId = place.placeId,
                type = SessionType.STILL,
                placeCategory = place.category,
                startTime = startTime,
                anchorLat = location.latitude,
                anchorLon = location.longitude
            )
        }
    }

    suspend fun beginMovementVisit(activityLabel: String, startTime: Long) {
        val categoryKey = "movement:${activityLabel.lowercase(Locale.US)}"
        val place = placeDao.getPlaceByCategory(categoryKey) ?: run {
            val label = "Trip • ${activityLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }}"
            val entity = PlaceEntity(
                label = label,
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 0f,
                category = categoryKey,
                colorArgb = colorForKey(categoryKey)
            )
            val id = placeDao.insert(entity)
            entity.copy(placeId = id)
        }
        mutex.withLock {
            val visitId = visitDao.insertVisit(
                VisitEntity(
                    placeOwnerId = place.placeId,
                    startTime = startTime,
                    confidence = 80
                )
            )
            activeState = ActiveVisitState(
                visitId = visitId,
                placeId = place.placeId,
                type = SessionType.MOVEMENT,
                placeCategory = place.category,
                startTime = startTime
            )
        }
    }

    suspend fun startVisitForPlace(placeId: Long, startTime: Long): VisitSummary? {
        val place = placeDao.getById(placeId) ?: return null
        val ended = endCurrentVisit(startTime)
        mutex.withLock {
            val visitId = visitDao.insertVisit(
                VisitEntity(
                    placeOwnerId = place.placeId,
                    startTime = startTime,
                    confidence = 100
                )
            )
            activeState = ActiveVisitState(
                visitId = visitId,
                placeId = place.placeId,
                type = SessionType.GEOFENCE,
                placeCategory = place.category,
                startTime = startTime
            )
        }
        return ended
    }

    suspend fun updateStillAnchor(location: Location) {
        mutex.withLock {
            val state = activeState ?: return
            if (state.type.isStillLike) {
                state.anchorLat = location.latitude
                state.anchorLon = location.longitude
            }
        }
    }

    suspend fun endCurrentVisit(endTime: Long): VisitSummary? {
        val summary = mutex.withLock {
            val state = activeState ?: return@withLock null
            visitDao.endVisit(state.visitId, endTime)
            activeState = null
            VisitSummary(
                visitId = state.visitId,
                placeId = state.placeId,
                category = state.placeCategory,
                type = state.type,
                startTime = state.startTime,
                endTime = endTime,
                anchorLatitude = state.anchorLat,
                anchorLongitude = state.anchorLon
            )
        }
        if (summary != null && summary.type.isStillLike) {
            sleepDetectionManager.onStillVisitCompleted(summary)
        }
        return summary
    }

    suspend fun recordStillVisit(
        location: Location,
        startTime: Long,
        endTime: Long,
        confidence: Int = 80
    ): VisitSummary? {
        val place = resolvePlaceForLocation(location)
        val summary = mutex.withLock {
            val visitId = visitDao.insertVisit(
                VisitEntity(
                    placeOwnerId = place.placeId,
                    startTime = startTime,
                    confidence = confidence
                )
            )
            visitDao.endVisit(visitId, endTime)
            VisitSummary(
                visitId = visitId,
                placeId = place.placeId,
                category = place.category,
                type = SessionType.STILL,
                startTime = startTime,
                endTime = endTime,
                anchorLatitude = location.latitude,
                anchorLongitude = location.longitude
            )
        }
        summary?.let { sleepDetectionManager.onStillVisitCompleted(it) }
        return summary
    }

    suspend fun reclassifyMovementAsStill(
        movement: VisitSummary,
        location: Location?
    ): VisitSummary? {
        if (movement.type != SessionType.MOVEMENT) return movement
        val anchorLocation = location ?: movement.anchorLatitude?.let { lat ->
            movement.anchorLongitude?.let { lon ->
                Location("reclassified").apply {
                    latitude = lat
                    longitude = lon
                }
            }
        } ?: return movement

        val place = resolvePlaceForLocation(anchorLocation)
        val summary = mutex.withLock {
            visitDao.deleteVisit(movement.visitId)
            val visitId = visitDao.insertVisit(
                VisitEntity(
                    placeOwnerId = place.placeId,
                    startTime = movement.startTime,
                    confidence = 85
                )
            )
            val endTime = movement.endTime ?: System.currentTimeMillis()
            visitDao.endVisit(visitId, endTime)
            VisitSummary(
                visitId = visitId,
                placeId = place.placeId,
                category = place.category,
                type = SessionType.STILL,
                startTime = movement.startTime,
                endTime = endTime,
                anchorLatitude = anchorLocation.latitude,
                anchorLongitude = anchorLocation.longitude
            )
        }
        summary?.let { sleepDetectionManager.onStillVisitCompleted(it) }
        return summary
    }

    suspend fun clear(endTime: Long) {
        endCurrentVisit(endTime)
    }

    private suspend fun resolvePlaceForLocation(location: Location): PlaceEntity {
        val existing = placeDao.getAll()
        existing.forEach { place ->
            if (place.radiusMeters > 0f) {
                val dest = Location("place").apply {
                    latitude = place.latitude
                    longitude = place.longitude
                }
                val distance = location.distanceTo(dest)
                if (distance <= place.radiusMeters) {
                    return place
                }
            }
        }
        val label = String.format(
            Locale.US,
            "Stay near %.3f, %.3f",
            location.latitude,
            location.longitude
        )
        val category = String.format(
            Locale.US,
            "still:%.4f:%.4f",
            location.latitude,
            location.longitude
        )
        val entity = PlaceEntity(
            label = label,
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMeters = 200f,
            category = category,
            colorArgb = colorForKey(category)
        )
        val id = placeDao.insert(entity)
        return entity.copy(placeId = id)
    }

    private fun colorForKey(key: String): Long {
        val hash = abs(key.hashCode())
        val r = 0x40 + (hash and 0x3F)
        val g = 0x40 + ((hash shr 6) and 0x3F)
        val b = 0x40 + ((hash shr 12) and 0x3F)
        val value = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return value.toLong()
    }
}
