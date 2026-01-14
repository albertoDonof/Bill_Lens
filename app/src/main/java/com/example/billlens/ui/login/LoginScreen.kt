package com.example.billlens.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isSigningIn) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSigningIn = true
                        // Avvia il nuovo flusso di login combinato
                        performSignIn(context, viewModel) {
                            isSigningIn = false
                        }
                    }
                }
            ) {
                Text("Sign in with Google")
            }
        }

        state.signInError?.let { error ->
            Text(text = error, color = androidx.compose.ui.graphics.Color.Red)
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