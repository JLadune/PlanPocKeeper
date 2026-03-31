package com.example.planpockeeper.ui.depenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.model.Expense
import com.example.planpockeeper.data.repository.BudgetCategoryRepository
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.data.repository.ExpenseRepository
import com.example.planpockeeper.ui.theme.Cyan_Pastel_Dark
import com.example.planpockeeper.ui.theme.Vieux_Rose_Dark
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun DepensesTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Cyan_Pastel_Dark,
        secondary = Cyan_Pastel_Dark,
        background = Cyan_Pastel_Dark,
        surface = Cyan_Pastel_Dark,
        onPrimary = Color.White,
        onSurface = Vieux_Rose_Dark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private enum class DepensesRoute {
    LIST,
    ADD
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}

private fun sanitizeAmountInput(input: String): String {
    val normalized = input.replace(',', '.')
    val filtered = buildString {
        normalized.forEachIndexed { index, c ->
            if (c.isDigit()) {
                append(c)
            } else if (c == '.' && index != 0 && !contains('.')) {
                append(c)
            }
        }
    }
    return filtered
}

private fun parseAmount(input: String): Double? {
    return sanitizeAmountInput(input).toDoubleOrNull()
}

@Composable
fun DepensesScreen() {
    DepensesTheme {
        var route by remember { mutableStateOf(DepensesRoute.LIST) }

        Box(modifier = Modifier.fillMaxSize()) {
            when (route) {
                DepensesRoute.LIST -> DepensesHomeContent(
                    onAddExpense = { route = DepensesRoute.ADD }
                )
                DepensesRoute.ADD -> AddDepenseScreen(
                    onBack = { route = DepensesRoute.LIST },
                    onSaved = { route = DepensesRoute.LIST }
                )
            }
        }
    }
}

@Composable
private fun DepensesHomeContent(onAddExpense: () -> Unit) {
    val budgetRepository = remember { BudgetRepository() }
    val expenseRepository = remember { ExpenseRepository() }
    val scope = rememberCoroutineScope()

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        budgetRepository.getActiveBudgetFlow().collectLatest { budget ->
            activeBudget = budget

            val budgetId = budget?.id
            if (budgetId.isNullOrBlank()) {
                expenses = emptyList()
                isLoading = false
                return@collectLatest
            }

            expenseRepository.getExpenses(budgetId).collectLatest { items ->
                expenses = items
                isLoading = false
            }
        }
    }

    val totalSpent = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Depenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total depense: ${formatAmount(totalSpent)} €")
                    Text("Nombre de depenses: ${expenses.size}")
                }
            }
        }

        item {
            Button(
                onClick = onAddExpense,
                enabled = activeBudget != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajouter une depense")
            }
        }

        item {
            Text(
                "Historique des dépenses",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (activeBudget == null) {
            item {
                Text("Aucun budget actif. Cree d'abord un budget.")
            }
        } else if (expenses.isEmpty()) {
            item {
                Text("Aucune depense pour le moment.")
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.categoryName, fontWeight = FontWeight.SemiBold)
                            Text("${formatAmount(expense.amount)} €")
                            Text(formatDate(expense.date.toDate().time), style = MaterialTheme.typography.bodySmall)
                            if (expense.description.isNotBlank()) {
                                Text(expense.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val deleteResult = expenseRepository.deleteExpense(expense)
                                    if (deleteResult.isFailure) {
                                        statusMessage = deleteResult.exceptionOrNull()?.localizedMessage
                                            ?: "Impossible de supprimer la depense."
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            item {
                Text(statusMessage ?: "")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDepenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val budgetRepository = remember { BudgetRepository() }
    val budgetCategoryRepository = remember { BudgetCategoryRepository() }
    val expenseRepository = remember { ExpenseRepository() }
    val scope = rememberCoroutineScope()

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var activeCategories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var expenseDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        budgetRepository.getActiveBudgetFlow().collectLatest { budget ->
            activeBudget = budget
            val budgetId = budget?.id
            if (budgetId.isNullOrBlank()) {
                activeCategories = emptyList()
                selectedCategoryId = ""
                return@collectLatest
            }
            budgetCategoryRepository.getActiveBudgetCategories(budgetId).collectLatest { categories ->
                activeCategories = categories
                if (categories.none { it.id == selectedCategoryId }) {
                    selectedCategoryId = categories.firstOrNull()?.id.orEmpty()
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = expenseDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expenseDateMillis = datePickerState.selectedDateMillis ?: expenseDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row {
            Button(onClick = onBack) {
                Text("Retour")
            }
        }

        Text("Ajouter une depense", style = MaterialTheme.typography.headlineSmall)

        if (activeBudget == null) {
            Text("Aucun budget actif. Retourne a l'ecran depenses.")
            return@Column
        }


        Text("Categorie")
        if (activeCategories.isEmpty()) {
            Text(
                "Aucune categorie active pour ce budget. Ajoute d'abord une categorie dans l'onglet Budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = {
                if (activeCategories.isNotEmpty()) {
                    categoryExpanded = !categoryExpanded
                }
            }
        ) {
            OutlinedTextField(
                value = activeCategories.firstOrNull { it.id == selectedCategoryId }?.categoryName ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = activeCategories.isNotEmpty()
            )

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                if (activeCategories.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Aucune categorie active") },
                        onClick = { categoryExpanded = false }
                    )
                }
                activeCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.categoryName) },
                        onClick = {
                            selectedCategoryId = cat.id
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = sanitizeAmountInput(it) },
            label = { Text("Montant") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        OutlinedTextField(
            value = descriptionInput,
            onValueChange = { descriptionInput = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Text("Date")
        Button(onClick = { showDatePicker = true }) {
            Text(formatDate(expenseDateMillis))
        }

        Button(
            onClick = {
                val amount = parseAmount(amountInput)
                if (amount == null || amount <= 0.0) {
                    statusMessage = "Montant invalide."
                    return@Button
                }

                val selectedCategory = activeCategories.firstOrNull { it.id == selectedCategoryId }
                if (selectedCategory == null) {
                    statusMessage = "Selectionne une categorie."
                    return@Button
                }

                isSaving = true
                statusMessage = null

                scope.launch {
                    val addResult = expenseRepository.addExpense(
                        Expense(
                            budgetId = activeBudget!!.id,
                            categoryId = selectedCategory.id,
                            categoryName = selectedCategory.categoryName,
                            amount = amount,
                            description = descriptionInput.trim(),
                            date = Timestamp(Date(expenseDateMillis))
                        )
                    )

                    isSaving = false

                    if (addResult.isFailure) {
                        statusMessage = addResult.exceptionOrNull()?.localizedMessage
                            ?: "Erreur lors de l'ajout de la depense."
                        return@launch
                    }

                    onSaved()
                }
            },
            enabled = !isSaving && activeCategories.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer la depense")
        }

        if (isSaving) {
            CircularProgressIndicator()
        }

        if (!statusMessage.isNullOrBlank()) {
            Text(statusMessage ?: "")
        }
    }
}