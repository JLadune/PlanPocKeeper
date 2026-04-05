package com.example.planpockeeper.ui.profile

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.planpockeeper.data.repository.AuthRepository
import com.example.planpockeeper.utils.CurrencyFormatter
import com.example.planpockeeper.utils.NotificationHelper
import com.example.planpockeeper.utils.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val authRepository = remember { AuthRepository() }
    // Load currency from Firestore on first open
    LaunchedEffect(Unit) {
        val remoteCurrency = authRepository.getCurrency()
        if (remoteCurrency != null) {
            prefsManager.setCurrency(remoteCurrency)
        }
    }
    val scope = rememberCoroutineScope()

    val darkMode by prefsManager.darkMode.collectAsState(initial = false)
    val notifPeriodEnd by prefsManager.notifPeriodEnd.collectAsState(initial = true)
    val notifNoExpense by prefsManager.notifNoExpense.collectAsState(initial = true)
    val currency by prefsManager.currency.collectAsState(initial = "EUR")

    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres",color = MaterialTheme.colorScheme.primary,)},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Apparence ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Apparence",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mode sombre", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Thème foncé pour l'application",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                scope.launch { prefsManager.setDarkMode(it) }
                            }
                        )
                    }
                }
            }

            // ── Devise ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Devise",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Devise affichée", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Actuellement : ${CurrencyFormatter.currencies.firstOrNull { it.code == currency }?.let { "${it.label} (${it.symbol})" } ?: currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            CurrencyFormatter.getSymbol(currency),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Exemple d'affichage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyFormatter.format(1234.56, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Dépense : ${CurrencyFormatter.format(49.99, currency)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Restant : ${CurrencyFormatter.format(1184.57, currency)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Notifications ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Fin de période",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Rappel avant la fin de votre période budgétaire",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifPeriodEnd,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        prefsManager.setNotifPeriodEnd(enabled)
                                        if (enabled) {
                                            NotificationHelper.createNotificationChannel(context)
                                            NotificationHelper.sendPeriodEndNotification(context)
                                        }
                                    }
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Absence de dépenses",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Rappel si aucune dépense depuis 3 jours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifNoExpense,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        prefsManager.setNotifNoExpense(enabled)
                                        if (enabled) {
                                            NotificationHelper.createNotificationChannel(context)
                                            NotificationHelper.sendNoExpenseReminderNotification(context)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ── Currency picker dialog ──
    if (showCurrencyDialog) {
        Dialog(onDismissRequest = { showCurrencyDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Choisir une devise",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CurrencyFormatter.currencies.forEach { currencyInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        prefsManager.setCurrency(currencyInfo.code)
                                        authRepository.updateCurrency(currencyInfo.code)
                                    }
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    currencyInfo.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(32.dp)
                                )
                                Column {
                                    Text(currencyInfo.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        currencyInfo.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (currency == currencyInfo.code) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = "Sélectionné",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (currencyInfo != CurrencyFormatter.currencies.last()) {
                            HorizontalDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showCurrencyDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Fermer") }
                }
            }
        }
    }
}