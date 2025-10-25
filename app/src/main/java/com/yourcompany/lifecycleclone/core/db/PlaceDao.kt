package com.yourcompany.lifecycleclone.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yourcompany.lifecycleclone.core.model.PlaceEntity

/**
 * DAO for interacting with [PlaceEntity].  It supports insertion, deletion and simple queries
 * returning all known places.  Additional operations such as updating place names and colors can
 * be added as the UI evolves.
 */
@Dao
interface PlaceDao {
    @Insert
    suspend fun insert(place: PlaceEntity): Long

    @Query("SELECT * FROM places")
    suspend fun getAll(): List<PlaceEntity>

    @Query("DELETE FROM places WHERE placeId = :placeId")
    suspend fun delete(placeId: Long)
}