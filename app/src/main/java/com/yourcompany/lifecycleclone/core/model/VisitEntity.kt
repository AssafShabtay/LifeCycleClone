package com.yourcompany.lifecycleclone.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records a continuous stay at a place.  The [startTime] marks when the user entered the place
 * and [endTime] marks when they left; if [endTime] is null the visit is ongoing.  The
 * [confidence] value indicates the algorithm's certainty that this visit is correctly classified.
 */
@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["placeId"],
            childColumns = ["placeOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("placeOwnerId"),
        Index("startTime"),
        Index("endTime")
    ]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val visitId: Long = 0,
    val placeOwnerId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val confidence: Int
)