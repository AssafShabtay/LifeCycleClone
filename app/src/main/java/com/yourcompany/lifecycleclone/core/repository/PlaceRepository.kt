package com.yourcompany.lifecycleclone.core.repository

import com.yourcompany.lifecycleclone.core.db.PlaceDao
import com.yourcompany.lifecycleclone.core.model.PlaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing [PlaceEntity] objects.  Provides methods for
 * observing all places as a flow, inserting new places and updating existing ones.  The flow
 * implementation here is simplistic; for real time updates you can return a Room Flow directly
 * from the DAO (requires newer Room versions).
 */
class PlaceRepository(private val placeDao: PlaceDao) {

    fun observeAllPlaces(): Flow<List<PlaceEntity>> = flow {
        val places = placeDao.getAll()
        emit(places)
    }

    suspend fun insertPlace(place: PlaceEntity): Long = withContext(Dispatchers.IO) {
        placeDao.insert(place)
    }

    suspend fun deletePlace(placeId: Long) = withContext(Dispatchers.IO) {
        placeDao.delete(placeId)
    }
}