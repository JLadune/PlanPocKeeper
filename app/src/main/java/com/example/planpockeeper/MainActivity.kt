package com.example.planpockeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.planpockeeper.ui.auth.AuthScreen
import com.example.planpockeeper.ui.theme.PlanPocKeeperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanPocKeeperTheme {
                AuthScreen()
            }
        }
    }
}