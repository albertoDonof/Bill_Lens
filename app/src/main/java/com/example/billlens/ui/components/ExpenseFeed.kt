package com.example.billlens.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import com.example.billlens.data.model.Expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.* // Esempio
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.billlens.utils.CurrencyFormatter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Un Composable riutilizzabile che mostra una singola voce di spesa.
 *
 * @param category La categoria della spesa (es. "Cibo", "Trasporti").
 * @param notes Note opzionali sulla spesa.
 * @param amount L'importo della spesa.
 * @param date La data della spesa.
 * @param icon L'icona che rappresenta la categoria.
 * @param modifier Modificatori per personalizzare il layout.
 */
@Composable
fun ExpenseItem(
    category: String,
    notes: String?,
    amount: BigDecimal,
    date: Date,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona della categoria
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Dettagli della spesa (Categoria e Note)
        Column(modifier = Modifier.weight(1f)) {
            if (!notes.isNullOrBlank()) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Importo e Data
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${CurrencyFormatter.formatBigToString(amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseItemPreview() {
    ExpenseItem(
        category = "Cibo",
        notes = "Pranzo con colleghi",
        amount = BigDecimal(25.50),
        date = Date(),
        icon = Icons.Rounded.AccountCircle
    )
}


/**
 * Mostra un feed di spese recenti in una lista scorrevole.
 *
 * @param expenses La lista di oggetti Expense da visualizzare.
 * @param modifier Modificatori per personalizzare il layout.
 */
@Composable
fun ExpensesFeed(
    expenses: List<Expense>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(expenses, key = { it.id }) { expense ->
            ExpenseItem(
                category = expense.category,
                notes = expense.notes,
                amount = expense.totalAmount,
                date = expense.receiptDate,
                icon = getIconForCategory(expense.category) // Funzione helper per l'icona
            )
            HorizontalDivider()
        }
    }
}

// Funzione helper per mappare una categoria a un'icona
private fun getIconForCategory(category: String): ImageVector {
    return when (category.lowercase()) {
        "cibo", "food" -> Icons.Default.AccountBox
        "trasporti", "transport" -> Icons.Default.AccountBox
        "shopping" -> Icons.Default.ShoppingCart
        "casa", "home" -> Icons.Default.Home
        "svago", "entertainment" -> Icons.Default.AccountBox
        else -> Icons.Default.AccountBox
    }
}