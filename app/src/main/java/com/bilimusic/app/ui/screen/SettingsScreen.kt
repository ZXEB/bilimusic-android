package com.bilimusic.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFavoritesManage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    com.bilimusic.app.ui.screens.settings.SettingsScreen(
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToDebug = onNavigateToDebug,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToFavoritesManage = onNavigateToFavoritesManage
    )
}
