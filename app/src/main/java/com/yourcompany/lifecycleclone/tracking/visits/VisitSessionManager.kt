package com.yourcompany.lifecycleclone.tracking.visits

import android.location.Location
import com.yourcompany.lifecycleclone.core.db.PlaceDao
import com.yourcompany.lifecycleclone.core.db.VisitDao
import com.yourcompany.lifecycleclone.core.model.VisitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Handles the creation and updating of visits based on incoming location samples and geofence
 * events.  In a complete implementation, this class clusters raw location data into visits and
 * closes visits when the user departs a place.  The current implementation contains
 * placeholders that must be filled in.
 */
class VisitSessionManager(
    private val visitDao: VisitDao,
    private val placeDao: PlaceDao
) {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    // The ID of the currently active visit, if any.  When null no visit is ongoing.
    private var currentVisitId: Long? = null

    // The place ID associated with the current visit.  Helps avoid starting a new visit if
    // successive location samples remain within the same place.
    private var currentPlaceId: Long? = null

    // A simple job that can be cancelled to stop any long‑running operations. Not used yet.
    private var trackingJob: Job? = null

    /**
     * Called when a new location sample arrives.  This method should update or create a Visit
     * based on the user's proximity to known places.  The current implementation is a stub
     * demonstrating how to persist a visit start.  Replace the TODO sections with real
     * clustering and geofence logic.
     */
    fun onLocationSample(location: Location) {
        scope.launch {
            // Determine the nearest place within its geofence radius.
            val places = placeDao.getAll()
            var matchedPlace: com.yourcompany.lifecycleclone.core.model.PlaceEntity? = null
            for (place in places) {
                val dest = Location("place").apply {
                    latitude = place.latitude
                    longitude = place.longitude
                }
                val distance = location.distanceTo(dest)
                if (distance <= place.radiusMeters) {
                    matchedPlace = place
                    break
                }
            }
            if (matchedPlace != null) {
                // We're inside an existing place.
                if (currentPlaceId == matchedPlace.placeId) {
                    // Same place, just update end time in database when we eventually leave.
                    return@launch
                } else {
                    // Entered a different place.  Close current visit if exists and start a new one.
                    endCurrentVisit()
                    val visit = VisitEntity(
                        placeOwnerId = matchedPlace.placeId,
                        startTime = System.currentTimeMillis(),
                        confidence = 100
                    )
                    currentVisitId = visitDao.insertVisit(visit)
                    currentPlaceId = matchedPlace.placeId
                }
            } else {
                // Not inside any known place.  Close existing visit and create a new place.
                endCurrentVisit()
                // Create a new unknown place at this location.
                val newPlace = com.yourcompany.lifecycleclone.core.model.PlaceEntity(
                    label = "Unknown Place",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = 200f,
                    category = "unknown",
                    colorArgb = (0xFF000000 or kotlin.random.Random.nextInt().toLong()).toLong()
                )
                val placeId = placeDao.insert(newPlace)
                // Start a new visit associated with the new place.
                val newVisit = VisitEntity(
                    placeOwnerId = placeId,
                    startTime = System.currentTimeMillis(),
                    confidence = 80
                )
                currentVisitId = visitDao.insertVisit(newVisit)
                currentPlaceId = placeId
            }
        }
    }

    /**
     * Starts a new visit at the specified [placeId].  Any currently active visit will be
     * closed before the new visit is created.  This method is used by geofence events to
     * ensure visits reflect enter/exit transitions.
     */
    suspend fun startVisitForPlace(placeId: Long) {
        endCurrentVisit()
        val visit = VisitEntity(
            placeOwnerId = placeId,
            startTime = System.currentTimeMillis(),
            confidence = 90
        )
        currentVisitId = visitDao.insertVisit(visit)
        currentPlaceId = placeId
    }

    /**
     * Closes any currently active visit.  This can be called when a geofence exit event is
     * received or when the tracking service stops.  The end time is set to the current time.
     */
    fun endCurrentVisit() {
        scope.launch {
            currentVisitId?.let { visitId ->
                visitDao.endVisit(visitId, System.currentTimeMillis())
                currentVisitId = null
                currentPlaceId = null
            }
        }
    }
}