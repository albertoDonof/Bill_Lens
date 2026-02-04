package com.example.billlens.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.billlens.domain.analytics.AnalyticsViewModel
import com.example.billlens.domain.analytics.CategorySpending
import com.example.billlens.domain.analytics.TimeFilter
import com.example.billlens.ui.navigation.AppBottomNavigationBar
import com.example.billlens.utils.CurrencyFormatter
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.style.TextOverflow
import com.example.billlens.domain.analytics.AnalyticsUiState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.example.billlens.domain.analytics.MapUiState
import com.example.billlens.ui.components.AutosizeText
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberTileOverlayState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("By Category", "By Location")

    // Lancia la geocodifica quando l'utente seleziona la tab della mappa per la prima volta
    LaunchedEffect(selectedTabIndex , selectedFilter) {
        if (selectedTabIndex == 1) {
            viewModel.loadMarkersData()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Statistics & Analysis") })
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TabRow per la selezione
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Contenuto che cambia in base alla tab selezionata
            when (selectedTabIndex) {
                0 -> CategoryAnalyticsContent(uiState, selectedFilter, viewModel::onTimeFilterChanged)
                1 -> LocationAnalyticsContent(uiState.mapState)
            }
        }
    }
}

// Contenuto per la prima tab (quella che avevi già)
@Composable
fun CategoryAnalyticsContent(
    uiState: AnalyticsUiState,
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Selettore del Filtro Temporale
            item {
                TimeFilterSelection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected
                )
            }

            // 2. Placeholder per il Grafico a Torta (Pie Chart)
            item {
                Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.categorySpendings.isNotEmpty()) {
                    PieChart(
                        data = uiState.categorySpendings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 20.dp, bottom = 8.dp)
                    )
                } else {
                    // Mostra un placeholder se non ci sono dati
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data for chart", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 3. Lista delle Categorie con i relativi totali
            item {
                Text("Category Totals", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.categorySpendings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No spending data for this period.")
                    }
                }
            } else {
                items(
                    uiState.categorySpendings,
                    key = { it.categoryName }) { categorySpending ->
                    // Calcoliamo l'indice della spesa corrente nella lista
                    val index = uiState.categorySpendings.indexOf(categorySpending)
                    // Usiamo lo stesso calcolo del PieChart per ottenere il colore corretto
                    val color = chartColors[index % chartColors.size]
                    CategorySpendingItem(item = categorySpending , color = color)
                    HorizontalDivider()
                }
            }

            // 4. Placeholder per il Grafico a Barre (Bar Chart)
            item {
                Text("Spending Over Time", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.monthlySpendings.isNotEmpty()) {
                    // NOTA: Stiamo riutilizzando gli stessi dati, ma in un'app reale
                    // qui useresti dati aggregati per mese o per giorno.
                    BarChart(
                        data = uiState.monthlySpendings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data for chart", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// NUOVO Composable per la tab della mappa
@Composable
fun LocationAnalyticsContent(mapState: MapUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        val italyCameraState = rememberCameraPositionState {
            // Posizione iniziale della mappa, centrata sull'Italia
            position = CameraPosition.fromLatLngZoom(LatLng(41.9, 12.5), 5f)
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = italyCameraState
        ) {
            BasicMarkersMapContent(mapState = mapState)
        }

        // Il loader per la geocodifica rimane lo stesso,
        // verrà mostrato mentre l'app sta ottenendo le coordinate.
        if (mapState.isGeocoding) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@GoogleMapComposable
fun BasicMarkersMapContent(
    mapState: MapUiState,
    onMarkerClick: (Marker) -> Boolean = { false },
){
    mapState.markers.forEach { expenseMarker ->
        Marker(
            // MarkerState definisce la posizione del marker.
            state = rememberMarkerState(position = expenseMarker.location),

            // Titolo che appare quando l'utente clicca sul marker.
            // In futuro potresti passare più dati per mostrare il nome del negozio.
            title = expenseMarker.title,

            // Snippet (sottotitolo) opzionale.
            snippet = expenseMarker.snippet,
            onClick = onMarkerClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFilterSelection(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    // Usa i SegmentedButton per una UI moderna e compatta
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TimeFilter.entries.forEachIndexed { index,filter ->
            SegmentedButton(
                modifier = Modifier.weight(1f),
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TimeFilter.entries.size
                )
            ) {
                AutosizeText(
                    text= filter.displayName,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CategorySpendingItem(item: CategorySpending, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- MODIFICA 2: Aggiungi l'indicatore colorato ---
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, shape = CircleShape) // Disegna un cerchio con il colore passato
        )

        // Potresti aggiungere un'icona della categoria qui in futuro
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${"%.1f".format(item.percentage)}% of total", // Formatta la percentuale con una cifra decimale
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Text(
            text = CurrencyFormatter.formatBigToString(item.totalAmount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}