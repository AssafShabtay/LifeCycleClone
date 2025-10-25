package com.yourcompany.lifecycleclone.settings

import android.content.Context
import android.content.Intent
import com.yourcompany.lifecycleclone.tracking.service.TrackingForegroundService

/**
 * Simple utility to start and stop the tracking foreground service.  This can be used by the
 * settings screen to toggle automatic tracking on and off.  The service runs with a
 * foreground notification due to background location restrictions on modern Android.
 */
object TrackingController {
    fun startTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java)
        context.startForegroundService(intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java)
        context.stopService(intent)
    }
}