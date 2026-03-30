package com.example.planpockeeper.data.model

data class BudgetCategory(
    val id: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val plannedAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val color: String = ""
)