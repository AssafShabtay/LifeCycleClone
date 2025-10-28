package com.yourcompany.lifecycleclone.tracking.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.yourcompany.lifecycleclone.tracking.service.TrackingForegroundService

/**
 * Broadcast receiver to listen for activity transition events such as walking or driving.
 * When a transition occurs we delegate to [TrackingForegroundService] to start or stop
 * special activity visits.  The Activity Transition API will deliver both enter and exit
 * transitions for supported activity types.
 */
class ActivityTransitionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        for (event in result.transitionEvents) {
            val entering = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            val category = when (event.activityType) {
                DetectedActivity.WALKING -> "walking"
                DetectedActivity.IN_VEHICLE -> "driving"
                DetectedActivity.RUNNING -> "running"
                DetectedActivity.ON_BICYCLE -> "cycling"
                DetectedActivity.ON_FOOT -> "walking"
                else -> null
            }
            category?.let {
                TrackingForegroundService.handleActivityTransition(context, it, entering)
            }
        }
    }
}