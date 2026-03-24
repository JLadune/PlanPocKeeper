package com.example.planpockeeper.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = Firebase.auth

    // Inscription
    suspend fun register(email: String, password: String, surname: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            Firebase.firestore
                .collection("users")
                .document(user.uid)
                .set(mapOf(
                    "name" to name,
                    "surname" to surname,
                    "email" to email
                )).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Connexion
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Déconnexion
    fun logout() {
        auth.signOut()
    }

    // Récupération de l'utilisateur courant
    fun currentUser() = auth.currentUser

    // Récupération des informations de l'utilisateur
    suspend fun getUserInfo(): Map<String, Any>? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = Firebase.firestore
            .collection("users")
            .document(userId)
            .get().await()
        return snapshot.data
    }

}