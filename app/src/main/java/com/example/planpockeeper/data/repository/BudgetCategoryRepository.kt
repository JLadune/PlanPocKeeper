package com.example.planpockeeper.data.repository

import com.example.planpockeeper.data.model.BudgetCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BudgetCategoryRepository {
    private val db = Firebase.firestore
    private val userId get() = Firebase.auth.currentUser?.uid!!

    private fun budgetCategoriesRef(budgetId: String) =
        db.collection("users").document(userId)
            .collection("budget").document(budgetId)
            .collection("budgetCategories")

    // Ajouter une catégorie au budget
    suspend fun addBudgetCategory(budgetId: String, budgetCategory: BudgetCategory): Result<Unit> {
        return try {
            val doc = budgetCategoriesRef(budgetId).document()
            budgetCategoriesRef(budgetId).document(doc.id)
                .set(budgetCategory.copy(id = doc.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Modifier le montant prévu d'une catégorie dans le budget
    suspend fun updatePlannedAmount(
        budgetId: String,
        budgetCategoryId: String,
        newAmount: Double
    ): Result<Unit> {
        return try {
            budgetCategoriesRef(budgetId).document(budgetCategoryId)
                .update("plannedAmount", newAmount).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Désactiver une catégorie du budget (plannedAmount = 0)
    suspend fun deactivateBudgetCategory(
        budgetId: String,
        budgetCategoryId: String
    ): Result<Unit> {
        return try {
            budgetCategoriesRef(budgetId).document(budgetCategoryId)
                .update("plannedAmount", 0.0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Récupérer toutes les catégories du budget en temps réel
    fun getBudgetCategories(budgetId: String): Flow<List<BudgetCategory>> = callbackFlow {
        val listener = budgetCategoriesRef(budgetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val categories = snapshot?.toObjects(BudgetCategory::class.java) ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }

    // Récupérer seulement les catégories actives (plannedAmount > 0)
    fun getActiveBudgetCategories(budgetId: String): Flow<List<BudgetCategory>> = callbackFlow {
        val listener = budgetCategoriesRef(budgetId)
            .whereGreaterThan("plannedAmount", 0.0)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val categories = snapshot?.toObjects(BudgetCategory::class.java) ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }
}