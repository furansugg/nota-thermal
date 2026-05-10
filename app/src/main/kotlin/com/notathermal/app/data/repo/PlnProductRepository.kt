package com.notathermal.app.data.repo

import com.notathermal.app.data.db.PlnProductDao
import com.notathermal.app.data.db.PlnProductEntity
import kotlinx.coroutines.flow.Flow

class PlnProductRepository(private val dao: PlnProductDao) {

    fun observeAll(): Flow<List<PlnProductEntity>> = dao.observeAll()

    suspend fun get(id: Long): PlnProductEntity? = dao.getById(id)

    suspend fun create(name: String, nominal: Double, now: Long = System.currentTimeMillis()): Long =
        dao.insert(
            PlnProductEntity(
                name = name.trim(),
                nominal = nominal,
                sortOrder = 0,
                createdAt = now
            )
        )

    suspend fun update(id: Long, name: String, nominal: Double) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(name = name.trim(), nominal = nominal))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
