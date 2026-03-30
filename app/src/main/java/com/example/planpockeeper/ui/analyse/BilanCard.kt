package com.example.planpockeeper.ui.analyse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Icon(
                imageVector = if (isBudgetOver) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isBudgetOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
                Text(" Budget dépassé ? ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (isBudgetOver) "OUI" else "NON",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBudgetOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            if (overCategories.isNotEmpty()) {
                Text("Catégories dépassées :", style = MaterialTheme.typography.bodyMedium)
                overCategories.forEach { cat ->
                    Text("• ${cat.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
                }
            }
        Row {
            Text(
                if (isBudgetOver) "Excès" else "Économie",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                " sur le budget : ${if (isBudgetOver) "-" else "+"}${kotlin.math.abs(totalDiff).toInt()}€",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        }
}
