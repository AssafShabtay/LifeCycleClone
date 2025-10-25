package com.yourcompany.lifecycleclone.tracking.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.yourcompany.lifecycleclone.tracking.service.TrackingForegroundService

/**
 * Receives geofence transition events from the Android system and delegates them to the
 * [TrackingForegroundService] for handling.  This receiver must be registered in the
 * manifest.  It simply parses the geofencing event and calls a static method on the service
 * to allow the service to update visits accordingly.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.w("GeofenceReceiver", "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }
        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Delegate handling to the tracking service
            TrackingForegroundService.handleGeofenceEvent(context, geofencingEvent)
        }
    }
}