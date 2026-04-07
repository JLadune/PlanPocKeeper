package com.example.planpockeeper.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = Firebase.auth

    // Inscription
    suspend fun register(email: String, password: String, surname: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            user.sendEmailVerification().await()

            Firebase.firestore
                .collection("users")
                .document(user.uid)
                .set(mapOf(
                    "name" to name,
                    "surname" to surname,
                    "email" to email
                )).await()

            auth.signOut()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Connexion
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            user.reload().await()

            if (!user.isEmailVerified) {
                auth.signOut()
                Result.failure(Exception("Veuillez vérifier votre email avant de vous connecter."))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            user.reload().await()

            if (user.isEmailVerified) {
                auth.signOut()
                Result.failure(Exception("Cet email est déjà vérifié."))
            } else {
                user.sendEmailVerification().await()
                auth.signOut()
                Result.success(Unit)
            }
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

    suspend fun updateCurrency(currency: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Non connecté"))
            Firebase.firestore
                .collection("users")
                .document(userId)
                .update("currency", currency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrency(): String? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val snapshot = Firebase.firestore
                .collection("users")
                .document(userId)
                .get().await()
            snapshot.getString("currency")
        } catch (e: Exception) {
            null
        }
    }

}