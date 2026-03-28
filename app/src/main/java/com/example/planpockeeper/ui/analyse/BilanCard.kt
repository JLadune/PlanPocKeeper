package com.example.planpockeeper.ui.analyse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun BilanCard(
    totalPlanned: Double,
    totalSpent: Double,
    categories: List<BudgetSummaryItem>,
    modifier: Modifier = Modifier
) {
    val isBudgetOver = totalSpent > totalPlanned
    val overCategories = categories.filter { it.spent > it.planned }
    val totalDiff = totalSpent - totalPlanned
        Column(modifier = Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text("Budget dépassé ? ", style = MaterialTheme.typography.bodyMedium)
                Text(if (isBudgetOver) "OUI" else "NON", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            if (overCategories.isNotEmpty()) {
                Text("Catégories dépassées :", style = MaterialTheme.typography.bodyMedium)
                overCategories.forEach { cat ->
                    Text("• ${cat.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Row {
                Text("Économie/Excès sur le budget : ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${if (isBudgetOver) "-" else "+"}${kotlin.math.abs(totalDiff).toInt()}€",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
}
