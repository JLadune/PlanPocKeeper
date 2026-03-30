package com.example.planpockeeper.data.repository

import com.example.planpockeeper.data.model.Category
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val db = Firebase.firestore
    private val userId get() = Firebase.auth.currentUser?.uid!!

    private fun categoriesRef() =
        db.collection("users").document(userId).collection("categories")

    // Ajouter une catégorie globale
    suspend fun addCategory(category: Category): Result<Unit> {
        return try {
            val doc = categoriesRef().document()
            categoriesRef().document(doc.id)
                .set(category.copy(id = doc.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Modifier le nom ou la couleur d'une catégorie globale
    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoriesRef().document(category.id)
                .set(category).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Supprimer une catégorie globale
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoriesRef().document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Récupérer toutes les catégories globales en temps réel
    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesRef()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val categories = snapshot?.toObjects(Category::class.java) ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }
}