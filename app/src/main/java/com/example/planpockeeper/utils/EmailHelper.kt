package com.example.planpockeeper.utils

import com.example.planpockeeper.data.model.BudgetSummary
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

object EmailHelper {

    fun sendSummaryEmail(userEmail: String, summary: BudgetSummary) {
        Firebase.firestore.collection("mail").add(
            mapOf(
                "to" to userEmail,
                "message" to mapOf(
                    "subject" to "Récapitulatif de votre période budgétaire",
                    "html" to buildEmailHtml(summary)
                )
            )
        )
    }

    private fun buildEmailHtml(summary: BudgetSummary): String {
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val isBudgetOver = summary.totalSpent > summary.totalPlanned
        val totalDiff = summary.totalSpent - summary.totalPlanned
        val overCategories = summary.categoryTotals
            .filter { (name, spent) ->
                // on compare avec le plannedAmount si dispo, sinon on skip
                spent > (summary.categoryPlanned[name] ?: Double.MAX_VALUE)
            }
        val underCategories = summary.categoryTotals
            .filter { (name, spent) ->
                spent < (summary.categoryPlanned[name] ?: 0.0)
            }
        val savingsPossible = underCategories.entries.sumOf { (name, spent) ->
            val planned = summary.categoryPlanned[name] ?: 0.0
            (planned - spent) * 0.5
        }

        // ── Lignes du tableau récapitulatif ──
        val tableRows = summary.categoryTotals.entries.joinToString("") { (name, spent) ->
            val planned = summary.categoryPlanned[name] ?: 0.0
            val isOver = spent > planned
            val diff = spent - planned
            val borderColor = if (isOver) "#B00020" else "#4CAF50"
            val diffLabel = "${if (isOver) "-" else "+"}${abs(diff).toInt()}€"
            """
            <tr>
              <td style="border-left: 4px solid $borderColor; padding-left: 8px;">$name</td>
              <td>${spent.toInt()}€ / ${planned.toInt()}€</td>
              <td style="color: $borderColor; font-weight: bold;">${if (isOver) "OUI" else "NON"} ($diffLabel)</td>
            </tr>
            """
        }

        // ── Catégories dépassées ──
        val overCatHtml = if (overCategories.isNotEmpty()) {
            "<p><strong>Catégories dépassées :</strong></p><ul>" +
                    overCategories.keys.joinToString("") { "<li style='color:#B00020;'>$it</li>" } +
                    "</ul>"
        } else ""

        // ── Économies possibles ──
        val savingsHtml = if (savingsPossible > 0) {
            "<p><strong>Économie possible :</strong> ${savingsPossible.toInt()}€</p>" +
                    "<p>Économie dans quelles catégories :</p><ul>" +
                    underCategories.entries.joinToString("") { (name, spent) ->
                        val planned = summary.categoryPlanned[name] ?: 0.0
                        val saving = (planned - spent) * 0.5
                        "<li><strong>$name</strong> : ${saving.toInt()}€</li>"
                    } + "</ul>"
        } else {
            "<p>Aucune économie réalisée, il faudrait revoir le budget.</p>"
        }

        return """
        <div style="font-family: sans-serif; max-width: 600px; margin: auto; color: #333;">

          <h2 style="border-bottom: 2px solid #eee; padding-bottom: 8px;">
            Récapitulatif budgétaire
          </h2>

          <p>Période : <strong>${df.format(summary.periodStart)}</strong> → <strong>${df.format(summary.periodEnd)}</strong></p>

          <!-- Bilan -->
          <h3>Bilan</h3>
          <p>
            Budget dépassé ?
            <strong style="color: ${if (isBudgetOver) "#B00020" else "#4CAF50"};">
              ${if (isBudgetOver) "OUI" else "NON"}
            </strong>
          </p>
          $overCatHtml
          <p>
            ${if (isBudgetOver) "Excès" else "Économie"} sur le budget :
            <strong>${if (isBudgetOver) "-" else "+"}${abs(totalDiff).toInt()}€</strong>
          </p>

          <!-- Tableau récapitulatif -->
          <h3>Tableau récapitulatif</h3>
          <table style="width:100%; border-collapse: collapse; font-size: 14px;">
            <thead>
              <tr style="background: #f5f5f5;">
                <th style="text-align:left; padding: 8px;">Catégorie</th>
                <th style="text-align:left; padding: 8px;">Dépensé / Prévu</th>
                <th style="text-align:left; padding: 8px;">Déficit</th>
              </tr>
            </thead>
            <tbody>$tableRows</tbody>
          </table>

          <!-- Analyse -->
          <h3>Analyse</h3>
          $savingsHtml

          <p style="margin-top: 32px; font-size: 12px; color: #999;">
            Cet email a été généré automatiquement par PlanPocKeeper.
          </p>
        </div>
        """.trimIndent()
    }
}