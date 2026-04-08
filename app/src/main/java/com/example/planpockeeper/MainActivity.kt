package com.example.planpockeeper

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.planpockeeper.utils.PreferencesManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.planpockeeper.ui.auth.AuthScreen
import com.example.planpockeeper.ui.splash.SplashScreen
import com.example.planpockeeper.ui.theme.PlanPocKeeperTheme
import com.example.planpockeeper.utils.NotificationHelper
import com.example.planpockeeper.utils.WorkScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(applicationContext)
        WorkScheduler.schedulePeriodCheck(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        setContent {
            val prefsManager = remember { PreferencesManager(applicationContext) }
            val darkMode by prefsManager.darkMode.collectAsState(initial = false)

            PlanPocKeeperTheme(darkMode = darkMode) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onFinished = {
                navController.navigate("auth") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("auth") {
            AuthScreen()
        }
    }
}