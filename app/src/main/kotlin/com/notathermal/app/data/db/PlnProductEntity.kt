package com.notathermal.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pre-configured PLN token product (e.g. "Token Listrik 100k").
 *
 * The PLN form has a product picker that auto-fills `productName` and `nominal`
 * from one of these rows so the cashier doesn't have to retype them every
 * transaction. Admin fee stays manual (default 0) since it varies per agen/PPOB.
 */
@Entity(tableName = "pln_products")
data class PlnProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nominal: Double,
    val sortOrder: Int = 0,
    val createdAt: Long
)
