package com.example.planpockeeper.data.repository

import com.example.planpockeeper.data.model.Budget
import com.example.planpockeeper.data.model.BudgetSummary
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.planpockeeper.utils.PeriodUtils
import com.google.firebase.Timestamp

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

// Nouvelle méthode à ajouter dans la classe BudgetRepository

    /**
     * Clôture la période courante et en démarre une nouvelle :
     * 1. Supprime toutes les dépenses de la période
     * 2. Remet spentAmount à 0 sur chaque budgetCategory
     * 3. Met à jour la startDate du budget pour la prochaine période
     *
     * @return Le résumé des dépenses avant suppression (pour l'email)
     */
    suspend fun rolloverBudget(budget: Budget): Result<BudgetSummary> {
        return try {
            val budgetDoc = budgetRef().document(budget.id)

            // 1. Récupérer les dépenses pour le résumé
            val expensesSnap = budgetDoc.collection("expenses").get().await()
            val expenses = expensesSnap.documents.mapNotNull { doc ->
                doc.toObject(com.example.planpockeeper.data.model.Expense::class.java)
            }

            // 2. Construire le résumé avant suppression
            val categoryTotals = expenses
                .groupBy { it.categoryName }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val summary = BudgetSummary(
                budgetDescription = budget.description,
                totalPlanned = budget.totalAmount,
                totalSpent = expenses.sumOf { it.amount },
                categoryTotals = categoryTotals,
                periodStart = budget.startDate.toDate(),
                periodEnd = PeriodUtils.computeEndDate(budget)
            )

            // 3. Supprimer toutes les dépenses
            expensesSnap.documents.forEach { it.reference.delete().await() }

            // 4. Remettre spentAmount à 0 sur chaque catégorie
            val categoriesSnap = budgetDoc.collection("budgetCategories").get().await()
            categoriesSnap.documents.forEach { doc ->
                doc.reference.update("spentAmount", 0.0).await()
            }

            // 5. Avancer la startDate à la prochaine période
            val nextStart = PeriodUtils.computeNextStartDate(budget)
            budgetDoc.update("startDate", Timestamp(nextStart)).await()

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}