package com.example.planpockeeper.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.repository.AuthRepository
import com.example.planpockeeper.ui.main.MainScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

private const val ALLOWED_SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{}|;:',.<>?/"

private fun mapAuthErrorMessage(rawMessage: String?): String {
    val fallback = "Une erreur est survenue."
    val message = rawMessage?.trim()?.lowercase() ?: return fallback

    return when {
        "password is invalid" in message ||
            "invalid login credentials" in message ||
            "there is no user record" in message ||
            "credential is incorrect" in message ||
            "credential is incorect" in message ||
            "invalid credential" in message -> "E-mail ou mot de passe incorrect."

        "email address is badly formatted" in message ||
            "badly formatted" in message -> "Le format de l'e-mail est invalide."

        "email address is already in use" in message ||
            "already in use" in message -> "Cette adresse e-mail est déjà utilisée."

        "too many requests" in message -> "Trop de tentatives. Réessaie dans quelques instants."

        "canceled" in message ||
            "cancelled" in message -> "Connexion Google annulée."

        "network" in message ||
            "timeout" in message -> "Problème réseau. Vérifie ta connexion Internet puis réessaie."

        else -> rawMessage ?: fallback
    }
}

private fun signUpPasswordError(password: String): String? {
    if (password.length < 8) {
        return "Le mot de passe doit contenir au moins 8 caractères."
    }
    if (password.none { it.isDigit() }) {
        return "Le mot de passe doit contenir au moins un nombre."
    }
    if (password.none { it.isLowerCase() }) {
        return "Le mot de passe doit contenir au moins une lettre minuscule."
    }
    if (password.none { it.isUpperCase() }) {
        return "Le mot de passe doit contenir au moins une lettre majuscule."
    }
    if (password.none { it in ALLOWED_SPECIAL_CHARACTERS }) {
        return "Le mot de passe doit contenir au moins un caractère spécial parmi : $ALLOWED_SPECIAL_CHARACTERS"
    }
    return null
}

@Composable
fun AuthScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val initialConnectedEmail = authRepository.currentUser()?.takeIf { it.isEmailVerified }?.email

    var state by remember {
        mutableStateOf(AuthUiState(connectedEmail = initialConnectedEmail))
    }

    fun updateStatus(message: String?) {
        state = state.copy(statusMessage = message)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                updateStatus("Impossible de récupérer le jeton Google.")
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                state = state.copy(isLoading = true, statusMessage = null)
                val loginResult = authRepository.loginWithGoogleIdToken(idToken)
                state = if (loginResult.isSuccess) {
                    state.copy(
                        connectedEmail = loginResult.getOrNull()?.email,
                        isLoading = false,
                        statusMessage = "Connexion Google réussie."
                    )
                } else {
                    state.copy(
                        isLoading = false,
                        statusMessage = mapAuthErrorMessage(loginResult.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        } catch (e: ApiException) {
            updateStatus(mapAuthErrorMessage(e.localizedMessage))
        }
    }

    fun loginWithGoogle() {
        val webClientIdRes = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (webClientIdRes == 0) {
            updateStatus("Client Google non configuré. Vérifie Firebase et google-services.json.")
            return
        }

        val webClientId = context.getString(webClientIdRes)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun submitAuth() {
        val cleanedEmail = state.email.trim()
        val cleanedPassword = state.password.trim()
        val cleanedConfirmPassword = state.confirmPassword.trim()

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

                if (cleanedPassword != cleanedConfirmPassword) {
                    state = state.copy(
                        isLoading = false,
                        statusMessage = "Les mots de passe ne correspondent pas."
                    )
                    return@launch
                }

                val passwordError = signUpPasswordError(cleanedPassword)
                if (passwordError != null) {
                    state = state.copy(
                        isLoading = false,
                        statusMessage = passwordError
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
                    statusMessage = mapAuthErrorMessage(result.exceptionOrNull()?.localizedMessage)
                )
            }
        }
    }

    fun resendVerificationEmail() {
        val cleanedEmail = state.email.trim()
        val cleanedPassword = state.password.trim()

        if (cleanedEmail.isBlank() || cleanedPassword.isBlank()) {
            updateStatus("Saisis un e-mail et un mot de passe pour renvoyer l'e-mail de vérification.")
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
                    statusMessage = mapAuthErrorMessage(result.exceptionOrNull()?.localizedMessage)
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        
        AuthForm(
            state = state,
            onModeChange = { mode ->
                state = state.copy(
                    mode = mode,
                    statusMessage = null,
                    confirmPassword = ""
                )
            },
            onNameChange = { value -> state = state.copy(name = value) },
            onSurnameChange = { value -> state = state.copy(surname = value) },
            onEmailChange = { value -> state = state.copy(email = value) },
            onPasswordChange = { value -> state = state.copy(password = value) },
            onConfirmPasswordChange = { value -> state = state.copy(confirmPassword = value) },
            onSubmit = ::submitAuth,
            onResendVerificationEmail = ::resendVerificationEmail,
            onGoogleSignIn = ::loginWithGoogle
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}