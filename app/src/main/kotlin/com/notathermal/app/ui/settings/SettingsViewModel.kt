package com.notathermal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.domain.PaperWidth
import com.notathermal.app.domain.TextAlign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Emits null while the saved settings are still loading from DataStore. Once
     * loaded, emits the actual stored values. Using null (rather than
     * AppSettings.Default) avoids the race where the form pre-fills with the
     * default values before the persisted ones arrive.
     */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun save(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        headerText: String,
        footerText: String,
        paperWidth: PaperWidth,
        titleAlignment: TextAlign,
        currencySymbol: String,
        showCashier: Boolean,
        showCustomer: Boolean,
        cutPaper: Boolean,
        copies: Int,
        taxPercentDefault: Double,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _saving.update { true }
            settingsRepository.update {
                it.copy(
                    storeName = storeName,
                    storeAddress = storeAddress,
                    storePhone = storePhone,
                    headerText = headerText,
                    footerText = footerText,
                    paperWidth = paperWidth,
                    titleAlignment = titleAlignment,
                    currencySymbol = currencySymbol,
                    showCashier = showCashier,
                    showCustomer = showCustomer,
                    cutPaper = cutPaper,
                    copies = copies,
                    taxPercentDefault = taxPercentDefault
                )
            }
            _saving.update { false }
            onDone()
        }
    }
}
