package com.yourcompany.lifecycleclone.core.repository

import com.yourcompany.lifecycleclone.core.db.VisitDao
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository exposing visit data queries to the UI. It wraps [VisitDao] and provides
 * convenience functions to observe visits over different time ranges as [Flow]s, which can
 * automatically update the UI when the underlying database changes.
 */
class VisitRepository(private val visitDao: VisitDao) {

    /**
     * Returns a [Flow] of visits occurring between the provided epoch millis. Consumers
     * should specify [from] and [to] boundaries (e.g. the start and end of the current day).
     */
    fun observeVisitsInRange(from: Long, to: Long): Flow<List<VisitWithPlace>> = flow {
        // We emit the query once initially. Clients can trigger updates by
        // collecting from the returned flow repeatedly or by using a database change
        // observation mechanism (e.g. RoomFlow). For simplicity this example does not
        // implement continuous updates; you could replace this with visitDao.getVisitsInRangeFlow
        // if using Room 2.5+ with Flow return types.
        val visits = visitDao.getVisitsInRange(from, to)
        emit(visits)
    }

    /**
     * Helper function to obtain visits for the current day. It computes the start of today
     * and end of today using the system default timezone.
     */
    fun observeTodayVisits(): Flow<List<VisitWithPlace>> {
        val now = java.time.ZonedDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
        return observeVisitsInRange(startOfDay, endOfDay)
    }

    /**
     * Convenience suspend variant for one-off range queries.
     */
    suspend fun getVisitsInRange(from: Long, to: Long): List<VisitWithPlace> = visitDao.getVisitsInRange(from, to)
}
