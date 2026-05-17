package com.bilimusic.app.ui.navigation

sealed class Destinations(val route: String, val label: String) {
    data object Home : Destinations("home", "首页")
    data object Explore : Destinations("explore", "发现")
    data object Settings : Destinations("settings", "设置")
    data object Login : Destinations("login", "登录")
    data object Debug : Destinations("debug", "调试")
    data object About : Destinations("about", "关于")
    data object FolderDetail : Destinations("folder_detail/{folderId}", "收藏夹")
    data object FavoritesManage : Destinations("favorites_manage", "管理收藏夹")
}
