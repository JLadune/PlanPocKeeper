package com.example.planpockeeper.data.model

import com.google.firebase.Timestamp

data class Budget(
    val id: String = "",
    val description: String = "",
    val totalAmount: Double = 0.0,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val periodical: Boolean = true,
    val periodicity: String = "mensuel",
    val active: Boolean = true
)