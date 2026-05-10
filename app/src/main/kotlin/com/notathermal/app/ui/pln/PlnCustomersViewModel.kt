package com.notathermal.app.ui.pln

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.PlnCustomerEntity
import com.notathermal.app.data.repo.PlnCustomerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlnCustomersViewModel(
    private val repository: PlnCustomerRepository
) : ViewModel() {

    val customers: StateFlow<List<PlnCustomerEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
