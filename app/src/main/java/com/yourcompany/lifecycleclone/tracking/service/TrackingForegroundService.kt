package com.yourcompany.lifecycleclone.tracking.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.yourcompany.lifecycleclone.R
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.tracking.activity.ActivityTransitionBroadcastReceiver
import com.yourcompany.lifecycleclone.tracking.geofence.GeofenceManager
import com.yourcompany.lifecycleclone.tracking.visits.VisitSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground service responsible for acquiring location data in a battery-friendly manner.
 * It runs as a long-lived service with a persistent notification when automatic tracking is
 * enabled. On each location update it delegates to [VisitSessionManager] to maintain the
 * user's visit timeline.
 */
class TrackingForegroundService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var visitSessionManager: VisitSessionManager
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    override fun onCreate() {
        super.onCreate()

        // Initialize fused location + callback
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location: Location ->
                    visitSessionManager.onLocationSample(location)
                }
            }
        }

        // Init Activity Recognition via factory, not constructor
        activityRecognitionClient = ActivityRecognition.getClient(this)

        // Init DB + session manager
        val db = AppDatabase.getInstance(applicationContext)
        visitSessionManager = VisitSessionManager(db.visitDao(), db.placeDao())
        sessionManager = visitSessionManager // expose for geofence/activity callbacks

        // Become a foreground service with persistent notification
        startForegroundServiceInternal()

        // Start location updates if we still have location permission
        requestLocationUpdatesSafe()

        // Register geofences for all saved places
        val geofenceManager = GeofenceManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            val savedPlaces = db.placeDao().getAll()
            geofenceManager.registerGeofences(savedPlaces)
        }

        // Register for activity transition updates (walking, driving, etc.)
        registerActivityTransitionsSafe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the service is killed by the system, try to recreate it with the same intent
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop location updates (no permission check needed here; removeLocationUpdates is safe
        // even if we never started it)
        fusedClient.removeLocationUpdates(locationCallback)

        // End any open visit and clear the static ref
        visitSessionManager.endCurrentVisit()
        sessionManager = null

        // Clean up Activity Recognition registration if we had it
        removeActivityTransitionsSafe()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Build & show the required ongoing notification for a foreground service that uses location.
     */
    private fun startForegroundServiceInternal() {
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
     * Android O+ requires a notification channel for foreground-service notifications.
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
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(chan)
        }
        return channelId
    }

    /**
     * Requests periodic location updates with balanced power accuracy.
     * We gate this behind a runtime permission check, because location is also a dangerous
     * permission.
     */
    private fun requestLocationUpdatesSafe() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            // We don't have location permission; just skip requesting updates.
            return
        }

        // Build a power-friendly request (10s interval, 50m displacement)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            /* intervalMillis = */ 10_000L
        )
            .setMinUpdateIntervalMillis(10_000L)
            .setMinUpdateDistanceMeters(50f)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    /**
     * Register for ActivityRecognition Transition API updates (walking/driving/etc.).
     * We only call the real registration if ACTIVITY_RECOGNITION permission is granted,
     * otherwise we'd risk a SecurityException.
     */
    private fun registerActivityTransitionsSafe() {
        val hasActivityPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasActivityPermission) {
            return // user denied or not yet granted; skip silently
        }

        registerActivityTransitions()
    }

    /**
     * Actually builds the ActivityTransitionRequest and registers it.
     *
     * NOTE: only call this if you already verified ACTIVITY_RECOGNITION is granted.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun registerActivityTransitions() {
        val transitions = listOf(
            // Walking
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            // Driving (in vehicle)
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            // Running
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            // Cycling
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            // On foot (generic)
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        val intent = Intent(this, ActivityTransitionBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        activityRecognitionClient
            .requestActivityTransitionUpdates(request, pendingIntent)
            .addOnFailureListener {
                // You could log this if you want.
            }
    }

    /**
     * Mirror cleanup: only try to remove transitions if we currently have
     * ACTIVITY_RECOGNITION, to avoid SecurityException on destroy.
     */
    private fun removeActivityTransitionsSafe() {
        val hasActivityPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasActivityPermission) {
            return
        }

        removeActivityTransitions()
    }

    /**
     * Unregister from ActivityRecognition Transition API.
     * Should be called when the service is going away to avoid leaked callbacks.
     */
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun removeActivityTransitions() {
        val intent = Intent(this, ActivityTransitionBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
        // You could also .addOnSuccessListener { pendingIntent.cancel() } etc.,
        // as shown in Google examples. :contentReference[oaicite:3]{index=3}
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTIVITY_TRANSITION_REQUEST_CODE = 2001

        /**
         * Holds a reference to the current [VisitSessionManager] so that geofence and
         * activity transition broadcasts can update visits even if they fire outside
         * the service instance.
         */
        @Volatile
        private var sessionManager: VisitSessionManager? = null

        /**
         * Handle geofence transitions forwarded by GeofenceBroadcastReceiver.
         * ENTER => startVisitForPlace(placeId)
         * EXIT  => endCurrentVisit()
         */
        fun handleGeofenceEvent(
            context: Context,
            event: com.google.android.gms.location.GeofencingEvent
        ) {
            val transition = event.geofenceTransition
            val triggeredGeofences = event.triggeringGeofences ?: return
            val manager = sessionManager ?: return

            val scope = CoroutineScope(Dispatchers.IO)
            triggeredGeofences.forEach { geofence ->
                val placeId = geofence.requestId.toLongOrNull() ?: return@forEach
                when (transition) {
                    com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        scope.launch { manager.startVisitForPlace(placeId) }
                    }
                    com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        manager.endCurrentVisit()
                    }
                }
            }
        }

        /**
         * Handle activity transitions forwarded by ActivityTransitionBroadcastReceiver.
         * We map activity category ("walking", "driving", etc.) to a synthetic Place,
         * end the old visit, and start a new visit.
         */
        fun handleActivityTransition(
            context: Context,
            category: String,
            entering: Boolean
        ) {
            val manager = sessionManager ?: return
            val scope = CoroutineScope(Dispatchers.IO)

            if (entering) {
                scope.launch {
                    // Look up or create a synthetic place row for this category.
                    val db = AppDatabase.getInstance(context)
                    val placeDao = db.placeDao()

                    val existing = placeDao.getPlaceByCategory(category)
                    val placeId = if (existing != null) {
                        existing.placeId
                    } else {
                        placeDao.insert(
                            com.yourcompany.lifecycleclone.core.model.PlaceEntity(
                                label = category.replaceFirstChar { it.uppercase() },
                                latitude = 0.0,
                                longitude = 0.0,
                                radiusMeters = 0f,
                                category = category,
                                // cheap-ish stable color from hash
                                colorArgb = (0xFF000000 or abs(category.hashCode()).toLong())
                            )
                        )
                    }

                    manager.endCurrentVisit()
                    manager.startVisitForPlace(placeId)
                }
            } else {
                manager.endCurrentVisit()
            }
        }
    }
}
