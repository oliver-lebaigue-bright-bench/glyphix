package com.glyphix.shared.logic

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.glyphix.shared.model.Tab

class MainViewModel : ViewModel() {
    private val _selectedTab = MutableStateFlow(Tab.Audio)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _isFabMenuExpanded = MutableStateFlow(false)
    val isFabMenuExpanded: StateFlow<Boolean> = _isFabMenuExpanded.asStateFlow()

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun toggleVisualizer() {
        _isRunning.value = !_isRunning.value
    }
    
    fun toggleFabMenu() {
        _isFabMenuExpanded.value = !_isFabMenuExpanded.value
    }
}
