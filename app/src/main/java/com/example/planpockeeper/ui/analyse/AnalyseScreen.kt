package com.example.planpockeeper.ui.analyse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.data.*
import androidx.compose.ui.text.style.TextAlign
import com.example.planpockeeper.ui.chart.PieChartBox
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.data.repository.BudgetCategoryRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

fun buildPlannedEntries(
    budgetCategories: List<BudgetCategory>,
    totalAmount: Double
): Pair<List<PieEntry>, List<Int>> {
    if (totalAmount <= 0.0) return Pair(emptyList(), emptyList())

    val entries = mutableListOf<PieEntry>()
    val colors = mutableListOf<Int>()

    budgetCategories.filter { it.plannedAmount > 0 }.forEach { cat ->
        val pct = ((cat.plannedAmount / totalAmount) * 100).toFloat()
        entries.add(PieEntry(pct, cat.categoryName))
        val c = runCatching {
            android.graphics.Color.parseColor(cat.color)
        }.getOrElse { android.graphics.Color.GRAY }
        colors.add(c)
    }

    val usedTotal = budgetCategories.sumOf { it.plannedAmount }
    val remaining = totalAmount - usedTotal
    if (remaining > 0.01) {
        entries.add(PieEntry(((remaining / totalAmount) * 100).toFloat(), "Autre"))
        colors.add(android.graphics.Color.LTGRAY)
    }

    return Pair(entries, colors)
}

fun buildRealEntries(
    budgetCategories: List<BudgetCategory>
): Pair<List<PieEntry>, List<Int>> {
    val totalSpent = budgetCategories.sumOf { it.spentAmount }
    if (totalSpent <= 0.0) return Pair(emptyList(), emptyList())

    val entries = mutableListOf<PieEntry>()
    val colors = mutableListOf<Int>()

    budgetCategories.filter { it.spentAmount > 0 }.forEach { cat ->
        val pct = ((cat.spentAmount / totalSpent) * 100).toFloat()
        entries.add(PieEntry(pct, cat.categoryName))
        val c = runCatching {
            android.graphics.Color.parseColor(cat.color)
        }.getOrElse { android.graphics.Color.GRAY }
        colors.add(c)
    }

    return Pair(entries, colors)
}

@Composable
fun AnalyseScreen() {

    val budgetRepository = remember { BudgetRepository() }
    val budgetCategoryRepository = remember { BudgetCategoryRepository() }

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var budgetCategories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }

    LaunchedEffect(Unit) {
        budgetRepository.getActiveBudgetFlow().collect { budget ->
            activeBudget = budget
        }
    }

    LaunchedEffect(activeBudget?.id) {
        val id = activeBudget?.id ?: return@LaunchedEffect
        budgetCategoryRepository.getActiveBudgetCategories(id).collect { cats ->
            budgetCategories = cats
        }
    }

    val totalAmount = activeBudget?.totalAmount ?: 0.0
    val (plannedEntries, plannedColors) = buildPlannedEntries(budgetCategories, totalAmount)
    val (realEntries, realColors) = buildRealEntries(budgetCategories)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Graphiques Camemberts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp, top = 16.dp)
        )

        if (realEntries.isNotEmpty()) {
            PieChartBox(
                title = "Réel",
                entries = realEntries,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                pieColors = realColors
            )
        } else {
            Text(
                text = "Aucune dépense enregistrée",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (plannedEntries.isNotEmpty()) {
            PieChartBox(
                title = "Prévu",
                entries = plannedEntries,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                pieColors = plannedColors
            )
        } else {
            Text(
                text = "Aucun budget actif",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tableau récapitulatif",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp)
        )

        val summaryData = budgetCategories.map { cat ->
            BudgetSummaryItem(
                name = cat.categoryName,
                planned = cat.plannedAmount,
                spent = cat.spentAmount
            )
        }

        BudgetSummaryTable(categories = summaryData)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bilan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp)
        )

        BilanCard(
            totalPlanned = totalAmount,
            totalSpent = budgetCategories.sumOf { it.spentAmount },
            categories = summaryData,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.Start)
        )


        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Analyse",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp)
        )

        AnalyseCard(
            categories = summaryData,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}
