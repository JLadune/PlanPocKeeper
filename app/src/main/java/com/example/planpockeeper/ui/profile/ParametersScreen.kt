package com.example.planpockeeper.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.utils.EmailHelper
import com.example.planpockeeper.utils.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val budgetRepository = remember { BudgetRepository() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                NotificationHelper.sendPeriodEndNotification(context)
            }) {
                Text("Test notification fin de période")
            }
            Button(onClick = {
                NotificationHelper.sendNoExpenseReminderNotification(context)
            }) {
                Text("Test notification inactivité")
            }
            Button(onClick = {
                scope.launch {
                    val budget = budgetRepository.getActiveBudget() ?: return@launch
                    val result = budgetRepository.rolloverBudget(budget)
                    if (result.isSuccess) {
                        val summary = result.getOrThrow()
                        val user = Firebase.auth.currentUser
                        EmailHelper.sendSummaryEmail(user?.email ?: "", summary)
                    }
                }
            }) {
                Text("Test email récapitulatif")
            }
        }
    }
}