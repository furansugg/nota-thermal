package com.notathermal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun observeWithItems(id: Long): Flow<InvoiceWithItems?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getWithItems(id: Long): InvoiceWithItems?

    @Query("SELECT COUNT(*) FROM invoices WHERE createdAt >= :startMillis AND createdAt < :endMillis")
    suspend fun countBetween(startMillis: Long, endMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Transaction
    suspend fun insertInvoiceWithItems(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val id = insertInvoice(invoice)
        insertItems(items.map { it.copy(invoiceId = id) })
        return id
    }

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "UPDATE invoices SET paymentMethod = :method, paymentReceived = :received, change = :change " +
            "WHERE id = :id"
    )
    suspend fun updatePayment(id: Long, method: String, received: Double, change: Double)
}
