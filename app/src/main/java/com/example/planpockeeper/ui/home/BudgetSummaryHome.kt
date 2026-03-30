package com.example.planpockeeper.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory

@Composable
fun BudgetSummaryHome(
    activeBudget: Budget?,
    budgetCategories: List<BudgetCategory>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (activeBudget != null) {
            val periodicityLabel = when {
                !activeBudget.periodical && activeBudget.endDate != null -> "Une fois"
                activeBudget.periodicity.startsWith("custom_") -> {
                    val days = activeBudget.periodicity.removePrefix("custom_").removeSuffix("j")
                    "Tous les $days jours"
                }
                else -> activeBudget.periodicity.replaceFirstChar { it.uppercase() }
            }
            Row {
                Text("Périodicité : ", style = MaterialTheme.typography.bodyMedium)
                Text(periodicityLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row {
                Text("Budget total : ", style = MaterialTheme.typography.bodyMedium)
                Text("${activeBudget.totalAmount.toInt()}€", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                "Aucun budget actif",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                "Catégorie",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Dépensé/Prévu",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider()

        budgetCategories.forEach { cat ->
            val isOver = cat.spentAmount > cat.plannedAmount

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        cat.categoryName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${cat.spentAmount.toInt()}/${cat.plannedAmount.toInt()}€",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}