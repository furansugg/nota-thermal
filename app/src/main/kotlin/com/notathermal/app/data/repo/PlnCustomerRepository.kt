package com.notathermal.app.data.repo

import com.notathermal.app.data.db.PlnCustomerDao
import com.notathermal.app.data.db.PlnCustomerEntity
import kotlinx.coroutines.flow.Flow

class PlnCustomerRepository(private val dao: PlnCustomerDao) {

    fun observeAll(): Flow<List<PlnCustomerEntity>> = dao.observeAll()

    suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Idempotent save by meter number. If the meter exists, the row is updated
     * (name + lastUsedAt) so the most recent name wins; if not, a new row is
     * created. Returns true when something was actually written so callers can
     * surface telemetry/logging if desired.
     */
    suspend fun rememberCustomer(
        meterNo: String,
        customerName: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanedMeter = meterNo.trim()
        val cleanedName = customerName.trim()
        if (cleanedMeter.isBlank() || cleanedName.isBlank()) return false
        val existing = dao.getByMeter(cleanedMeter)
        val toSave = (existing ?: PlnCustomerEntity(
            meterNo = cleanedMeter,
            customerName = cleanedName,
            lastUsedAt = now
        )).copy(
            meterNo = cleanedMeter,
            customerName = cleanedName,
            lastUsedAt = now
        )
        dao.upsert(toSave)
        return true
    }
}
