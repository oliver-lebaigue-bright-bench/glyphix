package com.glyphix.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glyphix.app.ui.*
import com.glyphix.shared.logic.MainViewModel

@Composable
fun App(viewModel: MainViewModel = viewModel { MainViewModel() }) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isFabMenuExpanded by viewModel.isFabMenuExpanded.collectAsState()

    GlyphixTheme {
        Scaffold(
            topBar = {
                FloatingTopBar(
                    title = "Glyphix",
                    onMenuClick = { /* TODO */ },
                    onProfileClick = { /* TODO */ }
                )
            },
            bottomBar = {
                FloatingBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    isRunning = isRunning,
                    onToggleVisualizer = { viewModel.toggleVisualizer() },
                    isFabMenuExpanded = isFabMenuExpanded,
                    onToggleFabMenu = { viewModel.toggleFabMenu() }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Selected Tab: ${selectedTab.label}")
            }
        }
    }
}
