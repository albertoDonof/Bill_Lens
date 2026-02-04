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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.billlens.domain.analytics.MonthlySpending
import java.util.Collections
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.rotate // <-- Importa la funzione di rotazione corretta



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

    val monthLabelTextPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
        }
    }

    val axisLabelTextPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(modifier = modifier.fillMaxSize()) { // 'this' qui dentro è un DrawScope
        monthLabelTextPaint.color = labelColor
        axisLabelTextPaint.color = labelColor

        // --- SOLUZIONE 'toPx' ---
        // Ora possiamo chiamare .toPx() perché siamo dentro un DrawScope
        monthLabelTextPaint.textSize = 12.sp.toPx()
        axisLabelTextPaint.textSize = 14.sp.toPx()

        val maxSpending = data.maxOfOrNull { it.totalAmount }?.toFloat() ?: 0f

        val yAxisLabelOffset = 40.dp.toPx()
        val xAxisLabelOffset = 40.dp.toPx()

        val barCount = data.size
        val spaceBetweenBars = 16.dp.toPx()

        val drawingWidth = size.width - yAxisLabelOffset
        val drawingHeight = size.height - xAxisLabelOffset

        val barWidth = if (barCount > 0) (drawingWidth - (barCount - 1) * spaceBetweenBars) / barCount else 0f

        // --- SOLUZIONE 'save', 'rotate', 'restore' ---
        // Usiamo la funzione 'rotate' fornita dal DrawScope
        rotate(
            degrees = -90f,
            pivot = Offset(x = yAxisLabelOffset / 2, y = drawingHeight / 2) // Ruota attorno al centro dell'area dell'etichetta
        ) {
            // Disegniamo il testo ruotato. Le coordinate ora sono relative al pivot.
            drawContext.canvas.nativeCanvas.drawText(
                "Amount (€)",
                yAxisLabelOffset / 2, // x (ora verticale)
                drawingHeight / 2 + axisLabelTextPaint.textSize / 3, // y (ora orizzontale)
                axisLabelTextPaint
            )
        }
        // Il blocco 'rotate' gestisce save/restore implicitamente, quindi non servono chiamate manuali.

        if (maxSpending == 0f) return@Canvas

        data.forEachIndexed { index, monthlySpending ->
            val barHeight = (monthlySpending.totalAmount.toFloat() / maxSpending) * drawingHeight * animationProgress.value
            val startX = yAxisLabelOffset + index * (barWidth + spaceBetweenBars)

            drawRect(
                color = chartColors[index % chartColors.size],
                topLeft = Offset(x = startX, y = drawingHeight - barHeight),
                size = Size(width = barWidth, height = barHeight)
            )

            drawContext.canvas.nativeCanvas.drawText(
                monthlySpending.monthLabel,
                startX + barWidth / 2,
                drawingHeight + 20.dp.toPx(),
                monthLabelTextPaint
            )
        }
    }
}