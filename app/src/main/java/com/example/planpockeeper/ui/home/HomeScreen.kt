package com.example.planpockeeper.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.repository.BudgetCategoryRepository
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.ui.analyse.buildRealEntries
import com.example.planpockeeper.ui.chart.PieChartBox

@Composable
fun HomeScreen() {
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

    val totalPlanned = activeBudget?.totalAmount ?: 0.0
    val totalSpent = budgetCategories.sumOf { it.spentAmount }
    val isBudgetOver = totalSpent > totalPlanned
    val totalDiff = totalSpent - totalPlanned
    val (realEntries, realColors) = buildRealEntries(budgetCategories)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isBudgetOver) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isBudgetOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Budget dépassé ? ", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(
                if (isBudgetOver) "OUI" else "NON",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isBudgetOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = "Graphique Réel",
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

        Text(
            text = "Historique des dépenses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp, top = 16.dp)
        )


        //à ajouter

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Résumé du budget",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 12.dp, top = 16.dp)
        )

        BudgetSummaryHome(
            activeBudget = activeBudget,
            budgetCategories = budgetCategories
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}