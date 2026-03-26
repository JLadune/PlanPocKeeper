package com.example.planpockeeper.ui.chart

import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class SilentPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : PieChartRenderer(chart, animator, viewPortHandler) {
    override fun drawValues(c: Canvas) { }
}