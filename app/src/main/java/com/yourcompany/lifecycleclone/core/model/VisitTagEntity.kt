package com.yourcompany.lifecycleclone.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Associates additional metadata with a particular visit.  Tags can be used to label visits with
 * people or custom activities (e.g. "Date night", "Deep work").  Tags are optional and can be
 * expanded in the future to support other types of contextual information.
 */
@Entity(tableName = "visit_tags")
data class VisitTagEntity(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0,
    val visitOwnerId: Long,
    val tagType: String,
    val value: String
)