package com.example.billlens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.name

// Rinominato da Screen a NavigationScreens
sealed class NavigationScreens(val route: String, val title: String, val icon: ImageVector) {
    data object Home : NavigationScreens("home", "Home", Icons.Default.Home)
    data object Stats : NavigationScreens("stats", "Statistiche", Icons.Default.Star)
    data object Settings : NavigationScreens("settings", "Settings", Icons.Default.Settings)
    data object ScanReceipt : NavigationScreens("scan_receipt", "Scan", Icons.Default.AccountCircle)

    data object TextResult : NavigationScreens("text_result", "Scan Result" /*...*/, Icons.Default.Star)

    data object Login : NavigationScreens("login", "Login", Icons.Default.Home)
}