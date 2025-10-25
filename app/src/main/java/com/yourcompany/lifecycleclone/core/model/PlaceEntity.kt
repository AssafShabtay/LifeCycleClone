package com.yourcompany.lifecycleclone.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a named location where the user spends time.  The app will use this information to
 * determine when the user is at Home, Work, Gym, etc.  Each place stores a lat/lon coordinate
 * along with a radius for geofencing.
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val placeId: Long = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val category: String,
    val colorArgb: Long
)