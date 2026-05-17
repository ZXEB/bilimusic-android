package com.bilimusic.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bilimusic.app.ui.screens.search.SearchScreen

@Composable
fun ExploreScreen(
    onPlayVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchScreen(
        onVideoClick = onPlayVideo,
        onBack = {}
    )
}
