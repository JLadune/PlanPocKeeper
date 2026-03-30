package com.example.planpockeeper.ui.analyse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
@Composable
fun BudgetSummaryTable(
    categories: List<BudgetSummaryItem>,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                "Catégorie",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Dépensé/Prévu",
                modifier = Modifier.Companion.width(180.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Déficit",
                modifier = Modifier.width(60.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider()

        categories.forEach { item ->
            val plannedBarWidth = 100.dp
            val barCapWidth = 150.dp
            val ratio = if (item.planned > 0) (item.spent / item.planned).toFloat() else 0f
            val realBarWidth = (plannedBarWidth * ratio).coerceAtMost(barCapWidth)
            val isOver = item.spent > item.planned
            val diff = item.spent - item.planned

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Bordure gauche
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Column(
                        modifier = Modifier.width(180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Réel",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(36.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(realBarWidth)
                                    .height(10.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                                    .background(if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Prévu",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(36.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(plannedBarWidth)
                                    .height(10.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.width(60.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            if (isOver) "OUI" else "NON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${if (isOver) "-" else "+"}${abs(diff).toInt()}€",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

data class BudgetSummaryItem(
    val name: String,
    val planned: Double,
    val spent: Double
)