package com.example.billlens.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.takePicture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.billlens.domain.scan.ScanNavigationEvent
import com.example.billlens.domain.scan.ScanReceiptViewModel
import com.example.billlens.ui.navigation.NavigationScreens
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.regex.Pattern
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    navController: NavController,
    viewModel: ScanReceiptViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ascolta gli eventi di navigazione "one-shot" dal ViewModel
    LaunchedEffect(Unit) {
        viewModel.onNewScanRequested()
        viewModel.navigationEvent.collect { event ->
            when (event) {
                // Ora navighiamo a una route semplice, senza passare argomenti.
                is ScanNavigationEvent.NavigateToTextResult -> {
                    navController.navigate(NavigationScreens.TextResult.route)
                }
            }
        }
    }


    // Launcher per richiedere il permesso della fotocamera in modo sicuro
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> viewModel.onPermissionResult(isGranted) }
    )

    // All'avvio del Composable, controlla se il permesso è già stato concesso.
    // Se non lo è, lo richiede.
    LaunchedEffect(key1 = true) {
        val isGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scansiona Scontrino") },
                navigationIcon = {
                    IconButton(onClick = {navController.popBackStack()}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.hasCameraPermission) {
                // Box che contiene l'anteprima della fotocamera
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)) // Angoli arrotondati
                        .background(Color.Black)
                ) {
                    if (uiState.frozenBitmap != null) {
                        Image(
                            bitmap = uiState.frozenBitmap!!.asImageBitmap(),
                            contentDescription = "Ultimo frame catturato",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CameraPreview(
                            onTextRecognized = viewModel::onTextRecognized,
                            setCaptureFunction = viewModel::setCaptureFunction,
                            onAnalysisStarted = viewModel::onAnalysisStarted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottone di scatto sotto l'anteprima
                Button(
                    onClick = viewModel::onTakePhotoClick,
                    enabled = !uiState.isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Scatta Foto",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Scatta Foto")
                    }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Permesso fotocamera non concesso.")
                }
            }
        }
            // Overlay di caricamento mostrato quando isAnalyzing è true
            if (uiState.isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onTextRecognized: (String) -> Unit,
    setCaptureFunction: (() -> Unit) -> Unit,
    onAnalysisStarted: (Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)

            val captureFunc: () -> Unit = {
                imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        Log.d("CameraPreview", "Foto scattata con successo!")

                        // --- MODIFICA CHIAVE QUI ---
                        // 1. Converti l'ImageProxy in un Bitmap ruotato correttamente.
                        val bitmap = imageProxyToBitmap(imageProxy)

                        // 2. Notifica la UI che l'analisi è iniziata, passando il Bitmap
                        //    dell'immagine catturata per "congelare" la UI.
                        onAnalysisStarted(bitmap)

                        // 3. Esegui l'analisi sull'ImageProxy originale (o sul bitmap).
                        //    Passare imageProxy è più efficiente se analyzeImage lo supporta.
                        analyzeImage(imageProxy, onTextRecognized)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraPreview", "Errore nello scatto", exception)
                        onTextRecognized("Errore nello scatto: ${exception.message}")
                    }
                })
            }
            setCaptureFunction(captureFunc)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Collegamento fallito", exc)
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Converte un ImageProxy (da CameraX) in un Bitmap, applicando la rotazione corretta.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val image = imageProxy.image!!
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Applica la rotazione per visualizzare l'immagine correttamente
    val matrix = Matrix()
    matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}


// Funzione helper per analizzare una singola immagine
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeImage(imageProxy: ImageProxy, onTextRecognized: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // 1. Riorganizza il testo in righe logiche
                val organizedText = reorganizeTextByRow(visionText)

                // 2. Estrai i dettagli specifici
                val detailsString = extractReceiptDetails(organizedText)

                // 4. Passa alla UI il testo completo e riorganizzato
                onTextRecognized(detailsString)
            }
            .addOnFailureListener { e ->
                Log.e("TextAnalyzer", "Riconoscimento testo fallito", e)
                onTextRecognized("Analisi fallita: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close() // Assicurati di chiudere il proxy anche in caso di errore
        onTextRecognized("Errore: Impossibile leggere l'immagine.")
    }
}


/**
 * Riorganizza il testo riconosciuto in righe logiche basandosi sulla posizione verticale.
 * @param visionText Il risultato dell'analisi di ML Kit.
 * @return Una lista di stringhe, dove ogni stringa è una riga ricostruita.
 */
private fun reorganizeTextByRow(visionText: Text): List<String> {
    val allLines = visionText.textBlocks.flatMap { it.lines }
    val logicalRows = mutableListOf<MutableList< Text.Line>>()

    if (allLines.isEmpty()) return emptyList()

    for (line in allLines) {
        val lineCenterY = line.boundingBox?.exactCenterY() ?: continue
        var foundRow = false

        for (row in logicalRows) {
            val rowCenterY = row.first().boundingBox?.exactCenterY() ?: continue
            val tolerance = (row.first().boundingBox?.height() ?: 20) * 0.7

            if (kotlin.math.abs(lineCenterY - rowCenterY) < tolerance) {
                row.add(line)
                foundRow = true
                break
            }
        }

        if (!foundRow) {
            logicalRows.add(mutableListOf(line))
        }
    }

    logicalRows.sortBy { it.first().boundingBox?.top }

    return logicalRows.map { row ->
        row.sortBy { it.boundingBox?.left }
        row.joinToString(separator = " ") { it.text }
    }
}

/**
 * Estrae informazioni chiave (totale, data, nome negozio) da una lista di righe di testo.
 * @param organizedLines Il testo dello scontrino già organizzato per righe.
 * @return Un oggetto ReceiptDetails con le informazioni trovate.
 */
private fun extractReceiptDetails(organizedLines: List<String>): String {
    var storeName: String? = null
    var date: String? = null
    var total: String? = null

    // --- 1. Estrazione Nome Negozio (Logica con rimozione suffissi) ---
    // Lista di suffissi societari comuni da rimuovere.
    val corporateSuffixes = listOf("srl", "s.r.l", "spa", "s.p.a", "s.a.s", "sas", "snc", "s.n.c")

    // Cerca tra le prime 5 righe la riga che termina con uno dei suffissi.
    val companyLine = organizedLines
        .take(5)
        .firstOrNull { line ->
            val lowerLine = line.lowercase()
            // Usa 'contains' invece di 'endsWith' per una maggiore flessibilità.
            corporateSuffixes.any { suffix -> lowerLine.contains(suffix) }
        }

    if (companyLine != null) {
        // Se abbiamo trovato la riga candidata, la "puliamo".
        var cleanedLine = companyLine as String
        corporateSuffixes.forEach { suffix ->
            // Cerca l'indice del suffisso nella riga
            val suffixIndex = cleanedLine.lowercase().indexOf(suffix)
            if (suffixIndex != -1) {
                // Se trovato, taglia la stringa fino a quel punto
                cleanedLine = cleanedLine.substring(0, suffixIndex).trim()
            }
        }
        // Spesso rimangono parole come "a socio unico", rimuoviamole se presenti
        val finalIndex = cleanedLine.lowercase().indexOf(" a socio")
        if (finalIndex != -1) {
            cleanedLine = cleanedLine.substring(0, finalIndex).trim()
        }
        storeName = cleanedLine
    } else {
        // Fallback: se nessuna riga contiene un suffisso, cerchiamo tra le prime righe
        // quella che è più lunga e composta principalmente da lettere maiuscole.
        storeName = organizedLines
            .take(3)
            .filter { it.length > 3 && it.count { char -> char.isUpperCase() } > it.length / 2 }
            .maxByOrNull { it.length }
            ?: null // Fallback finale
    }

    // Regex per cercare importi monetari (es. 12,34 o 12.34)
    val amountPattern = Pattern.compile("""(\d+([.,]\d{2}))""")
    // Regex per cercare date (es. 12/03/2024 o 12-03-24)
    val datePattern = Pattern.compile("""(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})""")

    var maxAmount = 0.0

    for (line in organizedLines) {
        val lowerLine = line.lowercase()

        // --- 2. Estrazione Data ---
        if (date == null) {
            val dateMatcher = datePattern.matcher(line)
            if (dateMatcher.find()) {
                date = dateMatcher.group(1)
            }
        }

        // --- 3. Estrazione Totale (euristica: cerca "totale" e il numero più grande) ---
        val amountMatcher = amountPattern.matcher(line)
        while (amountMatcher.find()) {
            val amountString = amountMatcher.group(1).replace(',', '.')
            val currentAmount = amountString.toDoubleOrNull() ?: 0.0

            // Se la riga contiene "totale", questo è quasi sicuramente il nostro totale.
            if (lowerLine.contains("totale") || lowerLine.contains("total")) {
                total = amountString

            }

            // Altrimenti, teniamo traccia dell'importo più alto trovato.
            // Spesso il totale è il numero più grande sullo scontrino.
            if (currentAmount > maxAmount) {
                maxAmount = currentAmount
            }
        }
    }

    // Se non abbiamo trovato un totale esplicito, usiamo il valore più alto che abbiamo trovato.
    if (total == null && maxAmount > 0) {
        total = maxAmount.toString()
    }

    // Costruisce la stringa di risultato come richiesto
    val fullText = organizedLines.joinToString("\n")
    return "--- Dettagli Estratti ---\nNegozio: ${storeName ?: "Non trovato"}\nData: ${date ?: "Non trovata"}\nTotale: ${total ?: "Non trovato"}\n\n--- Testo Completo ---\n$fullText"
}
