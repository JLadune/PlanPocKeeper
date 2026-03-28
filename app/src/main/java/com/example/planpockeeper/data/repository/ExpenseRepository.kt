package com.example.planpockeeper.data.repository

import com.example.planpockeeper.data.model.Expense
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepository {
    private val db = Firebase.firestore
    private val userId get() = Firebase.auth.currentUser?.uid!!

    private fun expensesRef(budgetId: String) =
        db.collection("users").document(userId)
            .collection("budget").document(budgetId)
            .collection("expenses")

    private fun budgetCategoryRef(budgetId: String, budgetCategoryId: String) =
        db.collection("users").document(userId)
            .collection("budget").document(budgetId)
            .collection("budgetCategories").document(budgetCategoryId)

    // Ajouter une dépense + mettre à jour spentAmount de la catégorie
    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            // Étape 1 — Ajouter la dépense
            val doc = expensesRef(expense.budgetId).document()
            expensesRef(expense.budgetId).document(doc.id)
                .set(expense.copy(id = doc.id)).await()

            // Étape 2 — Incrémenter spentAmount de la catégorie
            budgetCategoryRef(expense.budgetId, expense.categoryId)
                .update("spentAmount", FieldValue.increment(expense.amount)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Supprimer une dépense + décrémenter spentAmount de la catégorie
    suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            // Étape 1 — Supprimer la dépense
            expensesRef(expense.budgetId).document(expense.id)
                .delete().await()

            // Étape 2 — Décrémenter spentAmount de la catégorie
            budgetCategoryRef(expense.budgetId, expense.categoryId)
                .update("spentAmount", FieldValue.increment(-expense.amount)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Modifier une dépense + ajuster spentAmount de la catégorie
    suspend fun updateExpense(oldExpense: Expense, newExpense: Expense): Result<Unit> {
        return try {
            // Étape 1 — Mettre à jour la dépense
            expensesRef(newExpense.budgetId).document(newExpense.id)
                .set(newExpense).await()

            // Étape 2 — Ajuster spentAmount (enlever l'ancien montant, ajouter le nouveau)
            val difference = newExpense.amount - oldExpense.amount
            budgetCategoryRef(newExpense.budgetId, newExpense.categoryId)
                .update("spentAmount", FieldValue.increment(difference)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Historique des dépenses en temps réel (trié par date)
    fun getExpenses(budgetId: String): Flow<List<Expense>> = callbackFlow {
        val listener = expensesRef(budgetId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    // Dépenses d'une seule catégorie en temps réel
    fun getExpensesByCategory(budgetId: String, categoryId: String): Flow<List<Expense>> = callbackFlow {
        val listener = expensesRef(budgetId)
            .whereEqualTo("categoryId", categoryId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }
}