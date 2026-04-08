package com.example.planpockeeper.utils

import com.example.planpockeeper.data.model.Budget
import com.google.firebase.Timestamp
import java.util.*

object PeriodUtils {

    //Retourne la date de fin de la période courante
    fun computeEndDate(budget: Budget): Date {
        // Budget ponctuel avec date de fin explicite
        if (!budget.periodical && budget.endDate != null) {
            return budget.endDate.toDate()
        }

        val start = budget.startDate.toDate()
        val cal = Calendar.getInstance().apply { time = start }

        return when {
            budget.periodicity == "hebdomadaire" -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.time
            }
            budget.periodicity == "mensuel" -> {
                cal.add(Calendar.MONTH, 1)
                cal.time
            }
            budget.periodicity == "annuel" -> {
                cal.add(Calendar.YEAR, 1)
                cal.time
            }
            budget.periodicity.startsWith("custom_") -> {
                val days = budget.periodicity
                    .removePrefix("custom_")
                    .removeSuffix("j")
                    .toIntOrNull() ?: 30
                cal.add(Calendar.DAY_OF_YEAR, days)
                cal.time
            }
            else -> {
                cal.add(Calendar.MONTH, 1)
                cal.time
            }
        }
    }

    //Retourne la date de début de la période suivante (= fin de l'actuelle)
    fun computeNextStartDate(budget: Budget): Date = computeEndDate(budget)

    //Vérifie si la période est terminée
    fun isPeriodExpired(budget: Budget): Boolean {
        return computeEndDate(budget).before(Date())
    }
}