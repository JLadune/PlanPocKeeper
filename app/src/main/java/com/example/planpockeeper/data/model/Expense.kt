package com.example.planpockeeper.data.model

import com.google.firebase.Timestamp

data class Expense(
    val id: String = "",
    val budgetId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: Timestamp = Timestamp.now()
)