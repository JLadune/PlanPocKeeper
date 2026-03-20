package com.example.planpockeeper.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun PieChartBox(
    title: String,
    entries: List<PieEntry>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(1.2f)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        var pieChartRef by remember { mutableStateOf<PieChart?>(null) }
        val holeColor = MaterialTheme.colorScheme.tertiary.toArgb()

        AndroidView(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center),
            factory = { context ->
                PieChart(context).apply {
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = ColorTemplate.COLORFUL_COLORS.toMutableList()
                    }
                    data = PieData(dataSet)
                    setUsePercentValues(false)
                    isDrawHoleEnabled = true
                    holeRadius = 80f
                    rotationAngle = 270f
                    isRotationEnabled = false
                    description.isEnabled = false
                    legend.isEnabled = false
                    setHoleColor(holeColor)
                    setDrawEntryLabels(false)
                    renderer = SilentPieChartRenderer(this, animator, viewPortHandler)
                    pieChartRef = this
                    invalidate()
                }
            }
        )

        pieChartRef?.let { chart ->
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { context ->
                    PieLabelOverlayView(context, chart)
                }
            )
        }
    }
}