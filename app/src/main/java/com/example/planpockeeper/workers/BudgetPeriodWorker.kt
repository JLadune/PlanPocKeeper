package com.example.planpockeeper.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.planpockeeper.data.repository.BudgetRepository
import com.example.planpockeeper.data.repository.ExpenseRepository
import com.example.planpockeeper.utils.EmailHelper
import com.example.planpockeeper.utils.NotificationHelper
import com.example.planpockeeper.utils.PeriodUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class BudgetPeriodWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = Firebase.auth.currentUser ?: return Result.success()
        val repository = BudgetRepository()
        val budget = repository.getActiveBudget() ?: return Result.success()

        // ── Vérification fin de période (existant) ──
        if (PeriodUtils.isPeriodExpired(budget)) {
            val rolloverResult = repository.rolloverBudget(budget)
            if (rolloverResult.isSuccess) {
                val summary = rolloverResult.getOrThrow()
                NotificationHelper.sendPeriodEndNotification(applicationContext)
                EmailHelper.sendSummaryEmail(user.email ?: "", summary)
            }
        }

        // ── Vérification inactivité 3 jours ──
        val expenseRepository = ExpenseRepository()
        val lastExpenseDate = expenseRepository.getLastExpenseDate(budget.id)

        if (lastExpenseDate != null) {
            val daysSinceLast = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - lastExpenseDate.time
            )
            if (daysSinceLast >= 3) {
                NotificationHelper.sendNoExpenseReminderNotification(applicationContext)
            }
        }

        return Result.success()
    }
}