package com.example.planpockeeper.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.planpockeeper.ui.analyse.AnalyseScreen
import com.example.planpockeeper.ui.budget.BudgetScreen
import com.example.planpockeeper.ui.depenses.DepensesScreen
import com.example.planpockeeper.ui.home.HomeScreen

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val navItems = listOf(
    NavItem("accueil",  "Accueil",  Icons.Outlined.Home),
    NavItem("budget",   "Budget",   Icons.Outlined.Wallet),
    NavItem("depenses", "Dépenses", Icons.Outlined.Payments),
    NavItem("analyse",  "Analyse",  Icons.Outlined.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentLabel = navItems.firstOrNull { it.route == currentRoute }?.label ?: "Accueil"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(currentLabel) })
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
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