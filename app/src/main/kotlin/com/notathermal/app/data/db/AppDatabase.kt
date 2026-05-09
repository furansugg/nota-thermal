package com.notathermal.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [InvoiceEntity::class, InvoiceItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
}
