package com.yourcompany.lifecycleclone.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.yourcompany.lifecycleclone.tracking.service.LocationService

/**
 * Simple utility to start and stop the tracking foreground service. Settings can toggle automatic
 * tracking through this helper.
 */
object TrackingController {
    fun startTracking(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
    }
}
