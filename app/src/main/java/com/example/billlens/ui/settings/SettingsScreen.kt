package com.example.billlens.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.billlens.domain.settings.SettingsViewModel
import com.example.billlens.ui.navigation.AppBottomNavigationBar
import com.google.android.gms.auth.api.identity.AuthorizationClient
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.identity.Identity
import com.example.billlens.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lastSyncTime by viewModel.lastSyncRelativeTime.collectAsStateWithLifecycle()


    var showSignOutDialog by remember { mutableStateOf(false) }



    // Ottieni il client di autorizzazione tramite Identity.getAuthorizationClient()
    val authorizationClient: AuthorizationClient = remember {
        Identity.getAuthorizationClient(context)
    }

    // 2. Crea il launcher per il risultato
    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Estrai il risultato e passalo al ViewModel
        val authorizationResult = authorizationClient.getAuthorizationResultFromIntent(result.data)
        viewModel.onAuthorizationResult(authorizationResult)
    }

    // --- NEW: Observe the request from the ViewModel ---
    LaunchedEffect(uiState.authorizationRequest) {
        uiState.authorizationRequest?.let { request ->
            authorizationClient.authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                        authorizationLauncher.launch(intentSenderRequest)
                    } else {
                        // The user has already granted permissions, we can proceed directly
                        viewModel.onAuthorizationResult(result)
                    }
                    // Reset the request in the ViewModel so this doesn't run again on recomposition
                    viewModel.authorizationRequestConsumed()
                }
                .addOnFailureListener { e ->
                    Log.e("SettingsScreen", "Authorization failed to start", e)
                    viewModel.authorizationRequestConsumed() // Also reset on failure
                }
        }
    }

    // Gestione Snackbar per feedback
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Confirm Sign Out?") },
            text = {
                Text(
                    "You will be logged out of your account. " +
                            "You'll need to sign in again to see your expenses.",
                    textAlign = TextAlign.Left
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Azione di logout effettiva
                        viewModel.signOut()
                        onLogout()
                        showSignOutDialog = false // Chiudi il dialogo
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sezione 1: Profile
            item {
                SectionHeader("Account")
                ProfileCard(
                    name = uiState.userName ?: "User",
                    email = uiState.userEmail ?: "",
                    profilePic = uiState.profilePictureUrl
                )
            }

            // Sezione 2: Data & Sync
            item {
                SectionHeader("Data Management")
                SettingsRow(
                    drawableVector = R.drawable.outline_cloud_sync_24,
                    title = "Sync Data",
                    subtitle = if (uiState.isSyncing) "Syncing..." else "Last sync: $lastSyncTime",
                    enabled = !uiState.isSyncing,
                    onClick = { viewModel.triggerSync() }
                )
                // IL NUOVO TASTO GOOGLE SHEETS
                SettingsRow(
                    drawableVector = R.drawable.outline_file_export_24,
                    title = "Export to Google Sheets",
                    subtitle = if (uiState.isExporting) "Processing..." else "Create a spreadsheet in your Drive",
                    onClick = {
                        // --- CORRECT: Just tell the ViewModel to start the flow ---
                        viewModel.startAuthorizationFlow()
                    },

                )
                val context = LocalContext.current

                if (uiState.lastExportUrl != null) {
                    SettingsRow(
                        drawableVector = R.drawable.outline_dataset_linked_24,
                        title = "Open Last Export",
                        subtitle = "View the spreadsheet in your browser",
                        onClick = {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(uiState.lastExportUrl!!))
                            context.startActivity(intent)
                        },
                        trailingContent = {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    )
                }
            }

            // Sezione 3: Logout
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        showSignOutDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Bill Lens v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun ProfileCard(name: String, email: String, profilePic: String?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profilePic,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(email, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SettingsRow(
    drawableVector: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)?= null,
    enabled: Boolean = true
) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                painter = painterResource(id = drawableVector),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = trailingContent
    )
}