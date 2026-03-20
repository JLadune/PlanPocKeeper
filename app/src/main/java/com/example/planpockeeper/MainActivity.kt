package com.example.planpockeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.planpockeeper.ui.chart.SilentPieChartRenderer
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.example.planpockeeper.ui.chart.PieLabelOverlayView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

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

        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1.2f)
                .background(
                    color = Color(0x80FF0000),
                    shape = RoundedCornerShape(25.dp)
                ),

        ) {
            Text(
                text = "Réel",
                fontSize = 22.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            var pieChartRef by remember { mutableStateOf<PieChart?>(null) }

            AndroidView(
                modifier = Modifier.size(250.dp)
                    .align(Alignment.Center),
                factory = { context ->
                    PieChart(context).apply {
                        val entries = listOf(
                            PieEntry(40f, "Catégorie 1"),
                            PieEntry(30f, "Catégorie 2"),
                            PieEntry(30f, "Catégorie 3"),
                            PieEntry(30f, "Catégorie 3"),
                            PieEntry(30f, "Catégorie 3"),
                            PieEntry(30f, "Catégorie 3")
                        )
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

                        // Renderer qui ne dessine RIEN (on gère les labels sur l'overlay)
                        renderer = SilentPieChartRenderer(this, animator, viewPortHandler)

                        pieChartRef = this
                        invalidate()
                    }
                }
            )

            // Overlay transparent qui couvre toute la Box et dessine les labels
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
}

