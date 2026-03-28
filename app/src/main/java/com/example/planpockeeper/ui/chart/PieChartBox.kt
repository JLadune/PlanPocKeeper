package com.example.planpockeeper.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate

@Composable
fun PieChartBox(
    title: String,
    entries: List<PieEntry>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(1.2f)
            .background(
                color = color,
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        var pieChartRef by remember { mutableStateOf<PieChart?>(null) }
        val holeColor = color.copy(alpha = 0f).toArgb()

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
                    setDrawEntryLabels(false)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    renderer = SilentPieChartRenderer(this, animator, viewPortHandler)
                    pieChartRef = this
                    invalidate()
                }
            },
            update = { chart ->
                chart.setHoleColor(holeColor)
                chart.invalidate()
            }
        )

        pieChartRef?.let { chart ->
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { context ->
                    PieLabelOverlayView(context, chart).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                }
            )
        }
    }
}