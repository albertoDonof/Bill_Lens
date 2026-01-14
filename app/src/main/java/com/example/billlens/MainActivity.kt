package com.example.billlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.billlens.ui.home.HomeScreen
import com.example.billlens.ui.home.MainScreen
import com.example.billlens.ui.navigation.AppNavigation
import com.example.billlens.ui.navigation.NavigationScreens
import com.example.billlens.ui.theme.BillLensTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillLensTheme {
                // Determina la route di partenza
                val startDestination = if (Firebase.auth.currentUser != null) {
                    "main_graph" // Utente già loggato
                } else {
                    NavigationScreens.Login.route // Utente non loggato
                }

                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BillLensTheme {
        Greeting("Android")
    }
}