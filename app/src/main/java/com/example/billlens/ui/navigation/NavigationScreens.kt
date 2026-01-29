package com.example.billlens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.name
import com.example.billlens.R

// Rinominato da Screen a NavigationScreens
sealed class NavigationScreens(val route: String, val title: String, val iconId: Int) {
    data object Home : NavigationScreens("home", "Home", R.drawable.baseline_home_filled_24)
    data object Stats : NavigationScreens("stats", "Analytics", R.drawable.outline_analytics_24)
    data object AllExpenses : NavigationScreens("all_expenses", "All Expenses", R.drawable.outline_list_alt_24)

    data object ExpenseDetail : NavigationScreens("expense_detail/{expenseId}", "Detail", R.drawable.outline_category_24)

    data object Settings : NavigationScreens("settings", "Settings", R.drawable.baseline_settings_24)
    data object ScanReceipt : NavigationScreens("scan_receipt", "Scan", R.drawable.outline_category_24)

    data object TextResult : NavigationScreens("text_result", "Scan Result" /*...*/, R.drawable.outline_category_24)

    data object Login : NavigationScreens("login", "Login", R.drawable.outline_category_24)
}