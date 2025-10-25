package com.yourcompany.lifecycleclone.tracking.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yourcompany.lifecycleclone.R
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.tracking.visits.VisitSessionManager
import kotlinx.coroutines.launch
import com.yourcompany.lifecycleclone.tracking.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Foreground service responsible for acquiring location data in a battery‑friendly manner.  It
 * runs as a long‑lived service with a persistent notification when automatic tracking is
 * enabled.  On each location update it delegates to [VisitSessionManager] to maintain the
 * user's visit timeline.
 */
class TrackingForegroundService : Service() {
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var visitSessionManager: VisitSessionManager

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        // Initialize location provider and callback.
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location? = result.lastLocation
                if (location != null) {
                    visitSessionManager.onLocationSample(location)
                }
            }
        }
        // Initialise database and session manager.
        val db = AppDatabase.getInstance(applicationContext)
        visitSessionManager = VisitSessionManager(db.visitDao(), db.placeDao())
        // Expose session manager for geofence broadcast receiver
        sessionManager = visitSessionManager
        startForegroundService()
        requestLocationUpdates()

        // Register geofences for all saved places so transitions can trigger visit updates.
        // Because getAll() is suspend, launch a coroutine on IO dispatcher.
        val geofenceManager = GeofenceManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            val savedPlaces = db.placeDao().getAll()
            geofenceManager.registerGeofences(savedPlaces)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the service is killed by the system, restart it with the same intent.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        visitSessionManager.endCurrentVisit()
        // Clear the static reference to the session manager
        sessionManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Creates and displays the persistent notification required by Android for a foreground
     * service that uses location.  The notification uses strings defined in the resource file.
     */
    private fun startForegroundService() {
        val channelId = createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_content))
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Registers the service as a foreground service.  Android 8+ requires a notification channel.
     */
    private fun createNotificationChannel(): String {
        val channelId = "tracking_foreground_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Tracking"
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }
        return channelId
    }

    /**
     * Requests periodic location updates with balanced power accuracy.  In a real implementation
     * this method would also subscribe to ActivityRecognition transitions and register geofences
     * for known places.  The update interval here is set to a modest 10 seconds and may need
     * tuning for your use case.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L
        ).setMinUpdateIntervalMillis(10_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()
        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        /**
         * Holds a reference to the current [VisitSessionManager] so that geofence events can
         * update visits even when the service instance isn't directly accessible.  It will be
         * set when the service is created and cleared when the service is destroyed.
         */
        @Volatile
        private var sessionManager: VisitSessionManager? = null

        /**
         * Handles geofence transition events forwarded by [GeofenceBroadcastReceiver].  This
         * method updates the visit timeline according to the transition type: entering a
         * geofence starts a new visit for that place, while exiting ends the current visit.
         */
        fun handleGeofenceEvent(context: Context, event: com.google.android.gms.location.GeofencingEvent) {
            val transition = event.geofenceTransition
            val triggeredGeofences = event.triggeringGeofences ?: return
            val manager = sessionManager ?: return
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            triggeredGeofences.forEach { geofence ->
                val placeId = geofence.requestId.toLongOrNull() ?: return@forEach
                if (transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER) {
                    scope.launch {
                        manager.startVisitForPlace(placeId)
                    }
                } else if (transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
                    manager.endCurrentVisit()
                }
            }
        }
    }
}