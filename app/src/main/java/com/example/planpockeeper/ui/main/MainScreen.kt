package com.example.planpockeeper.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.planpockeeper.ui.analyse.AnalyseScreen
import com.example.planpockeeper.ui.budget.BudgetScreen
import com.example.planpockeeper.ui.depenses.DepensesScreen
import com.example.planpockeeper.ui.home.HomeScreen
import com.example.planpockeeper.ui.profile.InfosCompteScreen
import com.example.planpockeeper.ui.profile.ParametresScreen

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val navItems = listOf(
    NavItem("accueil",  "Accueil",  Icons.Outlined.Home),
    NavItem("budget",   "Budget",   Icons.Outlined.Savings),
    NavItem("depenses", "Dépenses", Icons.Outlined.Payments),
    NavItem("analyse",  "Analyse",  Icons.Outlined.BarChart)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit, userEmail: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentLabel = navItems.firstOrNull { it.route == currentRoute }?.label ?: "Accueil"

    var profileOpen by remember { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth = screenWidth * 0.80f

    val offsetX by animateDpAsState(
        targetValue = if (profileOpen) 0.dp else panelWidth,
        animationSpec = tween(durationMillis = 300),
        label = "profilePanel"
    )

    val barColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("file:///android_asset/logo.svg")
                                .decoderFactory(SvgDecoder.Factory())
                                .build(),
                            contentDescription = "Logo",
                            modifier = Modifier.height(100.dp)
                                .offset(x = (-16).dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { profileOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profil",
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = barColor,
                        titleContentColor = contentColor,
                        actionIconContentColor = contentColor
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = barColor) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = contentColor,
                                selectedTextColor = contentColor,
                                indicatorColor = barColor,
                                unselectedIconColor = contentColor.copy(alpha = 0.5f),
                                unselectedTextColor = contentColor.copy(alpha = 0.5f)
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
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
                composable("accueil")      { HomeScreen() }
                composable("budget")       { BudgetScreen() }
                composable("depenses")     { DepensesScreen() }
                composable("analyse")      { AnalyseScreen() }
                composable("infos_compte") {
                    InfosCompteScreen(
                        onBack = { navController.popBackStack() },
                        onAccountDeleted = { onLogout() },
                        onEmailChangeRequiresLogout = { onLogout() }
                    )
                }
                composable("parametres")   { ParametresScreen(onBack = { navController.popBackStack() }) }
            }
        }

        if (profileOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { profileOpen = false }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(panelWidth)
                .offset(x = offsetX)
                .align(Alignment.TopEnd)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { profileOpen = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Retour",
                            tint = contentColor
                        )
                    }
                    Text("Retour", style = MaterialTheme.typography.bodyMedium, color = contentColor)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Photo de profil",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Utilisateur",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                        Text(
                            text = userEmail ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        profileOpen = false
                        navController.navigate("infos_compte")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Informations de compte", style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }

                TextButton(
                    onClick = {
                        profileOpen = false
                        navController.navigate("parametres")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paramètres de l'application", style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { profileOpen = false; onLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Se déconnecter")
                }
            }
        }
    }
}