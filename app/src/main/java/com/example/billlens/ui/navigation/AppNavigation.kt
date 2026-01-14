package com.example.billlens.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.billlens.domain.scan.ScanReceiptViewModel
import com.example.billlens.ui.home.HomeScreen
import com.example.billlens.ui.login.LoginScreen
import com.example.billlens.ui.scan.ScanReceiptScreen
import com.example.billlens.ui.scan.TextResultScreen
import com.example.billlens.ui.settings.SettingsScreen

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

const val SCAN_GRAPH_ROUTE = "scan_graph"
/**
 * Definisce il grafo di navigazione e associa ogni route a un Composable.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(NavigationScreens.Login.route) {
                LoginScreen(
                    onSignInSuccess = {
                        // Dopo il login, vai alla home e pulisci lo stack
                        navController.navigate(NavigationScreens.Home.route) {
                            popUpTo(NavigationScreens.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                )
        }

        navigation(
            startDestination = NavigationScreens.Home.route,
            route = "main_graph"
        ){
            composable(NavigationScreens.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(NavigationScreens.Stats.route) {
                StatsScreen(navController = navController) // Sostituisci con la tua vera schermata
            }
            composable(NavigationScreens.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    onLogout = {
                        // Naviga al Login e cancella tutto lo stack precedente
                        navController.navigate(NavigationScreens.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        navigation(
            startDestination = NavigationScreens.ScanReceipt.route,
            route = SCAN_GRAPH_ROUTE
        ) {
            composable(NavigationScreens.ScanReceipt.route) { backStackEntry ->
                // Il ViewModel viene associato al grafo genitore "scan_graph"
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(SCAN_GRAPH_ROUTE)
                }
                val scanViewModel = hiltViewModel<ScanReceiptViewModel>(parentEntry)

                ScanReceiptScreen(
                    navController = navController,
                    viewModel = scanViewModel
                )
            }

            composable(NavigationScreens.TextResult.route) { backStackEntry ->
                // Anche qui, il ViewModel viene recuperato dal grafo genitore "scan_graph"
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(SCAN_GRAPH_ROUTE)
                }
                val scanViewModel = hiltViewModel<ScanReceiptViewModel>(parentEntry)

                TextResultScreen(
                    scanViewModel = scanViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSuccess = {
                        // Torna alla Home e rimuove tutto il flusso di scansione dalla cronologia
                        navController.navigate(NavigationScreens.Home.route) {
                            popUpTo(SCAN_GRAPH_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}