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
): List<PieEntry> {
    if (totalAmount <= 0.0) return emptyList()

    val entries = budgetCategories
        .filter { it.plannedAmount > 0 }
        .map { cat ->
            val pct = ((cat.plannedAmount / totalAmount) * 100).toFloat()
            PieEntry(pct, cat.categoryName)
        }

    val usedTotal = budgetCategories.sumOf { it.plannedAmount }
    val remaining = totalAmount - usedTotal

    return if (remaining > 0.01) {
        entries + PieEntry(((remaining / totalAmount) * 100).toFloat(), "Autre")
    } else {
        entries
    }
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
    val plannedEntries = buildPlannedEntries(budgetCategories, totalAmount)

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

        PieChartBox(
            title = "Réel",
            entries = listOf(
                PieEntry(40f, "Catégorie 1"),
                PieEntry(30f, "Catégorie 2"),
                PieEntry(30f, "Catégorie 3")
            ),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (plannedEntries.isNotEmpty()) {
            PieChartBox(
                title = "Prévu",
                entries = plannedEntries,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
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

        val fakeData = listOf(
            BudgetSummaryItem("Catégorie 1", 500.0, 620.0),
            BudgetSummaryItem("Catégorie 2", 300.0, 210.0),
            BudgetSummaryItem("Catégorie 3", 400.0, 390.0),
            BudgetSummaryItem("Catégorie 4", 200.0, 80.0)
        )

        BudgetSummaryTable(categories = fakeData)

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
            totalPlanned = fakeData.sumOf { it.planned },
            totalSpent = fakeData.sumOf { it.spent },
            categories = fakeData,
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

        AnalyseCard(categories = fakeData,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.Start))

        Spacer(modifier = Modifier.height(16.dp))

    }
}
