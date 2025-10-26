package com.yourcompany.lifecycleclone.tracking.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.yourcompany.lifecycleclone.core.model.PlaceEntity

/**
 * Helper class responsible for registering and unregistering geofences for the user's saved
 * places. See original KDoc for details.
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    /**
     * Registers geofences for all provided [places]. Existing geofences are cleared first.
     *
     * If required location permission isn't granted, this does nothing.
     */
    fun registerGeofences(places: List<PlaceEntity>) {
        if (!hasRequiredLocationPermission()) {
            Log.w(TAG, "registerGeofences() skipped: location permission not granted.")
            return
        }

        try {
            geofencingClient.removeGeofences(getPendingIntent()).addOnCompleteListener {
                if (places.isEmpty()) {
                    Log.i(TAG, "registerGeofences() skipped: no saved places to register.")
                    return@addOnCompleteListener
                }

                val geofences = places.map { place ->
                    Geofence.Builder()
                        .setRequestId(place.placeId.toString())
                        .setCircularRegion(
                            place.latitude,
                            place.longitude,
                            place.radiusMeters
                        )
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or
                                    Geofence.GEOFENCE_TRANSITION_EXIT
                        )
                        .setLoiteringDelay(5 * 60 * 1000) // 5 min debounce
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build()
                }

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences)
                    .build()

                // Safe because we already checked permission and we're in try/catch.
                geofencingClient.addGeofences(request, getPendingIntent())
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to add geofences", e)
                    }
            }
        } catch (se: SecurityException) {
            // This can still happen if permission was revoked between the check and the call.
            Log.w(TAG, "SecurityException while registering geofences", se)
        }
    }

    /**
     * Removes all currently registered geofences. Useful when stopping tracking.
     *
     * We wrap in try/catch for the same SecurityException reason.
     */
    fun clearGeofences() {
        if (!hasRequiredLocationPermission()) {
            Log.w(TAG, "clearGeofences() skipped: location permission not granted.")
            return
        }

        try {
            geofencingClient.removeGeofences(getPendingIntent())
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException while clearing geofences", se)
        }
    }

    /**
     * Returns a [PendingIntent] that the geofencing system will fire when a geofence
     * transition occurs.
     */
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Checks that we hold both foreground location and (on Android 10+/Q+) background location,
     * which is required for geofencing to work reliably when the app is not in the foreground.
     */
    private fun hasRequiredLocationPermission(): Boolean {
        val fineGranted =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val backgroundGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // pre-Q doesn't have background permission
            }

        return fineGranted && backgroundGranted
    }

    companion object {
        private const val TAG = "GeofenceManager"
    }
}
