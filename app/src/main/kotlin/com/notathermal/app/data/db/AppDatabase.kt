package com.notathermal.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InvoiceEntity::class, InvoiceItemEntity::class, PlnCustomerEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun plnCustomerDao(): PlnCustomerDao

    companion object {
        /**
         * v2 → v3: introduce the `pln_customers` table that backs the PLN form's
         * "saved customers" picker. Existing invoice tables are untouched, so
         * historical invoices remain readable across the upgrade.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pln_customers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        meterNo TEXT NOT NULL,
                        customerName TEXT NOT NULL,
                        lastUsedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pln_customers_meterNo " +
                        "ON pln_customers(meterNo)"
                )
            }
        }
    }
}
