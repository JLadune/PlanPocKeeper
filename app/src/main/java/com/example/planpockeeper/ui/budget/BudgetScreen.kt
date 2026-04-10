package com.example.planpockeeper.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetCategory
import com.example.planpockeeper.data.model.Category
import com.example.planpockeeper.utils.CurrencyFormatter
import com.example.planpockeeper.utils.PreferencesManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val QUICK_CATEGORY_COLORS = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2", "#5C6BC0", "#42A5F5",
    "#26C6DA", "#26A69A", "#66BB6A", "#9CCC65", "#D4E157", "#FFCA28",
    "#FFA726", "#FF7043", "#8D6E63", "#78909C"
)

private val CATEGORY_PALETTES = listOf(
    "Pastel" to listOf("#F8BBD0", "#E1BEE7", "#C5CAE9", "#B3E5FC", "#C8E6C9", "#FFF9C4"),
    "Nature" to listOf("#2E7D32", "#558B2F", "#8BC34A", "#A1887F", "#6D4C41", "#4E342E"),
    "Océan" to listOf("#01579B", "#0277BD", "#0288D1", "#039BE5", "#00ACC1", "#00838F"),
    "Énergie" to listOf("#B71C1C", "#E53935", "#FB8C00", "#FDD835", "#7CB342", "#43A047")
)

// ─── Firestore helpers ────────────────────────────────────────────────────

private fun userId() = Firebase.auth.currentUser?.uid
    ?: throw IllegalStateException("User not logged in")

private fun budgetCol() = Firebase.firestore
    .collection("users").document(userId()).collection("budget")

private fun categoryCol() = Firebase.firestore
    .collection("users").document(userId()).collection("categories")

private fun budgetCategoryCol(budgetId: String) = Firebase.firestore
    .collection("users").document(userId())
    .collection("budget").document(budgetId)
    .collection("budgetCategories")

// ─── Color helpers ────────────────────────────────────────────────────────

private fun randomHexColor(excludedHexes: List<String>): String {
    val rng = Random()
    var hex: String
    do {
        val color = Color(
            red = rng.nextFloat(),
            green = rng.nextFloat(),
            blue = rng.nextFloat()
        )
        hex = "#%06X".format(color.toArgb() and 0xFFFFFF)
    } while (excludedHexes.any { it.trim().equals(hex.trim(), ignoreCase = true) })
    return hex
}

private fun hexToComposeColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }

// ─── Screen ───────────────────────────────────────────────────────────────

@Composable
fun BudgetScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val currency by prefsManager.currency.collectAsState(initial = "EUR")

    var activeBudget by remember { mutableStateOf<Budget?>(null) }
    var budgetCategories by remember { mutableStateOf<List<BudgetCategory>>(emptyList()) }
    var availableCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf<BudgetCategory?>(null) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = budgetCol().whereEqualTo("active", true).get().await()
            activeBudget = snapshot.documents.firstOrNull()?.toObject(Budget::class.java)
        } catch (e: Exception) { /* ignore */ }
        isLoading = false
    }

    LaunchedEffect(activeBudget?.id) {
        val id = activeBudget?.id ?: return@LaunchedEffect
        try {
            val snapshot = budgetCategoryCol(id).whereGreaterThan("plannedAmount", 0.0).get().await()
            budgetCategories = snapshot.documents.mapNotNull { it.toObject(BudgetCategory::class.java) }
        } catch (e: Exception) { /* ignore */ }
    }

    LaunchedEffect(Unit) {
        try {
            val snapshot = categoryCol().get().await()
            availableCategories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
        } catch (e: Exception) { /* ignore */ }
    }

    fun refreshBudget() {
        scope.launch {
            try {
                val snapshot = budgetCol().whereEqualTo("active", true).get().await()
                activeBudget = snapshot.documents.firstOrNull()?.toObject(Budget::class.java)
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun refreshCategories() {
        val id = activeBudget?.id ?: return
        scope.launch {
            try {
                val snapshot = budgetCategoryCol(id).whereGreaterThan("plannedAmount", 0.0).get().await()
                budgetCategories = snapshot.documents.mapNotNull { it.toObject(BudgetCategory::class.java) }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun refreshAvailableCategories() {
        scope.launch {
            try {
                val snapshot = categoryCol().get().await()
                availableCategories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
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
        // ── Budget card ──
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
                        Text("Budget total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (activeBudget != null) {
                            Row {
                                IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
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
                                    Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Text(
                        text = CurrencyFormatter.format(activeBudget?.totalAmount ?: 0.0, currency),
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
                                val days = budget.periodicity.removePrefix("custom_").removeSuffix("j")
                                "Tous les $days jours"
                            }
                            else -> "Périodicité : ${budget.periodicity.replaceFirstChar { it.uppercase() }}"
                        }
                        if (budget.description.isNotBlank()) {
                            Text(budget.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(periodicityLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Dépensé", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(totalSpent, currency), fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Restant", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyFormatter.format(remaining, currency),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        val progress = if (budget.totalAmount > 0)
                            (totalSpent / budget.totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── Action buttons ──
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCreateDialog = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (activeBudget == null) "Créer un budget" else "Modifier le budget")
                }
                Button(
                    onClick = { refreshAvailableCategories(); showAddCategoryDialog = true },
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
            Text("Catégories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (budgetCategories.isEmpty()) {
            item {
                Text(
                    if (activeBudget == null) "Créez d'abord un budget" else "Aucune catégorie pour l'instant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Category rows ──
        items(budgetCategories, key = { it.id }) { cat ->
            val catRemaining = cat.plannedAmount - cat.spentAmount
            val catColor = hexToComposeColor(cat.color)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(catColor))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.categoryName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${CurrencyFormatter.format(cat.spentAmount, currency)} / ${CurrencyFormatter.format(cat.plannedAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Restant : ${CurrencyFormatter.format(catRemaining, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (catRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { editingBudgetCategory = cat }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    budgetCategoryCol(activeBudget!!.id).document(cat.id).update("plannedAmount", 0.0).await()
                                    refreshCategories()
                                } catch (e: Exception) { /* ignore */ }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", modifier = Modifier.size(18.dp))
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
            currency = currency,
            currentCategoriesTotal = budgetCategories.sumOf { it.plannedAmount },
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
                            ref.set(Budget(
                                id = ref.id,
                                totalAmount = amount,
                                description = desc,
                                startDate = Timestamp(start),
                                periodical = periodical,
                                periodicity = resolvedPeriodicity,
                                endDate = if (end != null) Timestamp(end) else null,
                                active = true
                            )).await()
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
            currency = currency,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { category, amount ->
                scope.launch {
                    try {
                        val ref = budgetCategoryCol(activeBudget!!.id).document()
                        ref.set(BudgetCategory(
                            id = ref.id,
                            categoryId = category.id,
                            categoryName = category.name,
                            plannedAmount = amount,
                            color = category.color
                        )).await()
                        refreshCategories()
                    } catch (e: Exception) { /* ignore */ }
                }
                showAddCategoryDialog = false
            },
            onCreateNew = {
                showAddCategoryDialog = false
                showAddCustomCategoryDialog = true
            },
            onDeleteCategory = { category ->
                scope.launch {
                    try {
                        categoryCol().document(category.id).delete().await()
                        refreshAvailableCategories()
                    } catch (e: Exception) { /* ignore */ }
                }
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
                        ref.set(Category(id = ref.id, name = name, color = color)).await()
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
            budgetTotal = activeBudget?.totalAmount ?: 0.0,
            currentCategoriesTotal = budgetCategories.filter { it.id != cat.id }.sumOf { it.plannedAmount },
            usedColors = budgetCategories
                .filter { it.id != cat.id }
                .map { it.color.trim().lowercase() }
                .toSet(),
            currency = currency,
            onDismiss = { editingBudgetCategory = null },
            onConfirm = { newAmount, newColor ->
                scope.launch {
                    try {
                        budgetCategoryCol(activeBudget!!.id).document(cat.id)
                            .update(
                                mapOf(
                                    "plannedAmount" to newAmount,
                                    "color" to newColor
                                )
                            )
                            .await()
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
    currency: String,
    currentCategoriesTotal: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Date, Boolean, String, Int, Date?) -> Unit
) {
    var amount by remember { mutableStateOf(existing?.totalAmount?.toString() ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var periodical by remember { mutableStateOf(existing?.periodical ?: true) }
    val existingPeriod = existing?.periodicity ?: "mensuel"
    val isExistingCustom = existingPeriod.startsWith("custom_")
    var periodicity by remember { mutableStateOf(if (isExistingCustom) "custom" else existingPeriod) }
    var customDays by remember {
        mutableStateOf(if (isExistingCustom) existingPeriod.removePrefix("custom_").removeSuffix("j") else "")
    }
    var showPeriodicityMenu by remember { mutableStateOf(false) }
    val periodicities = listOf("hebdomadaire", "mensuel", "annuel", "custom")
    var startDate by remember { mutableStateOf(existing?.startDate?.toDate() ?: Date()) }
    var endDate by remember { mutableStateOf(existing?.endDate?.toDate()) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val startPickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.time)
    val endPickerState = rememberDatePickerState(initialSelectedDateMillis = endDate?.time ?: System.currentTimeMillis())

    // ── Validation ──
    var submitAttempted by remember { mutableStateOf(false) }
    val amountError = if (submitAttempted && amount.toDoubleOrNull() == null) "Montant invalide" else null
    val amountZeroError = if (submitAttempted && (amount.toDoubleOrNull() ?: 0.0) <= 0.0) "Le montant doit être supérieur à 0" else null
    val customDaysError = if (submitAttempted && periodical && periodicity == "custom" && customDays.toIntOrNull() == null) "Nombre de jours invalide" else null
    val endDateError = if (submitAttempted && !periodical && endDate == null) "Choisissez une date de fin" else null
    val amountBelowCategories = if (existing != null && currentCategoriesTotal > 0)
        (amount.toDoubleOrNull() ?: 0.0) < currentCategoriesTotal
    else false
    val displayedAmountError = amountError ?: amountZeroError
    ?: if (amountBelowCategories) "Montant minimum : ${CurrencyFormatter.format(currentCategoriesTotal, currency)} (selon vos catégories)" else null

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
                        label = { Text("Montant total (${CurrencyFormatter.getSymbol(currency)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = displayedAmountError != null,
                        supportingText = if (displayedAmountError != null) {
                            { Text(displayedAmountError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null
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
                        modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Budget périodique", modifier = Modifier.weight(1f))
                        Switch(checked = periodical, onCheckedChange = { periodical = it })
                    }
                }
                if (periodical) {
                    item {
                        ExposedDropdownMenuBox(expanded = showPeriodicityMenu, onExpandedChange = { showPeriodicityMenu = it }) {
                            OutlinedTextField(
                                value = when (periodicity) { "custom" -> "Personnalisé"; else -> periodicity.replaceFirstChar { it.uppercase() } },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Périodicité") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPeriodicityMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = showPeriodicityMenu, onDismissRequest = { showPeriodicityMenu = false }) {
                                periodicities.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(when (p) { "custom" -> "Personnalisé (X jours)"; else -> p.replaceFirstChar { it.uppercase() } }) },
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
                                modifier = Modifier.fillMaxWidth(),
                                isError = customDaysError != null,
                                supportingText = if (customDaysError != null) {
                                    { Text(customDaysError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                                } else null
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
                            modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                            enabled = false,
                            isError = endDateError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = if (endDateError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (endDateError != null) {
                            Text(endDateError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Annuler") }
                        Button(
                            onClick = {
                                submitAttempted = true
                                val amt = amount.toDoubleOrNull() ?: return@Button
                                if (amt <= 0.0) return@Button
                                if (amountBelowCategories) return@Button
                                if (periodical && periodicity == "custom" && customDays.toIntOrNull() == null) return@Button
                                if (!periodical && endDate == null) return@Button
                                val days = if (periodicity == "custom") customDays.toIntOrNull() ?: 0 else 0
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
                TextButton(onClick = { startPickerState.selectedDateMillis?.let { startDate = Date(it) }; showStartPicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = startPickerState) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = { endPickerState.selectedDateMillis?.let { endDate = Date(it) }; showEndPicker = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Annuler") } }
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
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Category, Double) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amount by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    val selectableCategories = availableCategories.filter { it.id !in alreadyUsedCategoryIds }
    val remaining = budgetTotal - currentCategoriesTotal
    val parsedAmount = amount.toDoubleOrNull()
    val amountExceedsRemaining = parsedAmount != null && parsedAmount > remaining

    // Delete confirmation
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Supprimer \"${cat.name}\" ?") },
            text = { Text("Cette catégorie sera supprimée définitivement.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteCategory(cat); if (selectedCategory?.id == cat.id) selectedCategory = null; categoryToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = { OutlinedButton(onClick = { categoryToDelete = null }) { Text("Annuler") } }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ajouter une catégorie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { showMenu = it }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (selectableCategories.isEmpty()) {
                            DropdownMenuItem(text = { Text("Aucune catégorie disponible") }, onClick = {})
                        }
                        selectableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                                .background(hexToComposeColor(cat.color))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { showMenu = false; categoryToDelete = cat },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Supprimer",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
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

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        val filtered = input.replace(',', '.')
                        amount = buildString {
                            filtered.forEachIndexed { i, c ->
                                if (c.isDigit()) append(c)
                                else if (c == '.' && i != 0 && !contains('.')) append(c)
                            }
                        }
                    },
                    label = { Text("Montant prévu (${CurrencyFormatter.getSymbol(currency)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountExceedsRemaining,
                    supportingText = if (amountExceedsRemaining) {
                        { Text("Dépasse le budget restant. Max : ${CurrencyFormatter.format(remaining, currency)}", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Annuler") }
                    Button(
                        onClick = {
                            val cat = selectedCategory ?: return@Button
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            if (amt <= 0 || amountExceedsRemaining) return@Button
                            onConfirm(cat, amt)
                        },
                        enabled = selectedCategory != null && parsedAmount != null && !amountExceedsRemaining,
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
    budgetTotal: Double,
    currentCategoriesTotal: Double,
    usedColors: Set<String>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf(budgetCategory.plannedAmount.toString()) }
    val remaining = budgetTotal - currentCategoriesTotal
    val parsedAmount = amount.toDoubleOrNull()
    val amountExceedsRemaining = parsedAmount != null && parsedAmount > remaining

    val initialHsv = remember(budgetCategory.color) {
        FloatArray(3).also { hsv ->
            runCatching {
                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(budgetCategory.color), hsv)
            }.onFailure {
                hsv[0] = 0f
                hsv[1] = 0.8f
                hsv[2] = 0.9f
            }
        }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }

    val pickedColor = Color.hsv(hue, saturation, brightness)
    val pickedHex = "#%06X".format(pickedColor.toArgb() and 0xFFFFFF)
    val colorTaken = usedColors.contains(pickedHex.trim().lowercase())

    fun applyHexSelection(hex: String) {
        runCatching {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Modifier ${budgetCategory.categoryName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        val filtered = input.replace(',', '.')
                        amount = buildString {
                            filtered.forEachIndexed { i, c ->
                                if (c.isDigit()) append(c)
                                else if (c == '.' && i != 0 && !contains('.')) append(c)
                            }
                        }
                    },
                    label = { Text("Nouveau montant (${CurrencyFormatter.getSymbol(currency)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountExceedsRemaining,
                    supportingText = if (amountExceedsRemaining) {
                        { Text("Dépasse le budget disponible. Max : ${CurrencyFormatter.format(remaining, currency)}", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null
                )

                Text("Couleur", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(pickedColor)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        pickedHex,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (colorTaken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (colorTaken) {
                    Text(
                        "Cette couleur est déjà utilisée dans ce budget.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                ColorSwatchRows(
                    colors = QUICK_CATEGORY_COLORS,
                    selectedHex = pickedHex,
                    takenColors = usedColors,
                    onSelect = ::applyHexSelection
                )

                CATEGORY_PALETTES.forEach { (_, paletteColors) ->
                    ColorSwatchRows(
                        colors = paletteColors,
                        selectedHex = pickedHex,
                        takenColors = usedColors,
                        onSelect = ::applyHexSelection,
                        swatchesPerRow = 6
                    )
                }

                Text("Teinte", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onColorChange = { sat, bri ->
                        saturation = sat
                        brightness = bri
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Annuler") }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: return@Button
                            if (amountExceedsRemaining || colorTaken) return@Button
                            onConfirm(amt, pickedHex)
                        },
                        enabled = parsedAmount != null && parsedAmount > 0 && !amountExceedsRemaining && !colorTaken,
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
    val existingColors = existingCategories.map { it.color.trim().lowercase() }.toSet()

    var name by remember { mutableStateOf("") }

    // Visual picker state (HSV)
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0.8f) }
    var brightness by remember { mutableStateOf(0.9f) }

    val pickedColor = Color.hsv(hue, saturation, brightness)
    val pickedHex = "#%06X".format(pickedColor.toArgb() and 0xFFFFFF)

    fun applyHexSelection(hex: String) {
        runCatching {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        }
    }

    val nameTaken = name.isNotBlank() &&
            existingCategories.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
    val colorTaken = existingColors.contains(pickedHex.trim().lowercase())
    val canConfirm = name.isNotBlank() && !nameTaken && !colorTaken

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nouvelle catégorie", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la catégorie") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameTaken,
                    supportingText = if (nameTaken) {
                        { Text("Ce nom est déjà utilisé.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null
                )

                Text("Couleur choisie", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(pickedColor)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        pickedHex,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (colorTaken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (colorTaken) {
                    Text("Cette couleur est déjà utilisée. Choisis-en une autre.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Text("Couleurs rapides", style = MaterialTheme.typography.labelMedium)
                ColorSwatchRows(
                    colors = QUICK_CATEGORY_COLORS,
                    selectedHex = pickedHex,
                    takenColors = existingColors,
                    onSelect = ::applyHexSelection
                )

                Text("Palettes", style = MaterialTheme.typography.labelMedium)
                CATEGORY_PALETTES.forEach { (paletteName, paletteColors) ->
                    Text(
                        paletteName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorSwatchRows(
                        colors = paletteColors,
                        selectedHex = pickedHex,
                        takenColors = existingColors,
                        onSelect = ::applyHexSelection,
                        swatchesPerRow = 6
                    )
                }

                Text("Personnaliser", style = MaterialTheme.typography.labelMedium)
                Text("Teinte", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onColorChange = { sat, bri ->
                        saturation = sat
                        brightness = bri
                    }
                )

                if (existingCategories.isNotEmpty()) {
                    Text("Couleurs déjà utilisées", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorSwatchRows(
                        colors = existingCategories.map { it.color },
                        selectedHex = pickedHex,
                        takenColors = existingColors,
                        onSelect = {},
                        swatchesPerRow = 10,
                        enabled = false
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Annuler") }
                    Button(
                        onClick = {
                            if (!canConfirm) return@Button
                            val finalColor = if (colorTaken) randomHexColor(existingColors.toList()) else pickedHex
                            onConfirm(name, finalColor)
                        },
                        enabled = canConfirm,
                        modifier = Modifier.weight(1f)
                    ) { Text("Créer") }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRows(
    colors: List<String>,
    selectedHex: String,
    takenColors: Set<String>,
    onSelect: (String) -> Unit,
    swatchesPerRow: Int = 8,
    enabled: Boolean = true
) {
    colors.chunked(swatchesPerRow).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { hex ->
                val normalized = hex.trim().lowercase()
                val isSelected = normalized == selectedHex.trim().lowercase()
                val isTaken = takenColors.contains(normalized)

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(hexToComposeColor(hex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isTaken -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                        .let {
                            if (enabled) {
                                it.clickable { onSelect(hex) }
                            } else {
                                it
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun SaturationBrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (Float, Float) -> Unit
) {
    var pickerWidth by remember { mutableStateOf(1f) }
    var pickerHeight by remember { mutableStateOf(1f) }

    fun applyOffset(offset: Offset) {
        val sat = (offset.x / pickerWidth).coerceIn(0f, 1f)
        val bri = (1f - (offset.y / pickerHeight)).coerceIn(0f, 1f)
        onColorChange(sat, bri)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .onSizeChanged {
                pickerWidth = it.width.toFloat().coerceAtLeast(1f)
                pickerHeight = it.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    applyOffset(offset)
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    applyOffset(change.position)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.hsv(hue, 1f, 1f))
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            val markerX = saturation * size.width
            val markerY = (1f - brightness) * size.height

            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(markerX, markerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = 12.dp.toPx(),
                center = Offset(markerX, markerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
    }

    Text(
        text = "Saturation ${(saturation * 100).roundToInt()}% • Luminosité ${(brightness * 100).roundToInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}