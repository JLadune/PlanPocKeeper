package com.example.planpockeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.planpockeeper.ui.chart.CustomPieChartRenderer
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PieChartScreen()
        }
    }
}

@Composable
fun PieChartScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.size(300.dp),
            factory = { context ->
                PieChart(context).apply {

                    val entries = listOf(
                        PieEntry(40f, "Catégorie 1"),
                        PieEntry(30f, "Catégorie 2"),
                        PieEntry(30f, "Catégorie 3")
                    )

                    val dataSet = PieDataSet(entries, "").apply {
                        colors = ColorTemplate.COLORFUL_COLORS.toMutableList()
                        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                        valueLineColor = android.graphics.Color.GRAY
                    }

                    data = PieData(dataSet)
                    setUsePercentValues(false)
                    isDrawHoleEnabled = true
                    holeRadius = 75f

                    rotationAngle = 270f
                    isRotationEnabled = false

                    description.isEnabled = false
                    legend.isEnabled = false

                    setDrawEntryLabels(false) // labels normaux désactivés

                    // appliquer notre renderer personnalisé
                    renderer = CustomPieChartRenderer(this, animator, viewPortHandler)

                    invalidate()
                }
            }
        )
    }
}