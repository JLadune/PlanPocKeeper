package com.example.planpockeeper.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.planpockeeper.ui.analyse.AnalyseScreen
import com.example.planpockeeper.ui.budget.BudgetScreen
import com.example.planpockeeper.ui.depenses.DepensesScreen
import com.example.planpockeeper.ui.home.HomeScreen

val navItems = listOf("accueil", "budget", "depenses", "analyse")
val navLabels = listOf("Accueil", "Budget", "Dépenses", "Analyse")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentLabel = navLabels.getOrElse(navItems.indexOf(currentRoute)) { "Accueil" }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(currentLabel) })
        },
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, route ->
                    NavigationBarItem(
                        icon = { Text(navLabels[index].first().toString()) },
                        label = { Text(navLabels[index]) },
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "accueil",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("accueil")  { HomeScreen() }
            composable("budget")   { BudgetScreen() }
            composable("depenses") { DepensesScreen() }
            composable("analyse")  { AnalyseScreen() }
        }
    }
}