package com.example.planpockeeper

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
    AndroidView(
        factory = { context ->
            PieChart(context).apply {

                val entries = listOf(
                    PieEntry(40f, "A"),
                    PieEntry(30f, "B"),
                    PieEntry(20f, "C"),
                    PieEntry(10f, "D")
                )

                val dataSet = PieDataSet(entries, "Reel")
                dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()

                val data = PieData(dataSet)
                this.data = data

                this.isDrawHoleEnabled = true
                this.holeRadius = 80f        //% du trou

                setUsePercentValues(true)
                description.isEnabled = false
                legend.isEnabled = false

                this.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                invalidate()
            }
        }
    )
}