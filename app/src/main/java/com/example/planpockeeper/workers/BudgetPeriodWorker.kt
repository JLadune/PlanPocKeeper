package com.example.planpockeeper.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.utils.EmailHelper
import com.example.planpockeeper.utils.NotificationHelper
import com.example.planpockeeper.utils.PeriodUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class BudgetPeriodWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Ne rien faire si pas connecté
        val user = Firebase.auth.currentUser ?: return Result.success()

        val repository = BudgetRepository()
        val budget = repository.getActiveBudget() ?: return Result.success()

        // La période n'est pas encore finie → rien à faire
        if (!PeriodUtils.isPeriodExpired(budget)) return Result.success()

        // Clôturer la période et récupérer le résumé
        val rolloverResult = repository.rolloverBudget(budget)

        if (rolloverResult.isSuccess) {
            val summary = rolloverResult.getOrThrow()

            // Envoyer la notification locale
            NotificationHelper.sendPeriodEndNotification(applicationContext)

            // Déclencher l'email récapitulatif via Firebase
            EmailHelper.sendSummaryEmail(user.email ?: "", summary)
        }

        return Result.success()
    }
}