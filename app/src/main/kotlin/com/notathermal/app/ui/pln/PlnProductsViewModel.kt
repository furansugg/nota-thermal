package com.notathermal.app.ui.pln

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.PlnProductEntity
import com.notathermal.app.data.repo.PlnProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlnProductsViewModel(
    private val repository: PlnProductRepository
) : ViewModel() {

    val products: StateFlow<List<PlnProductEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, nominal: Double) {
        if (name.isBlank() || nominal <= 0) return
        viewModelScope.launch { repository.create(name, nominal) }
    }

    fun update(id: Long, name: String, nominal: Double) {
        if (name.isBlank() || nominal <= 0) return
        viewModelScope.launch { repository.update(id, name, nominal) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
