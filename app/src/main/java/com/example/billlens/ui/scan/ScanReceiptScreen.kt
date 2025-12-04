package com.example.billlens.ui.scan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.semantics.text
import com.example.billlens.ui.scan.CameraPreview
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    // Launcher per richiedere il permesso della fotocamera in modo sicuro
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    // All'avvio del Composable, controlla se il permesso è già stato concesso.
    // Se non lo è, lo richiede.
    LaunchedEffect(key1 = true) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scansiona Scontrino") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasPermission) {
                // Se abbiamo il permesso, mostriamo l'anteprima della fotocamera
                CameraPreview()
            } else {
                // Altrimenti, mostriamo un messaggio all'utente
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Permesso fotocamera non concesso.")
                }
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                // Configura il caso d'uso "Preview" per mostrare l'anteprima
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Seleziona la fotocamera posteriore
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Configura il caso d'uso "ImageAnalysis" per processare i frame
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, TextAnalyzer()) // Usa la nostra classe Analyzer
                    }

                try {
                    // Svincola tutto prima di ricollegare per evitare conflitti
                    cameraProvider.unbindAll()
                    // Collega i casi d'uso (Preview e ImageAnalysis) alla fotocamera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Collegamento dei casi d'uso fallito", exc)
                }

            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}


// Classe dedicata per l'analisi dell'immagine con ML Kit
private class TextAnalyzer : ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Per adesso, stampiamo il testo riconosciuto nel log.
                    // Puoi aggiungere un debounce qui per non loggare troppo frequentemente.
                    if (visionText.text.isNotBlank()) {
                        Log.d("TextAnalyzer", "Testo Riconosciuto: ${visionText.text}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TextAnalyzer", "Riconoscimento testo fallito", e)
                }
                .addOnCompleteListener {
                    // Chiudi sempre l'imageProxy al termine dell'analisi,
                    // altrimenti CameraX smetterà di fornire nuovi frame.
                    imageProxy.close()
                }
        }
    }
}