package com.example.planpockeeper.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.repository.AuthRepository
import com.example.planpockeeper.ui.main.MainScreen
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(modifier: Modifier = Modifier) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val initialConnectedEmail = authRepository.currentUser()?.takeIf { it.isEmailVerified }?.email

    var state by remember {
        mutableStateOf(AuthUiState(connectedEmail = initialConnectedEmail))
    }

    fun updateStatus(message: String?) {
        state = state.copy(statusMessage = message)
    }

    fun submitAuth() {
        val cleanedEmail = state.email.trim()
        val cleanedPassword = state.password.trim()

        if (cleanedEmail.isBlank() || cleanedPassword.isBlank()) {
            updateStatus("Email et mot de passe obligatoires.")
            return
        }

        scope.launch {
            state = state.copy(isLoading = true, statusMessage = null)

            val result = if (state.mode == AuthMode.SIGN_UP) {
                if (state.name.isBlank() || state.surname.isBlank()) {
                    state = state.copy(
                        isLoading = false,
                        statusMessage = "Prénom et nom obligatoires pour l'inscription."
                    )
                    return@launch
                }
                authRepository.register(
                    email = cleanedEmail,
                    password = cleanedPassword,
                    surname = state.surname.trim(),
                    name = state.name.trim()
                )
            } else {
                authRepository.login(cleanedEmail, cleanedPassword)
            }

            state = if (result.isSuccess) {
                if (state.mode == AuthMode.SIGN_UP) {
                    state.copy(
                        connectedEmail = null,
                        isLoading = false,
                        statusMessage = "Inscription réussie. Vérifie ton email puis connecte-toi."
                    )
                } else {
                    state.copy(
                        connectedEmail = result.getOrNull()?.email,
                        isLoading = false,
                        statusMessage = "Connexion réussie."
                    )
                }
            } else {
                state.copy(
                    isLoading = false,
                    statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Erreur inconnue."
                )
            }
        }
    }

    fun resendVerificationEmail() {
        val cleanedEmail = state.email.trim()
        val cleanedPassword = state.password.trim()

        if (cleanedEmail.isBlank() || cleanedPassword.isBlank()) {
            updateStatus("Saisis email et mot de passe pour renvoyer le mail de vérification.")
            return
        }

        scope.launch {
            state = state.copy(isLoading = true, statusMessage = null)
            val result = authRepository.resendVerificationEmail(cleanedEmail, cleanedPassword)

            state = if (result.isSuccess) {
                state.copy(
                    isLoading = false,
                    statusMessage = "Email de vérification renvoyé. Vérifie ta boîte mail."
                )
            } else {
                state.copy(
                    isLoading = false,
                    statusMessage = result.exceptionOrNull()?.localizedMessage ?: "Erreur inconnue."
                )
            }
        }
    }

    if (state.connectedEmail != null) {
        MainScreen(
            userEmail = state.connectedEmail,
            onLogout = {
                authRepository.logout()
                state = state.copy(
                    connectedEmail = null,
                    statusMessage = "Déconnexion effectuée."
                )
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        
        AuthForm(
            state = state,
            onModeChange = { mode -> state = state.copy(mode = mode, statusMessage = null) },
            onNameChange = { value -> state = state.copy(name = value) },
            onSurnameChange = { value -> state = state.copy(surname = value) },
            onEmailChange = { value -> state = state.copy(email = value) },
            onPasswordChange = { value -> state = state.copy(password = value) },
            onSubmit = ::submitAuth,
            onResendVerificationEmail = ::resendVerificationEmail
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}