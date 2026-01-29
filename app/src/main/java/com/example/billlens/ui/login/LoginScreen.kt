package com.example.billlens.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billlens.R
import com.example.billlens.domain.login.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential


@Composable
fun LoginScreen(
    onSignInSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    /*
    // --- NEW: Google Sign-In Client with Scopes ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            // --- Add the required scopes here ---
            .requestScopes(
                Scope("https://www.googleapis.com/auth/spreadsheets"),
                Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // --- NEW: Launcher for the Google Sign-In Activity ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task, viewModel) {
                isSigningIn = false
            }
        }
    )
    */

    // Gestione automatica del successo del login
    LaunchedEffect(state.isSignInSuccessful) {
        if (state.isSignInSuccessful) {
            onSignInSuccess()
            viewModel.resetState()
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSigningIn) {
            CircularProgressIndicator()
        } else {
            // 1. Aggiungiamo il titolo dell'app
            Text(
                text = "Bill Lens",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.size(64.dp)) // Spazio tra il titolo e il bottone

            // 2. Modifichiamo il bottone per includere l'icona
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSigningIn = true
                        performSignIn(context, viewModel) {
                            isSigningIn = false
                        }
                    }
                }
            ) {
                // Aggiungiamo l'icona di Google che abbiamo importato
                Icon(
                    painter = painterResource(id = R.drawable.google_color_svgrepo_com),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    tint = androidx.compose.ui.graphics.Color.Unspecified // Usa i colori originali del logo
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing)) // Spazio standard tra icona e testo
                Text("Sign in with Google")
            }
        }

        // Mostra l'errore sotto il bottone/loader
        state.signInError?.let { error ->
            // Aggiungiamo un po' di spazio per non attaccare l'errore al bottone
            Spacer(modifier = Modifier.size(16.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Esegue il flusso di login usando il Credential Manager, come raccomandato da Google.
 * Richiede sia un ID Token per Firebase sia i permessi per Google Sheets/Drive.
 */
private suspend fun performSignIn(
    context: android.content.Context,
    viewModel: LoginViewModel,
    onFinished: () -> Unit
) {
    val activity = context as? Activity
    if (activity == null) {
        viewModel.onSignInResult(false, "Login failed: Context is not an Activity.")
        onFinished()
        return
    }

    // 1. Prepara la richiesta per l'ID Token di Google (per Firebase)
    // Usiamo un nonce per maggiore sicurezza contro attacchi di tipo "replay".
    val googleIdOption: GetGoogleIdOption =
        GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setAutoSelectEnabled(true)
        .build()

    // 2. Crea la richiesta al Credential Manager
    val request: GetCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credentialManager = CredentialManager.create(context)

    try {
        // 3. Esegui la richiesta, che mostrerà il popup "One Tap" di Google
        val result = credentialManager.getCredential(activity, request)
        handleSignInResult(result, viewModel)
    } catch (e: GetCredentialException) {
        Log.e("LoginScreen", "GetCredentialException: ${e.message}", e)
        viewModel.onSignInResult(false, "Sign-in canceled or failed.")
    } finally {
        onFinished()
    }
}

/**
 * Gestisce la risposta dal Credential Manager e autentica l'utente con Firebase.
 */
private fun handleSignInResult(result: GetCredentialResponse, viewModel: LoginViewModel) {
    val credential = result.credential
    if (credential is GoogleIdTokenCredential) {
        val googleIdToken = credential.idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                Log.d("LoginScreen", "Firebase Sign-In Successful.")
                viewModel.onSignInResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("LoginScreen", "Firebase Sign-In Failed.", e)
                viewModel.onSignInResult(false, "Firebase authentication error.")
            }
    } else {
        Log.w("LoginScreen", "Received an unsupported credential type.")
        viewModel.onSignInResult(false, "Unsupported credential type.")
    }
}