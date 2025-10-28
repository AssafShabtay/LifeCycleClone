package com.yourcompany.lifecycleclone.tracking.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.yourcompany.lifecycleclone.core.model.PlaceEntity

/**
 * Helper class responsible for registering and unregistering geofences for the user's saved
 * places.  When the user defines a place (e.g. "Home" or "Work") this class will create a
 * geofence around that location so that the system can wake the app when the user enters or
 * exits.  A [GeofenceBroadcastReceiver] will handle these events and pass them to the tracking
 * logic.  This class uses the Google Play services [GeofencingClient].
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    /**
     * Registers geofences for all provided [places].  Existing geofences will be cleared
     * beforehand.  If the location permission is missing the request will be silently ignored.
     */
    fun registerGeofences(places: List<PlaceEntity>) {
        // First remove any existing geofences to avoid duplicates
        geofencingClient.removeGeofences(getPendingIntent()).addOnCompleteListener {
            val geofences = places.mapNotNull { place ->
                if (place.radiusMeters <= 0f) return@mapNotNull null
                Geofence.Builder()
                    .setRequestId(place.placeId.toString())
                    .setCircularRegion(place.latitude, place.longitude, place.radiusMeters)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay(5 * 60 * 1000) // 5 min delay to avoid rapid enter/exit
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            }
            if (geofences.isEmpty()) {
                return@addOnCompleteListener
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()
            geofencingClient.addGeofences(request, getPendingIntent())
                .addOnFailureListener { /* Log or handle error in production */ }
        }
    }

    /**
     * Removes all currently registered geofences.  Useful when stopping tracking.
     */
    fun clearGeofences() {
        geofencingClient.removeGeofences(getPendingIntent())
    }

    /**
     * Returns a [PendingIntent] that the geofencing system will fire when a geofence
     * transition occurs.  This uses a broadcast receiver so the app can respond even when
     * backgrounded.
     */
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Use FLAG_MUTABLE to allow adding extra data on Android 12+
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}