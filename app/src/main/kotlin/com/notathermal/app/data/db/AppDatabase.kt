package com.notathermal.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PlnCustomerEntity::class,
        PlnProductEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun plnCustomerDao(): PlnCustomerDao
    abstract fun plnProductDao(): PlnProductDao

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

        /**
         * v3 → v4: introduce the `pln_products` table for pre-configured PLN
         * token products (name + nominal). Used by the product picker in the
         * PLN form to auto-fill product name & nominal in one tap.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pln_products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        nominal REAL NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
