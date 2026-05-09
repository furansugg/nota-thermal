package com.notathermal.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.notathermal.app.ui.detail.InvoiceDetailScreen
import com.notathermal.app.ui.form.InvoiceFormScreen
import com.notathermal.app.ui.home.HomeScreen
import com.notathermal.app.ui.printer.PrinterScreen
import com.notathermal.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val NEW_INVOICE = "invoice/new"
    const val INVOICE_DETAIL = "invoice/{id}"
    const val SETTINGS = "settings"
    const val PRINTER = "printer"

    fun invoiceDetail(id: Long) = "invoice/$id"
}

@Composable
fun NotaApp() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCreateInvoice = { navController.navigate(Routes.NEW_INVOICE) },
                    onOpenInvoice = { id -> navController.navigate(Routes.invoiceDetail(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenPrinter = { navController.navigate(Routes.PRINTER) }
                )
            }
            composable(Routes.NEW_INVOICE) {
                InvoiceFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.invoiceDetail(id))
                    }
                )
            }
            composable(Routes.INVOICE_DETAIL) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                InvoiceDetailScreen(
                    invoiceId = id,
                    onBack = { navController.popBackStack() },
                    onOpenPrinter = { navController.navigate(Routes.PRINTER) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PRINTER) {
                PrinterScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
