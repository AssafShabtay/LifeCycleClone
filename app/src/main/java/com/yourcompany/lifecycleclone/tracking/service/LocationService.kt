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
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yourcompany.lifecycleclone.MainActivity
import com.yourcompany.lifecycleclone.R
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.tracking.activity.ActivityTransitionBroadcastReceiver
import com.yourcompany.lifecycleclone.tracking.geofence.GeofenceManager
import com.yourcompany.lifecycleclone.tracking.visits.VisitSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

/**
 * Foreground service orchestrating ongoing location tracking. It captures fused location updates,
 * reacts to activity recognition transitions, listens for geofence broadcasts, and writes visits
 * through [VisitSessionManager].
 */
class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var sleepDetectionManager: SleepDetectionManager
    private lateinit var visitSessionManager: VisitSessionManager

    private var currentSession: Session? = null
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        val db = AppDatabase.getInstance(applicationContext)
        sleepDetectionManager = SleepDetectionManager(db.sleepSessionDao())
        visitSessionManager = VisitSessionManager(db.visitDao(), db.placeDao(), sleepDetectionManager)
        geofenceManager = GeofenceManager(this)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        activityRecognitionClient = ActivityRecognition.getClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val copy = Location(location)
                serviceScope.launch { handleLocationSample(copy) }
            }
        }

        if (!startForegroundServiceInternal()) {
            return
        }
        requestLocationUpdatesSafe()
        registerActivityTransitionsSafe()
        serviceScope.launch {
            val places = db.placeDao().getAll()
            geofenceManager.registerGeofences(places)
        }
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                serviceScope.launch {
                    sessionMutex.withLock {
                        lastLocation = Location(location)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        removeActivityTransitionsSafe()
        geofenceManager.clearGeofences()

        val now = System.currentTimeMillis()
        val finalResult = runBlocking { sessionMutex.withLock { finalizeCurrentSessionLocked(now) } }
        runBlocking { processFinalization(finalResult) }
        runBlocking { visitSessionManager.clear(now) }

        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    private suspend fun handleLocationSample(location: Location) {
        sessionMutex.withLock {
            lastLocation = location
            when (val session = currentSession) {
                is Session.Still -> {
                    if (!session.startedInDb) {
                        visitSessionManager.beginStillVisit(location, session.startTime)
                        session.startedInDb = true
                    } else {
                        visitSessionManager.updateStillAnchor(location)
                    }
                    session.anchor = location
                }
                is Session.Movement -> {
                    val previous = session.samples.lastOrNull()
                    if (previous != null) {
                        session.totalDistance += previous.distanceTo(location)
                    }
                    val first = session.samples.firstOrNull() ?: location
                    val distanceFromStart = first.distanceTo(location)
                    session.maxDistance = max(session.maxDistance, distanceFromStart)
                    session.samples.add(location)
                }
                is Session.Geofence -> {
                    visitSessionManager.updateStillAnchor(location)
                }
                null -> {
                    // No active session yet; store location for future anchors.
                }
            }
        }
    }

    private suspend fun startStillSession(triggerTime: Long) {
        val finalization = sessionMutex.withLock {
            val pending = finalizeCurrentSessionLocked(triggerTime)
            val anchor = lastLocation?.let { Location(it) }
            val session = Session.Still(triggerTime, startedInDb = false, anchor = anchor)
            currentSession = session
            if (anchor != null) {
                visitSessionManager.beginStillVisit(anchor, triggerTime)
                session.startedInDb = true
            }
            pending
        }
        processFinalization(finalization)
    }

    private suspend fun startMovementSession(activityType: ActivityType, triggerTime: Long) {
        val finalization = sessionMutex.withLock {
            val pending = finalizeCurrentSessionLocked(triggerTime)
            visitSessionManager.beginMovementVisit(activityType.label, triggerTime)
            currentSession = Session.Movement(triggerTime, activityType)
            pending
        }
        processFinalization(finalization)
    }

    private suspend fun endMovementSession(activityType: ActivityType, timestamp: Long) {
        val finalization = sessionMutex.withLock {
            val session = currentSession
            if (session is Session.Movement && session.activity == activityType) {
                finalizeCurrentSessionLocked(timestamp)
            } else {
                FinalizationResult.Empty
            }
        }
        processFinalization(finalization)
    }

    private suspend fun onGeofenceEnter(placeId: Long, timestamp: Long) {
        val finalization = sessionMutex.withLock {
            val pending = finalizeCurrentSessionLocked(timestamp)
            visitSessionManager.startVisitForPlace(placeId, timestamp)
            currentSession = Session.Geofence(timestamp, placeId)
            pending
        }
        processFinalization(finalization)
    }

    private suspend fun onGeofenceExit(placeId: Long, timestamp: Long) {
        val finalization = sessionMutex.withLock {
            val session = currentSession
            if (session is Session.Geofence && session.placeId == placeId) {
                finalizeCurrentSessionLocked(timestamp)
            } else {
                FinalizationResult.Empty
            }
        }
        processFinalization(finalization)
    }

    private suspend fun onActivityTransition(activityType: Int, entering: Boolean) {
        val type = ActivityType.fromDetectedActivity(activityType) ?: return
        val timestamp = System.currentTimeMillis()
        when {
            type == ActivityType.STILL && entering -> startStillSession(timestamp)
            type == ActivityType.STILL && !entering -> {
                val finalization = sessionMutex.withLock { finalizeCurrentSessionLocked(timestamp) }
                processFinalization(finalization)
            }
            entering -> startMovementSession(type, timestamp)
            else -> endMovementSession(type, timestamp)
        }
    }

    private suspend fun finalizeCurrentSessionLocked(endTime: Long): FinalizationResult {
        val session = currentSession ?: return FinalizationResult.Empty
        return when (session) {
            is Session.Still -> {
                currentSession = null
                if (!session.startedInDb) {
                    FinalizationResult.Empty
                } else {
                    FinalizationResult.Still(visitSessionManager.endCurrentVisit(endTime))
                }
            }
            is Session.Geofence -> {
                currentSession = null
                FinalizationResult.Geofence(visitSessionManager.endCurrentVisit(endTime))
            }
            is Session.Movement -> {
                currentSession = null
                val summary = visitSessionManager.endCurrentVisit(endTime)
                val snapshot = MovementSnapshot(
                    activity = session.activity,
                    startTime = session.startTime,
                    samples = session.samples.map { Location(it) },
                    maxDistance = session.maxDistance,
                    fallbackLocation = lastLocation?.let { Location(it) }
                )
                FinalizationResult.Movement(summary, snapshot)
            }
        }
    }

    private suspend fun processFinalization(result: FinalizationResult): VisitSessionManager.VisitSummary? {
        return when (result) {
            FinalizationResult.Empty -> null
            is FinalizationResult.Still -> result.summary
            is FinalizationResult.Geofence -> result.summary
            is FinalizationResult.Movement -> handleMovementFinalization(result.summary, result.snapshot)
        }
    }

    private suspend fun handleMovementFinalization(
        summary: VisitSessionManager.VisitSummary?,
        snapshot: MovementSnapshot
    ): VisitSessionManager.VisitSummary? {
        val endTime = summary?.endTime ?: System.currentTimeMillis()
        val duration = (endTime - snapshot.startTime).coerceAtLeast(0L)
        val isShortFootTrip = snapshot.activity.isFoot() && duration < FOOT_MIN_DURATION_MILLIS
        val stayedNearby = snapshot.maxDistance < MIN_MOVEMENT_RADIUS_METERS
        val shouldConvert = isShortFootTrip || stayedNearby
        if (!shouldConvert) {
            return summary
        }
        val candidateLocation = snapshot.samples.lastOrNull()
            ?: snapshot.samples.firstOrNull()
            ?: snapshot.fallbackLocation
        return if (summary != null) {
            visitSessionManager.reclassifyMovementAsStill(summary, candidateLocation)
        } else if (candidateLocation != null) {
            visitSessionManager.recordStillVisit(
                location = candidateLocation,
                startTime = snapshot.startTime,
                endTime = endTime
            )
        } else {
            summary
        }
    }

    private fun startForegroundServiceInternal(): Boolean {
        if (!canShowForegroundNotification()) {
            Log.w(TAG, "Notifications permission missing; stopping LocationService")
            stopSelf()
            return false
        }
        val channelId = createNotificationChannel()
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_content))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return true
    }

    private fun canShowForegroundNotification(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel(): String {
        val channelId = "tracking_foreground_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdatesSafe() {
        if (!hasLocationPermission()) return
        val builder = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateIntervalMillis(30_000L)
            .setMaxUpdateDelayMillis(5 * 60 * 1000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setMinUpdateDistanceMeters(50f)
        }
        val request = builder.build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun registerActivityTransitionsSafe() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val transitions = listOf(
            transitionFor(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transitionFor(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            transitionFor(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transitionFor(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            transitionFor(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transitionFor(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            transitionFor(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transitionFor(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            transitionFor(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transitionFor(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
        )
        val request = ActivityTransitionRequest(transitions)
        val intent = Intent(this, ActivityTransitionBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
    }

    private fun removeActivityTransitionsSafe() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val intent = Intent(this, ActivityTransitionBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
    }

    private fun transitionFor(activity: Int, transition: Int): ActivityTransition {
        return ActivityTransition.Builder()
            .setActivityType(activity)
            .setActivityTransition(transition)
            .build()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private sealed class Session {
        abstract val startTime: Long

        data class Still(
            override val startTime: Long,
            var startedInDb: Boolean,
            var anchor: Location?
        ) : Session()

        data class Movement(
            override val startTime: Long,
            val activity: ActivityType,
            val samples: MutableList<Location> = mutableListOf(),
            var maxDistance: Float = 0f,
            var totalDistance: Float = 0f
        ) : Session()

        data class Geofence(
            override val startTime: Long,
            val placeId: Long
        ) : Session()
    }

    private enum class ActivityType(val label: String) {
        STILL("still"),
        WALKING("walking"),
        RUNNING("running"),
        BICYCLING("cycling"),
        VEHICLE("driving");

        fun isFoot(): Boolean = this == WALKING || this == RUNNING

        companion object {
            fun fromDetectedActivity(type: Int): ActivityType? {
                return when (type) {
                    DetectedActivity.STILL -> STILL
                    DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> WALKING
                    DetectedActivity.RUNNING -> RUNNING
                    DetectedActivity.ON_BICYCLE -> BICYCLING
                    DetectedActivity.IN_VEHICLE -> VEHICLE
                    else -> null
                }
            }
        }
    }

    private sealed class FinalizationResult {
        object Empty : FinalizationResult()
        data class Still(val summary: VisitSessionManager.VisitSummary?) : FinalizationResult()
        data class Geofence(val summary: VisitSessionManager.VisitSummary?) : FinalizationResult()
        data class Movement(
            val summary: VisitSessionManager.VisitSummary?,
            val snapshot: MovementSnapshot
        ) : FinalizationResult()
    }

    private data class MovementSnapshot(
        val activity: ActivityType,
        val startTime: Long,
        val samples: List<Location>,
        val maxDistance: Float,
        val fallbackLocation: Location?
    )

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTIVITY_TRANSITION_REQUEST_CODE = 2001
        private const val FOOT_MIN_DURATION_MILLIS = 15 * 60 * 1000L
        private const val MIN_MOVEMENT_RADIUS_METERS = 100f
        private const val TAG = "LocationService"

        @Volatile
        private var instance: LocationService? = null

        fun handleGeofenceEvent(context: Context, event: GeofencingEvent) {
            val service = instance ?: run {
                if (hasNotificationPermission(context)) {
                    ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
                } else {
                    Log.w(TAG, "Skipping LocationService start; notification permission not granted")
                }
                return
            }
            val ids = event.triggeringGeofences?.mapNotNull { it.requestId.toLongOrNull() } ?: return
            val timestamp = System.currentTimeMillis()
            service.serviceScope.launch {
                ids.forEach { id ->
                    when (event.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER,
                        Geofence.GEOFENCE_TRANSITION_DWELL -> service.onGeofenceEnter(id, timestamp)
                        Geofence.GEOFENCE_TRANSITION_EXIT -> service.onGeofenceExit(id, timestamp)
                    }
                }
            }
        }

        fun handleActivityTransition(context: Context, activityType: Int, entering: Boolean) {
            val service = instance ?: run {
                if (hasNotificationPermission(context)) {
                    ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
                } else {
                    Log.w(TAG, "Skipping LocationService start; notification permission not granted")
                }
                return
            }
            service.serviceScope.launch {
                service.onActivityTransition(activityType, entering)
            }
        }
        private fun hasNotificationPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        }
    }
}




