package com.example.planpockeeper.ui.chart

import android.graphics.Canvas
import android.graphics.Paint
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.utils.ViewPortHandler
import com.github.mikephil.charting.data.PieDataSet
import kotlin.math.cos
import kotlin.math.sin

class CustomPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : PieChartRenderer(chart, animator, viewPortHandler) {

    private val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun drawValues(c: Canvas) {
        val pieChart = mChart ?: return
        if (pieChart.data == null) return

        val dataSet = pieChart.data.dataSets[0] as PieDataSet
        val count = dataSet.entryCount

        val centerX = pieChart.width / 2f
        val centerY = pieChart.height / 2f
        val radius = pieChart.radius + 100f

        val drawAngles = pieChart.drawAngles
        val absoluteAngles = pieChart.absoluteAngles

        for (i in 0 until count) {
            val entry = dataSet.getEntryForIndex(i)
            val label = entry.label
            val value = entry.y.toInt()

            val angle = pieChart.rotationAngle + drawAngles[i] / 2 + if (i > 0) absoluteAngles[i - 1] else 0f
            val rad = Math.toRadians(angle.toDouble())
            val x = (centerX + radius * cos(rad)).toFloat()
            val y = (centerY + radius * sin(rad)).toFloat()

            // Dessiner label et valeur sur 2 lignes
            c.drawText(label, x, y, textPaint)
            c.drawText("$value%", x, y + 45f, textPaint)
        }
    }
}