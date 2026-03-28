package com.example.planpockeeper.ui.analyse

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun AnalyseCard(
    categories: List<BudgetSummaryItem>,
    modifier: Modifier = Modifier
) {
    val underCategories = categories.filter { it.spent < it.planned }
    val savingsPossible = underCategories.sumOf { (it.planned - it.spent) * 0.5 }
        Column(modifier = Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (savingsPossible > 0) {
                Row {
                    Text("Économie possible : ", style = MaterialTheme.typography.bodyMedium)
                    Text("${savingsPossible.toInt()}€", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text("Économie dans quelles catégories :", style = MaterialTheme.typography.bodyMedium)
                underCategories.forEach { cat ->
                    val saving = (cat.planned - cat.spent) * 0.5
                    Text("• ${cat.name} : ${saving.toInt()}€", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Text(
                    "Aucune économie réalisée, il faudrait revoir le budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
}