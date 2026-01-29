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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.billlens.utils.CurrencyFormatter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import com.example.billlens.R
import com.example.billlens.domain.expenses.ExpenseCategory

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
    expense: Expense,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determina l'icona corretta basandosi sul `displayName` della categoria
    val iconResId = when (expense.category) {
        ExpenseCategory.GROCERIES.displayName -> R.drawable.outline_shopping_cart_24
        ExpenseCategory.DINING.displayName -> R.drawable.outline_restaurant_24
        ExpenseCategory.TRANSPORT.displayName -> R.drawable.outline_transportation_24
        ExpenseCategory.SHOPPING.displayName -> R.drawable.outline_shopping_bag_24
        ExpenseCategory.HEALTH.displayName -> R.drawable.baseline_health_and_safety_24
        ExpenseCategory.HOME.displayName -> R.drawable.outline_garage_home_24
        ExpenseCategory.ENTERTAINMENT.displayName -> R.drawable.outline_books_movies_and_music_24
        ExpenseCategory.PERSONAL_CARE.displayName -> R.drawable.outline_self_care_24
        ExpenseCategory.EDUCATION.displayName -> R.drawable.baseline_school_24
        else -> R.drawable.outline_category_24 // Icona di default per "Miscellaneous" o categorie sconosciute
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable{ onItemClick(expense.id) }
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
                painter = painterResource(id = iconResId),
                contentDescription = expense.category,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Dettagli della spesa (Categoria e Note)
        Column(modifier = Modifier.weight(1f)) {
            if (!expense.notes.isNullOrBlank()) {
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Importo e Data
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CurrencyFormatter.formatBigToString(expense.totalAmount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(expense.receiptDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseItemPreview() {
    val sampleExpense = Expense(
        notes = "Weekly Groceries",
        category = ExpenseCategory.GROCERIES.displayName, // Usa l'enum per la preview
        totalAmount = BigDecimal("55.20"),
        receiptDate = Date(),
        // Altri campi non necessari per la preview di questo componente
        storeLocation = "Supermarket",
        insertionDate = Date(),
        lastUpdated = Date()
    )
    ExpenseItem(expense = sampleExpense, onItemClick = {})
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
    onExpenseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 80.dp) // Aggiunto padding in basso
    ) {
        items(expenses, key = { it.id }) { expense ->
            // Ora basta passare l'oggetto expense
            ExpenseItem(
                expense = expense,
                onItemClick = onExpenseClick
                )
            HorizontalDivider()
        }
    }
}
