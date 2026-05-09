package com.notathermal.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notathermal.app.NotaApp
import com.notathermal.app.di.AppContainer

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as NotaApp).container
    return viewModel(
        factory = viewModelFactory {
            initializer { create(container) }
        }
    )
}
