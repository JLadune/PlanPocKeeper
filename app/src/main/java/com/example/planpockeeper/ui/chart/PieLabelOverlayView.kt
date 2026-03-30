package com.example.planpockeeper.ui.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieDataSet
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// View overlay qui couvre toute la Box
class PieLabelOverlayView(
    context: Context,
    private val pieChart: PieChart
) : View(context) {

    fun refresh() {
        invalidate()
    }

    private val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val valuePaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 35f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        // Se redessiner quand le PieChart se redessine
        pieChart.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            invalidate()
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (pieChart.data == null) return
        if (pieChart.width == 0 || pieChart.height == 0) return
        if (pieChart.data.dataSetCount == 0) return
        if (pieChart.drawAngles == null || pieChart.drawAngles.isEmpty()) return

        val dataSet = pieChart.data.dataSets[0] as PieDataSet
        val count = dataSet.entryCount
        if (count == 0) return
        val offsetX = (width - pieChart.width) / 2f
        val offsetY = (height - pieChart.height) / 2f
        val centerX = offsetX + pieChart.width / 2f
        val centerY = offsetY + pieChart.height / 2f

        val outerRadius = pieChart.radius
        val holeRadius = pieChart.holeRadius / 100f * outerRadius

        // Marges par rapport aux bords de la Box (this.width / this.height)
        val margin = 16f
        val ringGap = 14f  // espace minimum entre label et anneau

        val drawAngles = pieChart.drawAngles
        val absoluteAngles = pieChart.absoluteAngles
        val textHeight = textPaint.textSize
        val blockH = textHeight + valuePaint.textSize + 6f

        data class LabelBounds(val x: Float, val y: Float, val w: Float, val h: Float)
        val placed = mutableListOf<LabelBounds>()

        for (i in 0 until count) {
            val entry = dataSet.getEntryForIndex(i)
            var label = entry.label ?: continue
            val value = entry.y.toInt()
            if (label.length > 15) label = label.substring(0, 15) + "…"

            val angle = pieChart.rotationAngle +
                    (if (i > 0) absoluteAngles[i - 1] else 0f) +
                    drawAngles[i] / 2f
            val rad = Math.toRadians(angle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()

            val labelW = textPaint.measureText(label)
            val valueW = valuePaint.measureText("$value%")
            val blockW = maxOf(labelW, valueW)

            // ── Tentative 1 : extérieur de l'anneau ─────────────────────────
            // Le centre du bloc texte est placé à outerRadius + gap + demi-hauteur
            // dans la direction radiale
            val extR = outerRadius + ringGap + blockH / 2f
            var cx = centerX + dx * extR
            var cy = centerY + dy * extR

            // Clamper dans les bords de la Box
            cx = cx.coerceIn(margin + blockW / 2f, width - margin - blockW / 2f)
            cy = cy.coerceIn(margin + textHeight, height - margin - blockH)

            // Vérifier que après clamp on ne chevauche plus l'anneau
            val distAfterClamp = hypot(cx - centerX, cy - centerY)
            val isOutsideRing = distAfterClamp >= outerRadius + ringGap

            val finalCx: Float
            var finalCy: Float

            if (isOutsideRing) {
                // Position extérieure valide
                finalCx = cx
                finalCy = cy
            } else {
                // ── Tentative 2 : intérieur (dans le trou) ──────────────────
                val innerR = holeRadius * 0.6f
                var icx = centerX + dx * innerR
                var icy = centerY + dy * innerR

                // Clamper dans le trou
                icx = icx.coerceIn(centerX - holeRadius + margin, centerX + holeRadius - margin)
                icy = icy.coerceIn(centerY - holeRadius + textHeight, centerY + holeRadius - blockH)

                finalCx = icx
                finalCy = icy
            }

            // ── Anti-chevauchement vertical ──────────────────────────────────
            var resolvedY = finalCy
            for (prev in placed) {
                val overlapX = (finalCx - blockW / 2f) < (prev.x + prev.w) &&
                        (finalCx + blockW / 2f) > prev.x
                val overlapY = (resolvedY - textHeight) < (prev.y + prev.h) &&
                        (resolvedY + blockH) > prev.y
                if (overlapX && overlapY) {
                    resolvedY = prev.y + prev.h + textHeight + 4f
                }
            }
            placed.add(LabelBounds(finalCx - blockW / 2f, resolvedY - textHeight, blockW, blockH))

            // ── Dessiner ─────────────────────────────────────────────────────
            c.drawText(label, finalCx, resolvedY, textPaint)
            c.drawText("$value%", finalCx, resolvedY + textHeight + 6f, valuePaint)
        }
    }
}