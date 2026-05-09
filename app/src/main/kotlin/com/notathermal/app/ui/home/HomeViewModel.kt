package com.notathermal.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.InvoiceEntity
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val invoices: StateFlow<List<InvoiceEntity>> = invoiceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)
}
