package com.notathermal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlnProductDao {

    /**
     * Sorts by explicit `sortOrder` first (so the user can re-order if we ever
     * expose drag-to-reorder), then by `nominal` so larger nominal lands at
     * the bottom — matches how typical PLN agen displays them.
     */
    @Query("SELECT * FROM pln_products ORDER BY sortOrder ASC, nominal ASC, id ASC")
    fun observeAll(): Flow<List<PlnProductEntity>>

    @Query("SELECT * FROM pln_products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PlnProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: PlnProductEntity): Long

    @Update
    suspend fun update(product: PlnProductEntity)

    @Query("DELETE FROM pln_products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
