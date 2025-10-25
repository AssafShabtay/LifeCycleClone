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
        startForegroundService()
        requestLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the service is killed by the system, restart it with the same intent.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        visitSessionManager.endCurrentVisit()
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
            /* intervalMillis = */ 10_000L
        )
            .setMinUpdateIntervalMillis(10_000L) // fastest interval
            .setMinUpdateDistanceMeters(50f)     // only update if moved 50m
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}