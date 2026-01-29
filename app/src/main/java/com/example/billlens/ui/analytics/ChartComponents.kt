package com.example.billlens.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.billlens.domain.analytics.CategorySpending
import kotlin.math.min
import android.graphics.Paint
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.billlens.domain.analytics.MonthlySpending
import kotlin.math.cos
import kotlin.math.sin

/**
 * Un set di colori predefiniti e accattivanti da ciclare per i grafici.
 */
val chartColors = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39)
)

/**
 * Disegna un grafico a torta (Pie Chart) usando il Canvas.
 * @param data La lista dei dati di spesa per categoria.
 */
@Composable
fun PieChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    // Animiamo il progresso del disegno del grafico da 0 a 1.
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f) // Resetta l'animazione se i dati cambiano
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }


    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val circleSize = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val strokeWidth = circleSize * 0.2f
        val radius = (circleSize / 2) - (strokeWidth / 2)

        val textSizeSp = 14.sp
        // Oggetto Paint per disegnare il testo
        val textPaint = remember {
            Paint().apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
            }
        }

        Canvas(modifier = Modifier.size(with(LocalDensity.current) { circleSize.toDp() })) {
            textPaint.textSize = textSizeSp.toPx()
            var startAngle = -90f

            data.forEachIndexed { index, categorySpending ->
                val sweepAngle = categorySpending.percentage * 3.6f
                val animatedSweepAngle = sweepAngle * animationProgress.value

                // Disegna l'arco del grafico
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // --- LOGICA PER LE ETICHETTE ---
                // Calcola l'angolo a metà del settore per posizionare l'etichetta
                val medianAngleRad = Math.toRadians((startAngle + animatedSweepAngle / 2).toDouble())

                // Calcola le coordinate (x, y) sul cerchio
                val x = center.x + radius * cos(medianAngleRad).toFloat()
                val y = center.y + radius * sin(medianAngleRad).toFloat()

                // Disegna il testo solo se la percentuale è abbastanza grande
                if (categorySpending.percentage > 5) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${categorySpending.percentage.toInt()}%",
                        x,
                        y - (textPaint.descent() + textPaint.ascent()) / 2, // Centra verticalmente
                        textPaint
                    )
                }

                startAngle += sweepAngle
            }
        }
    }
}


/**
 * Disegna un grafico a barre (Bar Chart) con etichette sotto ogni barra.
 */
@Composable
fun BarChart(
    data: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }
    val textSizeSp = 12.sp
    // Paint per il testo sotto le barre
    val textPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
        }
    }

    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
        textPaint.textSize = textSizeSp.toPx()
        textPaint.color = labelColor

        // --- MODIFICA 2: Calcola il massimo basandosi sui nuovi dati ---
        val maxSpending = data.maxOfOrNull { it.totalAmount }?.toFloat() ?: 0f
        if (maxSpending == 0f) return@Canvas

        val barCount = data.size
        val spaceBetweenBars = 16.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * spaceBetweenBars) / barCount
        val canvasHeight = size.height - 24.dp.toPx() // Lascia spazio sotto per le etichette

        data.forEachIndexed { index, monthlySpending -> // <-- USA I NUOVI DATI
            // --- MODIFICA 3: Calcola l'altezza usando il nuovo oggetto ---
            val barHeight = (monthlySpending.totalAmount.toFloat() / maxSpending) * canvasHeight * animationProgress.value
            val startX = index * (barWidth + spaceBetweenBars)

            // Disegna la barra (questa parte non cambia)
            drawRect(
                color = chartColors[index % chartColors.size],
                topLeft = Offset(x = startX, y = canvasHeight - barHeight),
                size = Size(width = barWidth, height = barHeight)
            )

            // --- MODIFICA 4: Disegna l'etichetta del mese ---
            drawContext.canvas.nativeCanvas.drawText(
                monthlySpending.monthLabel, // Usa l'etichetta del mese (es. "Gen", "Feb")
                startX + barWidth / 2, // Centra orizzontalmente rispetto alla barra
                canvasHeight + 20.dp.toPx(), // Posiziona sotto la barra
                textPaint
            )
        }
    }
}