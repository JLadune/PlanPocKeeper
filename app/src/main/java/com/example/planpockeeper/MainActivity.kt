package com.example.planpockeeper

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanPocKeeperTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("auth") {
            AuthScreen()
        }
    }
}