package com.notathermal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlnCustomerDao {

    @Query("SELECT * FROM pln_customers ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<PlnCustomerEntity>>

    @Query("SELECT * FROM pln_customers WHERE meterNo = :meterNo LIMIT 1")
    suspend fun getByMeter(meterNo: String): PlnCustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: PlnCustomerEntity): Long

    @Query("DELETE FROM pln_customers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
