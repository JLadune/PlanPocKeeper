package com.example.planpockeeper.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.model.Category
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.Firebase

// ─── Direct Firestore helpers (no repository layer) ──────────────────────

private fun userId() = Firebase.auth.currentUser?.uid
    ?: throw IllegalStateException("User not logged in")

private fun budgetCol() = Firebase.firestore
    .collection("users").document(userId())
    .collection("budget")

private fun categoryCol() = Firebase.firestore
    .collection("users").document(userId())
    .collection("categories")

private fun budgetCategoryCol(budgetId: String) = Firebase.firestore
    .collection("users").document(userId())
    .collection("budget").document(budgetId)
    .collection("budgetCategories")

// ─── Screen ───────────────────────────────────────────────────────────────

@Composable
fun BudgetScreen() {
    val scope = rememberCoroutineScope()

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var budgetCategories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }
    var availableCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf<BudgetCategory?>(null) }

    // Load active budget once
    LaunchedEffect(Unit) {
        try {
            val snapshot = budgetCol()
                .whereEqualTo("active", true)
                .get().await()
            val budget = snapshot.documents.firstOrNull()?.toObject(Budget::class.java)
            activeBudget = budget
        } catch (e: Exception) {
            // not logged in yet or no budget
        }
        isLoading = false
    }

    // Load categories for the budget whenever budget changes
    LaunchedEffect(activeBudget?.id) {
        val id = activeBudget?.id ?: return@LaunchedEffect
        try {
            val snapshot = budgetCategoryCol(id)
                .whereGreaterThan("plannedAmount", 0.0)
                .get().await()
            budgetCategories = snapshot.documents.mapNotNull {
                it.toObject(BudgetCategory::class.java)
            }
        } catch (e: Exception) { /* ignore */ }
    }

    // Load available categories
    LaunchedEffect(Unit) {
        try {
            val snapshot = categoryCol().get().await()
            availableCategories = snapshot.documents.mapNotNull {
                it.toObject(Category::class.java)
            }
        } catch (e: Exception) { /* ignore */ }
    }

    fun refreshBudget() {
        scope.launch {
            try {
                val snapshot = budgetCol()
                    .whereEqualTo("active", true)
                    .get().await()
                activeBudget = snapshot.documents.firstOrNull()
                    ?.toObject(Budget::class.java)
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun refreshCategories() {
        val id = activeBudget?.id ?: return
        scope.launch {
            try {
                val snapshot = budgetCategoryCol(id)
                    .whereGreaterThan("plannedAmount", 0.0)
                    .get().await()
                budgetCategories = snapshot.documents.mapNotNull {
                    it.toObject(BudgetCategory::class.java)
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun refreshAvailableCategories() {
        scope.launch {
            try {
                val snapshot = categoryCol().get().await()
                availableCategories = snapshot.documents.mapNotNull {
                    it.toObject(Category::class.java)
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalSpent = budgetCategories.sumOf { it.spentAmount }
    val remaining = (activeBudget?.totalAmount ?: 0.0) - totalSpent

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Budget total card ──
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Budget total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeBudget != null) {
                            Row {
                                IconButton(
                                    onClick = { showCreateDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Modifier",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                // delete subcollections then doc
                                                val cats = budgetCategoryCol(activeBudget!!.id).get().await()
                                                cats.documents.forEach { it.reference.delete().await() }
                                                budgetCol().document(activeBudget!!.id).delete().await()
                                                activeBudget = null
                                                budgetCategories = emptyList()
                                            } catch (e: Exception) { /* ignore */ }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Supprimer",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${activeBudget?.totalAmount?.toInt() ?: 0}€",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activeBudget != null) {
                        val budget = activeBudget!!
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val periodicityLabel = when {
                            !budget.periodical && budget.endDate != null ->
                                "Fin le ${dateFormat.format(budget.endDate.toDate())}"
                            budget.periodicity.startsWith("custom_") -> {
                                val days = budget.periodicity
                                    .removePrefix("custom_").removeSuffix("j")
                                "Tous les $days jours"
                            }
                            else -> "Périodicité : ${budget.periodicity.replaceFirstChar { it.uppercase() }}"
                        }
                        if (budget.description.isNotBlank()) {
                            Text(
                                budget.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            periodicityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Dépensé", style = MaterialTheme.typography.labelSmall)
                                Text("${totalSpent.toInt()}€", fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Restant", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${remaining.toInt()}€",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (remaining < 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        val progress = if (budget.totalAmount > 0)
                            (totalSpent / budget.totalAmount).toFloat().coerceIn(0f, 1f)
                        else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── Permanent action buttons ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (activeBudget == null) "Créer un budget" else "Modifier le budget")
                }
                Button(
                    onClick = {
                        refreshAvailableCategories()
                        showAddCategoryDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = activeBudget != null
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter catégorie")
                }
            }
        }

        // ── Categories header ──
        item {
            Text(
                "Catégories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (budgetCategories.isEmpty()) {
            item {
                Text(
                    if (activeBudget == null)
                        "Créez d'abord un budget"
                    else
                        "Aucune catégorie pour l'instant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Category rows ──
        items(budgetCategories, key = { it.id }) { cat ->
            val catRemaining = cat.plannedAmount - cat.spentAmount
            val catColor = runCatching {
                Color(android.graphics.Color.parseColor(cat.color))
            }.getOrElse { MaterialTheme.colorScheme.primary }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(12.dp).clip(CircleShape).background(catColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.categoryName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${cat.spentAmount.toInt()}€ / ${cat.plannedAmount.toInt()}€",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Restant : ${catRemaining.toInt()}€",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (catRemaining < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { editingBudgetCategory = cat },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Modifier",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    budgetCategoryCol(activeBudget!!.id)
                                        .document(cat.id)
                                        .update("plannedAmount", 0.0).await()
                                    refreshCategories()
                                } catch (e: Exception) { /* ignore */ }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Supprimer",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // ── Dialogs ──

    if (showCreateDialog) {
        CreateBudgetDialog(
            existing = activeBudget,
            onDismiss = { showCreateDialog = false },
            onConfirm = { amount, desc, start, periodical, period, customDays, end ->
                val resolvedPeriodicity = when {
                    !periodical -> "once"
                    period == "custom" -> "custom_${customDays}j"
                    else -> period
                }
                scope.launch {
                    try {
                        if (activeBudget == null) {
                            val ref = budgetCol().document()
                            val budget = Budget(
                                id = ref.id,
                                totalAmount = amount,
                                description = desc,
                                startDate = Timestamp(start),
                                periodical = periodical,
                                periodicity = resolvedPeriodicity,
                                endDate = if (end != null) Timestamp(end) else null,
                                active = true
                            )
                            ref.set(budget).await()
                        } else {
                            val updated = activeBudget!!.copy(
                                totalAmount = amount,
                                description = desc,
                                periodical = periodical,
                                periodicity = resolvedPeriodicity,
                                endDate = if (end != null) Timestamp(end) else null
                            )
                            budgetCol().document(updated.id).set(updated).await()
                        }
                        refreshBudget()
                    } catch (e: Exception) { /* ignore */ }
                }
                showCreateDialog = false
            }
        )
    }

    if (showAddCategoryDialog) {
        AddBudgetCategoryDialog(
            availableCategories = availableCategories,
            alreadyUsedCategoryIds = budgetCategories.map { it.categoryId }.toSet(),
            budgetTotal = activeBudget?.totalAmount ?: 0.0,
            currentCategoriesTotal = budgetCategories.sumOf { it.plannedAmount },
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { category, amount ->
                scope.launch {
                    try {
                        val ref = budgetCategoryCol(activeBudget!!.id).document()
                        val budgetCategory = BudgetCategory(
                            id = ref.id,
                            categoryId = category.id,
                            categoryName = category.name,
                            plannedAmount = amount,
                            color = category.color
                        )
                        ref.set(budgetCategory).await()
                        refreshCategories()
                    } catch (e: Exception) { /* ignore */ }
                }
                showAddCategoryDialog = false
            },
            onCreateNew = {
                showAddCategoryDialog = false
                showAddCustomCategoryDialog = true
            }
        )
    }

    if (showAddCustomCategoryDialog) {
        CreateCategoryDialog(
            existingCategories = availableCategories,
            onDismiss = { showAddCustomCategoryDialog = false },
            onConfirm = { name, color ->
                scope.launch {
                    try {
                        val ref = categoryCol().document()
                        val category = Category(id = ref.id, name = name, color = color)
                        ref.set(category).await()
                        refreshAvailableCategories()
                    } catch (e: Exception) { /* ignore */ }
                }
                showAddCustomCategoryDialog = false
                showAddCategoryDialog = true
            }
        )
    }

    editingBudgetCategory?.let { cat ->
        EditBudgetCategoryDialog(
            budgetCategory = cat,
            onDismiss = { editingBudgetCategory = null },
            onConfirm = { newAmount ->
                scope.launch {
                    try {
                        budgetCategoryCol(activeBudget!!.id)
                            .document(cat.id)
                            .update("plannedAmount", newAmount).await()
                        refreshCategories()
                    } catch (e: Exception) { /* ignore */ }
                }
                editingBudgetCategory = null
            }
        )
    }
}

// ─── Create / Edit Budget Dialog ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetDialog(
    existing: Budget?,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Date, Boolean, String, Int, Date?) -> Unit
) {
    var amount by remember { mutableStateOf(existing?.totalAmount?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var periodical by remember { mutableStateOf(existing?.periodical ?: true) }
    val existingPeriod = existing?.periodicity ?: "mensuel"
    val isExistingCustom = existingPeriod.startsWith("custom_")
    var periodicity by remember {
        mutableStateOf(if (isExistingCustom) "custom" else existingPeriod)
    }
    var customDays by remember {
        mutableStateOf(
            if (isExistingCustom) existingPeriod.removePrefix("custom_").removeSuffix("j") else ""
        )
    }
    var showPeriodicityMenu by remember { mutableStateOf(false) }
    val periodicities = listOf("hebdomadaire", "mensuel", "annuel", "custom")
    var startDate by remember { mutableStateOf(existing?.startDate?.toDate() ?: Date()) }
    var endDate by remember { mutableStateOf(existing?.endDate?.toDate()) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.time)
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time ?: System.currentTimeMillis()
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        if (existing == null) "Créer un budget" else "Modifier le budget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Montant total (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Note (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = dateFormat.format(startDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date de début") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartPicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Budget périodique", modifier = Modifier.weight(1f))
                        Switch(checked = periodical, onCheckedChange = { periodical = it })
                    }
                }
                if (periodical) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = showPeriodicityMenu,
                            onExpandedChange = { showPeriodicityMenu = it }
                        ) {
                            OutlinedTextField(
                                value = when (periodicity) {
                                    "custom" -> "Personnalisé"
                                    else -> periodicity.replaceFirstChar { it.uppercase() }
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Périodicité") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPeriodicityMenu)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showPeriodicityMenu,
                                onDismissRequest = { showPeriodicityMenu = false }
                            ) {
                                periodicities.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (p) {
                                                    "custom" -> "Personnalisé (X jours)"
                                                    else -> p.replaceFirstChar { it.uppercase() }
                                                }
                                            )
                                        },
                                        onClick = { periodicity = p; showPeriodicityMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    if (periodicity == "custom") {
                        item {
                            OutlinedTextField(
                                value = customDays,
                                onValueChange = { customDays = it },
                                label = { Text("Nombre de jours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = if (endDate != null) dateFormat.format(endDate!!) else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date de fin") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEndPicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val amt = amount.toDoubleOrNull() ?: return@Button
                                val days = if (periodicity == "custom")
                                    customDays.toIntOrNull() ?: 0 else 0
                                onConfirm(amt, description, startDate, periodical, periodicity, days, endDate)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Sauvegarder") }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { startDate = Date(it) }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = startPickerState) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { endDate = Date(it) }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = endPickerState) }
    }
}

// ─── Add Budget Category Dialog ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetCategoryDialog(
    availableCategories: List<Category>,
    alreadyUsedCategoryIds: Set<String>,
    budgetTotal: Double,
    currentCategoriesTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Category, Double) -> Unit,
    onCreateNew: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Catégories disponibles = pas encore utilisées dans le budget
    val selectableCategories = availableCategories.filter { it.id !in alreadyUsedCategoryIds }

    val remaining = budgetTotal - currentCategoriesTotal
    val parsedAmount = amount.toDoubleOrNull()
    val amountExceedsRemaining = parsedAmount != null && parsedAmount > remaining
    val maxAvailable = remaining

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Ajouter une catégorie",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                //Dropdown catégories filtrées
                ExposedDropdownMenuBox(
                    expanded = showMenu,
                    onExpandedChange = { showMenu = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (selectableCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Aucune catégorie disponible") },
                                onClick = {}
                            )
                        }
                        selectableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val c = runCatching {
                                            Color(android.graphics.Color.parseColor(cat.color))
                                        }.getOrElse { Color.Gray }
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = { selectedCategory = cat; showMenu = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Créer une nouvelle catégorie") },
                            onClick = { showMenu = false; onCreateNew() }
                        )
                    }
                }

                //Montant
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        // chiffres et un seul point uniquement
                        val filtered = input.replace(',', '.')
                        val result = buildString {
                            filtered.forEachIndexed { i, c ->
                                if (c.isDigit()) append(c)
                                else if (c == '.' && i != 0 && !contains('.')) append(c)
                            }
                        }
                        amount = result
                    },
                    label = { Text("Montant prévu (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountExceedsRemaining,
                    supportingText = if (amountExceedsRemaining) {
                        {
                            Text(
                                "Dépasse le budget restant. Max disponible : ${maxAvailable.toInt()}€",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    } else null
                )

                //Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            val cat = selectedCategory ?: return@Button
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            if (amt <= 0 || amountExceedsRemaining) return@Button
                            onConfirm(cat, amt)
                        },
                        enabled = selectedCategory != null
                                && amount.toDoubleOrNull() != null
                                && !amountExceedsRemaining,
                        modifier = Modifier.weight(1f)
                    ) { Text("Ajouter") }
                }
            }
        }
    }
}

// ─── Edit Budget Category Dialog ─────────────────────────────────────────

@Composable
fun EditBudgetCategoryDialog(
    budgetCategory: BudgetCategory,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf(budgetCategory.plannedAmount.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Modifier ${budgetCategory.categoryName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Nouveau montant (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            onConfirm(amt)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Sauvegarder") }
                }
            }
        }
    }
}

// ─── Create Custom Category Dialog ───────────────────────────────────────

@Composable
fun CreateCategoryDialog(
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit

) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF5733") }
    val presetColors = listOf(
        "#FF5733", "#FF8C00", "#FFD700", "#4CAF50",
        "#2196F3", "#9C27B0", "#E91E63", "#00BCD4",
        "#795548", "#607D8B"
    )

    // ── Validations ──────────────────────────────────────────────────────
    val nameTaken = name.isNotBlank() &&
            existingCategories.any { it.name.trim().equals(name.trim(), ignoreCase = true) }

    val colorTaken = existingCategories.any {
        it.color.trim().equals(selectedColor.trim(), ignoreCase = true)
    }

    val canConfirm = name.isNotBlank() && !nameTaken && !colorTaken

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nouvelle catégorie",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // ── Nom ──────────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la catégorie") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (nameTaken) {
                        { Text("Ce nom est déjà utilisé.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null
                )

                // ── Couleur ───────────────────────────────────────────────
                Text("Couleur", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val c = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrElse { Color.Gray }
                        val isUsed = existingCategories.any {
                            it.color.trim().equals(hex.trim(), ignoreCase = true)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c.copy(alpha = if (isUsed) 0.3f else 1f))  // grisé si déjà pris
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
                if (colorTaken) {
                    Text(
                        "Cette couleur est déjà utilisée par une autre catégorie.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                // ── Boutons ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { if (canConfirm) onConfirm(name, selectedColor) },
                        enabled = canConfirm,
                        modifier = Modifier.weight(1f)
                    ) { Text("Créer") }
                }
            }
        }
    }
}