package com.example.planpockeeper.utils

import android.content.Context
import androidx.work.*
import com.example.planpockeeper.workers.BudgetPeriodWorker
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val BUDGET_CHECK_WORK = "budget_period_check"

    fun schedulePeriodCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BudgetPeriodWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BUDGET_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP, // ne pas remplacer si déjà planifié
            request
        )
    }
}