package com.bilimusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bilimusic.app.ui.BiliMusicMain
import com.bilimusic.app.ui.theme.BiliMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = BiliMusicApp.instance.preferences
            val isDark by settings.isDarkTheme.collectAsState(initial = true)
            val seedColor by settings.seedColor.collectAsState(initial = "00A1D6")

            BiliMusicTheme(
                darkTheme = isDark,
                dynamicColor = false,
                seedColorHex = seedColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BiliMusicMain()
                }
            }
        }
    }
}
