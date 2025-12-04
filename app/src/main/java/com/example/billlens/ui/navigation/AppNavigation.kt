package com.example.billlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.billlens.ui.home.HomeScreen
import com.example.billlens.ui.scan.ScanReceiptScreen

// Placeholder per le altre schermate
// Aggiorna le altre schermate per accettare il NavController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController) {
    // Esempio di come anche questa schermata avrà il suo Scaffold
    Scaffold(
        topBar = { TopAppBar(title = { Text("Statistiche") }) },
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Contenuto della schermata Statistiche")
        }
    }
}


// Aggiorna le altre schermate per accettare il NavController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    // Esempio di come anche questa schermata avrà il suo Scaffold
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        bottomBar = { AppBottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Contenuto della schermata Settings")
        }
    }
}


/**
 * Definisce il grafo di navigazione e associa ogni route a un Composable.
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationScreens.Home.route,
    ) {
        composable(NavigationScreens.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(NavigationScreens.Stats.route) {
            StatsScreen(navController = navController) // Sostituisci con la tua vera schermata
        }
        composable(NavigationScreens.Settings.route) {
            SettingsScreen(navController = navController) // Sostituisci con la tua vera schermata
        }
        // Aggiungi qui altre destinazioni che non sono nella bottom bar,
        // ad esempio la schermata di dettaglio di una spesa.
        composable(NavigationScreens.ScanReceipt.route) {
            ScanReceiptScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}