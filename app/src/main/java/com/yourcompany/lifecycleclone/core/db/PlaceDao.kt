package com.yourcompany.lifecycleclone.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yourcompany.lifecycleclone.core.model.PlaceEntity

/**
 * DAO for interacting with [PlaceEntity]. It supports insertion, deletion and simple queries
 * returning all known places. Additional operations such as updating place names and colors can
 * be added as the UI evolves.
 */
@Dao
interface PlaceDao {
    @Insert
    suspend fun insert(place: PlaceEntity): Long

    @Query("SELECT * FROM places")
    suspend fun getAll(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE placeId = :placeId LIMIT 1")
    suspend fun getById(placeId: Long): PlaceEntity?

    /**
     * Returns the first place whose [category] matches the given value, or null if no such
     * place exists. Useful for locating special categories like "walking" or "driving".
     */
    @Query("SELECT * FROM places WHERE category = :category LIMIT 1")
    suspend fun getPlaceByCategory(category: String): PlaceEntity?

    @Query("DELETE FROM places WHERE placeId = :placeId")
    suspend fun delete(placeId: Long)
}

