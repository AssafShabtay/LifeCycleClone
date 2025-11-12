package com.yourcompany.lifecycleclone.tracking.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.yourcompany.lifecycleclone.tracking.service.LocationService

/**
 * Broadcast receiver to listen for activity transition events such as walking or driving.
 * When a transition occurs we delegate to [LocationService] so it can start or stop the
 * appropriate session.
 */
class ActivityTransitionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        for (event in result.transitionEvents) {
            val entering = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            LocationService.handleActivityTransition(context, event.activityType, entering)
        }
    }
}
