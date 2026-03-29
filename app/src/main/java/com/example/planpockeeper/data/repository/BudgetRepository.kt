package com.example.planpockeeper.data.repository

import com.example.planpockeeper.data.model.Budget
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BudgetRepository {
    private val db = Firebase.firestore
    private val userId get() = Firebase.auth.currentUser?.uid!!

    private fun budgetRef() =
        db.collection("users").document(userId).collection("budget")

    // Créer le budget actif
    suspend fun createBudget(budget: Budget): Result<Unit> {
        return try {
            val doc = budgetRef().document()
            budgetRef().document(doc.id)
                .set(budget.copy(id = doc.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Récupérer le budget actif
    suspend fun getActiveBudget(): Budget? {
        val snapshot = budgetRef()
            .whereEqualTo("active", true)
            .get().await()
        return snapshot.toObjects(Budget::class.java).firstOrNull()
    }

    // Modifier le budget
    suspend fun updateBudget(budget: Budget): Result<Unit> {
        return try {
            budgetRef().document(budget.id)
                .set(budget).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Supprimer le budget + toutes ses budgetCategories et dépenses
    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            val budgetDoc = budgetRef().document(budgetId)

            // Supprimer toutes les budgetCategories liées
            val categories = budgetDoc.collection("budgetCategories").get().await()
            categories.documents.forEach { it.reference.delete().await() }

            // Supprimer toutes les dépenses liées
            val expenses = budgetDoc.collection("expenses").get().await()
            expenses.documents.forEach { it.reference.delete().await() }

            // Supprimer le budget lui-même
            budgetDoc.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Désactiver le budget actif (au lieu de supprimer)
    suspend fun deactivateBudget(budgetId: String): Result<Unit> {
        return try {
            budgetRef().document(budgetId)
                .update("active", false).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Écouter le budget actif en temps réel
    fun getActiveBudgetFlow(): Flow<Budget?> = callbackFlow {
        val listener = budgetRef()
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val budget = snapshot?.toObjects(Budget::class.java)?.firstOrNull()
                trySend(budget)
            }
        awaitClose { listener.remove() }
    }
}