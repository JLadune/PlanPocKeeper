package com.example.planpockeeper.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.model.Expense
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import com.example.planpockeeper.utils.CurrencyFormatter
import com.example.planpockeeper.utils.PreferencesManager

@Composable
fun ExpenseHistoryWidget(
    expenses: List<Expense>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val currency by prefsManager.currency.collectAsState(initial = "EUR")

    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                Text("Chargement des depenses...")
                return@Column
            }

            if (expenses.isEmpty()) {
                Text(
                    "Aucune depense enregistree.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            expenses.take(8).forEach { expense ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.categoryName, fontWeight = FontWeight.SemiBold)
                        if (expense.description.isNotBlank()) {
                            Text(
                                expense.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column {
                        Text(
                            CurrencyFormatter.format(expense.amount, currency),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            dateFormat.format(expense.date.toDate()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
