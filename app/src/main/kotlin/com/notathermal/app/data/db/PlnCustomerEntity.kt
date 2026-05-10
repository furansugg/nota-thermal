package com.notathermal.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Saved PLN customer (name + meter number) for autofill on the PLN form.
 *
 * `meterNo` is the natural key — only one row per meter. We rely on it for
 * upserts; updating a customer's name simply re-inserts and overwrites the
 * existing row via [androidx.room.OnConflictStrategy.REPLACE].
 */
@Entity(
    tableName = "pln_customers",
    indices = [Index(value = ["meterNo"], unique = true)]
)
data class PlnCustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterNo: String,
    val customerName: String,
    val lastUsedAt: Long
)
