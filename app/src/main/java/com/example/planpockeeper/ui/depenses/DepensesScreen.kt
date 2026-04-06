package com.example.planpockeeper.ui.depenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.model.Expense
import com.example.planpockeeper.data.repository.BudgetCategoryRepository
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.data.repository.ExpenseRepository
import com.example.planpockeeper.ui.theme.Cyan_Pastel_Dark
import com.example.planpockeeper.ui.theme.Vieux_Rose_Dark
import com.example.planpockeeper.utils.CurrencyFormatter
import com.example.planpockeeper.utils.PreferencesManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── Theme ────────────────────────────────────────────────────────────────

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
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// ─── Enums & helpers ─────────────────────────────────────────────────────

private enum class DepensesRoute { LIST, ADD, EDIT }

private enum class SortOrder(val label: String) {
    DATE_DESC("Date (récent)"),
    DATE_ASC("Date (ancien)"),
    AMOUNT_DESC("Montant (élevé)"),
    AMOUNT_ASC("Montant (faible)")
}

private enum class PeriodFilter(val label: String) {
    ALL("Toutes"),
    THIS_WEEK("Cette semaine"),
    THIS_MONTH("Ce mois"),
    CUSTOM("Personnalisé")
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

private fun sanitizeAmountInput(input: String): String {
    val normalized = input.replace(',', '.')
    return buildString {
        normalized.forEachIndexed { index, c ->
            if (c.isDigit()) append(c)
            else if (c == '.' && index != 0 && !contains('.')) append(c)
        }
    }
}

private fun parseAmount(input: String): Double? = sanitizeAmountInput(input).toDoubleOrNull()

// ─── Screen root ─────────────────────────────────────────────────────────

@Composable
fun DepensesScreen() {
    DepensesTheme {
        var route by remember { mutableStateOf(DepensesRoute.LIST) }
        var editingExpense by remember { mutableStateOf<Expense?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            when (route) {
                DepensesRoute.LIST -> DepensesHomeContent(
                    onAddExpense = { route = DepensesRoute.ADD },
                    onEditExpense = { expense ->
                        editingExpense = expense
                        route = DepensesRoute.EDIT
                    }
                )
                DepensesRoute.ADD -> AddDepenseScreen(
                    onBack = { route = DepensesRoute.LIST },
                    onSaved = { route = DepensesRoute.LIST }
                )
                DepensesRoute.EDIT -> editingExpense?.let { expense ->
                    AddDepenseScreen(
                        onBack = { route = DepensesRoute.LIST },
                        onSaved = { route = DepensesRoute.LIST },
                        existingExpense = expense
                    )
                }
            }
        }
    }
}

// ─── List screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepensesHomeContent(
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    val budgetRepository = remember { BudgetRepository() }
    val expenseRepository = remember { ExpenseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val currency by prefsManager.currency.collectAsState(initial = "EUR")

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var periodFilter by remember { mutableStateOf(PeriodFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var customStartMillis by remember { mutableStateOf<Long?>(null) }
    var customEndMillis by remember { mutableStateOf<Long?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCustomStartPicker by remember { mutableStateOf(false) }
    var showCustomEndPicker by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showNoBudgetDialog by remember { mutableStateOf(false) }

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

    val allCategories = expenses.map { it.categoryName }.distinct().sorted()

    val filteredExpenses = remember(
        expenses, sortOrder, periodFilter,
        selectedCategory, customStartMillis, customEndMillis
    ) {
        var list = expenses
        if (selectedCategory != null) {
            list = list.filter { it.categoryName == selectedCategory }
        }
        list = when (periodFilter) {
            PeriodFilter.ALL -> list
            PeriodFilter.THIS_WEEK -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                list.filter { it.date.toDate().time >= weekStart }
            }
            PeriodFilter.THIS_MONTH -> {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                list.filter { it.date.toDate().time >= monthStart }
            }
            PeriodFilter.CUSTOM -> {
                val start = customStartMillis
                val end = customEndMillis
                if (start != null) list = list.filter { it.date.toDate().time >= start }
                if (end != null) list = list.filter {
                    it.date.toDate().time <= end + 86_400_000
                }
                list
            }
        }
        when (sortOrder) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.date.toDate().time }
            SortOrder.DATE_ASC -> list.sortedBy { it.date.toDate().time }
            SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }

    val totalSpent = filteredExpenses.sumOf { it.amount }
    val activeFiltersCount = listOfNotNull(
        if (selectedCategory != null) 1 else null,
        if (periodFilter != PeriodFilter.ALL) 1 else null,
        if (sortOrder != SortOrder.DATE_DESC) 1 else null
    ).size

    // No budget dialog
    if (showNoBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showNoBudgetDialog = false },
            icon = {
                Icon(Icons.Outlined.Warning, contentDescription = null)
            },
            title = { Text("Aucun budget actif") },
            text = {
                Text("Tu dois d'abord créer un budget dans l'onglet Budget avant de pouvoir ajouter des dépenses.")
            },
            confirmButton = {
                Button(onClick = { showNoBudgetDialog = false }) { Text("Compris") }
            }
        )
    }

    // Delete confirmation dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Supprimer la dépense") },
            text = {
                Text(
                    "Supprimer \"${expense.categoryName}\" — " +
                            "${CurrencyFormatter.format(expense.amount, currency)} " +
                            "du ${formatDate(expense.date.toDate().time)} ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { expenseRepository.deleteExpense(expense) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Supprimer") }
            },
            dismissButton = {
                OutlinedButton(onClick = { expenseToDelete = null }) { Text("Annuler") }
            }
        )
    }

    // Custom date pickers
    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customStartMillis ?: System.currentTimeMillis()
    )
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = customEndMillis ?: System.currentTimeMillis()
    )

    if (showCustomStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showCustomStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customStartMillis = startPickerState.selectedDateMillis
                    showCustomStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomStartPicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = startPickerState) }
    }

    if (showCustomEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showCustomEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customEndMillis = endPickerState.selectedDateMillis
                    showCustomEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEndPicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = endPickerState) }
    }

    // Filter dialog
    if (showFilterDialog) {
        Dialog(onDismissRequest = { showFilterDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Filtres & tri",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Sort
                    Text(
                        "Trier par",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SortOrder.entries.forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sortOrder = order }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(order.label)
                            if (sortOrder == order) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Period
                    Text(
                        "Période",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PeriodFilter.entries.forEach { period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { periodFilter = period }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(period.label)
                            if (periodFilter == period) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (periodFilter == PeriodFilter.CUSTOM) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCustomStartPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (customStartMillis != null)
                                        formatDate(customStartMillis!!)
                                    else "Début"
                                )
                            }
                            OutlinedButton(
                                onClick = { showCustomEndPicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (customEndMillis != null)
                                        formatDate(customEndMillis!!)
                                    else "Fin"
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Category
                    Text(
                        "Catégorie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = null }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Toutes")
                        if (selectedCategory == null) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    allCategories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat)
                            if (selectedCategory == cat) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                sortOrder = SortOrder.DATE_DESC
                                periodFilter = PeriodFilter.ALL
                                selectedCategory = null
                                customStartMillis = null
                                customEndMillis = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Réinitialiser") }
                        Button(
                            onClick = { showFilterDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Appliquer") }
                    }
                }
            }
        }
    }

    // Main list
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dépenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total affiché : ${CurrencyFormatter.format(totalSpent, currency)}")
                    Text("Nombre : ${filteredExpenses.size}")
                    if (activeFiltersCount > 0) {
                        Text(
                            "$activeFiltersCount filtre(s) actif(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (activeBudget == null) showNoBudgetDialog = true
                        else onAddExpense()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter")
                }
                BadgedBox(
                    badge = {
                        if (activeFiltersCount > 0) {
                            Badge { Text(activeFiltersCount.toString()) }
                        }
                    }
                ) {
                    OutlinedButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filtres")
                    }
                }
            }
        }

        item {
            Text(
                "Historique des dépenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredExpenses.isEmpty()) {
            item {
                Text(
                    if (expenses.isEmpty()) "Aucune dépense pour le moment."
                    else "Aucune dépense ne correspond aux filtres.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.categoryName, fontWeight = FontWeight.SemiBold)
                            Text(
                                CurrencyFormatter.format(expense.amount, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                formatDate(expense.date.toDate().time),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (expense.description.isNotBlank()) {
                                Text(
                                    expense.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onEditExpense(expense) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Modifier",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { expenseToDelete = expense }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Supprimer",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ─── Add / Edit screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDepenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    existingExpense: Expense? = null
) {
    val budgetRepository = remember { BudgetRepository() }
    val budgetCategoryRepository = remember { BudgetCategoryRepository() }
    val expenseRepository = remember { ExpenseRepository() }
    val scope = rememberCoroutineScope()

    val isEditing = existingExpense != null

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var activeCategories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf(existingExpense?.categoryId ?: "") }
    var amountInput by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var descriptionInput by remember { mutableStateOf(existingExpense?.description ?: "") }
    var expenseDateMillis by remember {
        mutableStateOf(existingExpense?.date?.toDate()?.time ?: System.currentTimeMillis())
    }
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

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expenseDateMillis
    )

    if (showDatePicker) {
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
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Modifier la dépense" else "Ajouter une dépense")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeBudget == null) {
                Text("Aucun budget actif.")
                return@Column
            }

            Text("Catégorie")
            if (activeCategories.isEmpty()) {
                Text(
                    "Aucune catégorie active. Ajoute d'abord une catégorie dans l'onglet Budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = {
                    if (activeCategories.isNotEmpty()) categoryExpanded = !categoryExpanded
                }
            ) {
                OutlinedTextField(
                    value = activeCategories.firstOrNull { it.id == selectedCategoryId }
                        ?.categoryName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = activeCategories.isNotEmpty()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
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
                label = { Text("Description (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text("Date")
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(formatDate(expenseDateMillis))
            }

            Button(
                onClick = {
                    val amount = parseAmount(amountInput)
                    if (amount == null || amount <= 0.0) {
                        statusMessage = "Montant invalide."
                        return@Button
                    }
                    val selectedCategory = activeCategories.firstOrNull {
                        it.id == selectedCategoryId
                    }
                    if (selectedCategory == null) {
                        statusMessage = "Sélectionne une catégorie."
                        return@Button
                    }
                    isSaving = true
                    statusMessage = null
                    scope.launch {
                        val result = if (isEditing) {
                            expenseRepository.updateExpense(
                                existingExpense!!.copy(
                                    categoryId = selectedCategory.id,
                                    categoryName = selectedCategory.categoryName,
                                    amount = amount,
                                    description = descriptionInput.trim(),
                                    date = Timestamp(Date(expenseDateMillis))
                                )
                            )
                        } else {
                            expenseRepository.addExpense(
                                Expense(
                                    budgetId = activeBudget!!.id,
                                    categoryId = selectedCategory.id,
                                    categoryName = selectedCategory.categoryName,
                                    amount = amount,
                                    description = descriptionInput.trim(),
                                    date = Timestamp(Date(expenseDateMillis))
                                )
                            )
                        }
                        isSaving = false
                        if (result.isFailure) {
                            statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Erreur."
                            return@launch
                        }
                        onSaved()
                    }
                },
                enabled = !isSaving && activeCategories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Modifier la dépense" else "Enregistrer la dépense")
            }

            if (isSaving) CircularProgressIndicator()

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    statusMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}