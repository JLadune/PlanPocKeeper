package com.example.planpockeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.github.mikephil.charting.data.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.planpockeeper.ui.chart.PieChartBox
import com.example.planpockeeper.ui.theme.PlanPocKeeperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanPocKeeperTheme {
                PieChartScreen()
            }
        }
    }
}

@Composable
fun PieChartScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Graphiques Camemberts",
            textDecoration = TextDecoration.Underline,
            fontSize = 25.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, bottom = 8.dp)
        )

        PieChartBox(
            title = "Réel",
            entries = listOf(
                PieEntry(40f, "Catégorie 1"),
                PieEntry(30f, "Catégorie 2"),
                PieEntry(30f, "Catégorie 3")
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        PieChartBox(
            title = "Prévu",
            entries = listOf(
                PieEntry(50f, "Catégorie A"),
                PieEntry(50f, "Catégorie B")
            )
        )
    }
}

