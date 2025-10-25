package com.yourcompany.lifecycleclone.premium

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Simple in‑memory implementation of a premium entitlement store.  In a real application this
 * would wrap the Google Play Billing client and persist entitlements to disk.  Here we simply
 * keep a boolean flag in memory so the UI can react to premium status.  Consumers can call
 * [togglePremium] to simulate purchasing or cancelling the subscription.
 */
class PremiumRepository(context: Context) {
    private val _isPremium = MutableStateFlow(false)

    /**
     * A flow emitting the current premium status.  Use this to update UI components that
     * depend on whether the user has unlocked features such as backup and extended insights.
     */
    val isPremium: Flow<Boolean> = _isPremium.asStateFlow()

    /**
     * Simulates purchasing or cancelling a premium subscription by toggling the flag.  In a
     * production implementation this would initiate a purchase flow via the Play Billing
     * library and update the local entitlement based on the purchase state.
     */
    fun togglePremium() {
        _isPremium.update { !it }
    }
}