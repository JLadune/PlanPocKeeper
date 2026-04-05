package com.example.planpockeeper.data.model

import java.util.Date

data class BudgetSummary(
    val budgetDescription: String,
    val totalPlanned: Double,
    val totalSpent: Double,
    val categoryTotals: Map<String, Double>,
    val categoryPlanned: Map<String, Double>,
    val periodStart: Date,
    val periodEnd: Date
)