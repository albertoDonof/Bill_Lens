package com.example.billlens.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.billlens.ui.navigation.AppBottomNavigationBar
import com.example.billlens.ui.navigation.AppNavigation
import com.example.billlens.ui.navigation.NavigationScreens


/**
 * Il Composable principale dell'app che contiene lo Scaffold,
 * la Bottom Navigation Bar e l'host di navigazione.
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()


    AppNavigation(
        navController = navController,
        startDestination = NavigationScreens.Home.route
    )
}