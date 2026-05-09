package com.notathermal.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val customerName: String? = null,
    val cashierName: String? = null,
    val note: String? = null,
    val subtotal: Double,
    val discount: Double,
    val taxPercent: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String,
    val paymentReceived: Double,
    val change: Double,
    val createdAt: Long
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val name: String,
    val quantity: Double,
    val price: Double,
    val discount: Double = 0.0,
    val subtotal: Double
)
