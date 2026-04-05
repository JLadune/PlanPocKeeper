package com.example.planpockeeper.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.example.planpockeeper.utils.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametresScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                Text("Tester la notification")
            }
        }
    }
}