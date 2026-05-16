package com.bilimusic.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.ui.component.BiliBottomBar
import com.bilimusic.app.ui.component.BiliMiniPlayer
import com.bilimusic.app.ui.navigation.Destinations
import com.bilimusic.app.ui.screen.HomeScreen
import com.bilimusic.app.ui.screen.ExploreScreen
import com.bilimusic.app.ui.screen.AboutScreen
import com.bilimusic.app.ui.screen.SettingsScreen
import com.bilimusic.app.ui.screen.NowPlayingScreen
import com.bilimusic.app.ui.screens.debug.DebugScreen
import com.bilimusic.app.ui.screens.favorites.FolderDetailScreen
import com.bilimusic.app.ui.screens.login.LoginScreen
import com.bilimusic.app.ui.screens.settings.FavoritesManageScreen

private val bottomNavItems = listOf(
    Destinations.Home to Icons.Outlined.Home,
    Destinations.Explore to Icons.Outlined.Search,
    Destinations.Settings to Icons.Outlined.Settings
)

@Composable
fun BiliMusicMain() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomBarRoutes = bottomNavItems.map { it.first.route }
    val showBottomBar = currentRoute in bottomBarRoutes

    val playerState by PlayerManager.state.collectAsState()
    val showMiniPlayer = playerState.currentSong != null
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showNowPlaying) {
        showNowPlaying = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                BiliBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemSelected = { dest ->
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Destinations.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    Destinations.Home.route,
                    enterTransition = {
                        scaleIn(
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 200f
                            ),
                            initialScale = 0.85f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    exitTransition = {
                        scaleOut(
                            animationSpec = tween(200, easing = EaseInOutCubic),
                            targetScale = 0.95f
                        ) + fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    },
                    popEnterTransition = {
                        scaleIn(
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 200f
                            ),
                            initialScale = 0.95f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    popExitTransition = {
                        scaleOut(
                            animationSpec = tween(200, easing = EaseInOutCubic),
                            targetScale = 0.85f
                        ) + fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    }
                ) {
                    HomeScreen(
                        onNavigateToLogin = { navController.navigate(Destinations.Login.route) },
                        onNavigateToDebug = { navController.navigate(Destinations.Debug.route) },
                        onNavigateToSettings = { navController.navigate(Destinations.Settings.route) },
                        onNavigateToFolder = { folderId ->
                            navController.navigate("folder_detail/$folderId")
                        }
                    )
                }
                composable(
                    Destinations.Explore.route,
                    enterTransition = {
                        scaleIn(
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                            initialScale = 0.85f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    exitTransition = {
                        scaleOut(animationSpec = tween(200, easing = EaseInOutCubic), targetScale = 0.95f) +
                            fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    },
                    popEnterTransition = {
                        scaleIn(
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                            initialScale = 0.95f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    popExitTransition = {
                        scaleOut(animationSpec = tween(200, easing = EaseInOutCubic), targetScale = 0.85f) +
                            fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    }
                ) {
                    ExploreScreen(
                        onPlayVideo = { showNowPlaying = true }
                    )
                }
                composable(
                    Destinations.Settings.route,
                    enterTransition = {
                        scaleIn(
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                            initialScale = 0.85f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    exitTransition = {
                        scaleOut(animationSpec = tween(200, easing = EaseInOutCubic), targetScale = 0.95f) +
                            fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    },
                    popEnterTransition = {
                        scaleIn(
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                            initialScale = 0.95f
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInOutCubic))
                    },
                    popExitTransition = {
                        scaleOut(animationSpec = tween(200, easing = EaseInOutCubic), targetScale = 0.85f) +
                            fadeOut(animationSpec = tween(200, easing = EaseInOutCubic))
                    }
                ) {
                    SettingsScreen(
                        onNavigateToLogin = { navController.navigate(Destinations.Login.route) },
                        onNavigateToDebug = { navController.navigate(Destinations.Debug.route) },
                        onNavigateToAbout = { navController.navigate(Destinations.About.route) },
                        onNavigateToFavoritesManage = { navController.navigate(Destinations.FavoritesManage.route) }
                    )
                }
                composable(
                    Destinations.Login.route,
                    enterTransition = {
                        slideInVertically(animationSpec = tween(220)) { it } + fadeIn()
                    },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = {
                        slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(animationSpec = tween(240)) { it } + fadeOut()
                    }
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Destinations.Home.route) {
                                popUpTo(Destinations.Login.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    Destinations.Debug.route,
                    enterTransition = {
                        slideInVertically(animationSpec = tween(220)) { it } + fadeIn()
                    },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = {
                        slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(animationSpec = tween(240)) { it } + fadeOut()
                    }
                ) {
                    DebugScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Destinations.About.route,
                    enterTransition = {
                        slideInVertically(animationSpec = tween(220)) { it } + fadeIn()
                    },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = {
                        slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(animationSpec = tween(240)) { it } + fadeOut()
                    }
                ) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Destinations.FolderDetail.route,
                    arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
                    enterTransition = {
                        slideInVertically(animationSpec = tween(220)) { it } + fadeIn()
                    },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = {
                        slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(animationSpec = tween(240)) { it } + fadeOut()
                    }
                ) { backStackEntry ->
                    val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
                    FolderDetailScreen(
                        folderId = folderId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    Destinations.FavoritesManage.route,
                    enterTransition = {
                        slideInVertically(animationSpec = tween(220)) { it } + fadeIn()
                    },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = {
                        slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()
                    },
                    popExitTransition = {
                        slideOutVertically(animationSpec = tween(240)) { it } + fadeOut()
                    }
                ) {
                    FavoritesManageScreen(onBack = { navController.popBackStack() })
                }
            }

            AnimatedVisibility(
                visible = showMiniPlayer,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(120))
            ) {
                BiliMiniPlayer(
                    onExpand = { showNowPlaying = true }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showNowPlaying,
        enter = slideInVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(150)),
        exit = slideOutVertically(
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(150))
    ) {
        NowPlayingScreen(
            onDismiss = { showNowPlaying = false }
        )
    }
}
