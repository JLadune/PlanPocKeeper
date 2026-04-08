package com.example.planpockeeper.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.planpockeeper.data.repository.AuthRepository
import kotlinx.coroutines.launch

private fun mapFirebaseErrorMessage(rawMessage: String?): String {
    val fallback = "Une erreur est survenue."
    val message = rawMessage?.trim()?.lowercase() ?: return fallback

    return when {
        "credential is incorrect" in message ||
            "credential is incorect" in message ||
            "malformed" in message ||
            "expired" in message ||
            "invalid credential" in message -> "Mot de passe actuel incorrect. Merci de réessayer."

        "requires recent login" in message ||
            "recent authentication" in message -> "Session expirée. Déconnecte-toi puis reconnecte-toi avant de réessayer."

        "network" in message ||
            "timeout" in message -> "Problème réseau. Vérifie ta connexion Internet puis réessaie."

        "user" in message && "not" in message && "found" in message -> "Compte introuvable. Vérifie que tu es bien connecté."

        "email" in message && ("already" in message || "in use" in message) -> "Cette adresse e-mail est déjà utilisée."

        else -> rawMessage ?: fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfosCompteScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onEmailChangeRequiresLogout: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentEmail = authRepository.currentUser()?.email ?: "Aucun e-mail"

    var newEmail by remember { mutableStateOf("") }
    var currentPasswordForEmail by remember { mutableStateOf("") }
    var currentPasswordForDelete by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun showMessage(value: String) {
        message = value
        scope.launch {
            snackbarHostState.showSnackbar(value)
        }
    }

    fun sendPasswordReset() {
        scope.launch {
            isLoading = true
            message = null
            val result = authRepository.sendPasswordResetForCurrentUser()
            isLoading = false

            if (result.isSuccess) {
                showMessage("E-mail de réinitialisation envoyé à $currentEmail. Vérifie aussi les spams.")
            } else {
                showMessage(mapFirebaseErrorMessage(result.exceptionOrNull()?.localizedMessage))
            }
        }
    }

    fun changeEmail() {
        if (newEmail.isBlank() || currentPasswordForEmail.isBlank()) {
            showMessage("Renseigne le nouvel e-mail et ton mot de passe actuel.")
            return
        }

        scope.launch {
            isLoading = true
            message = null
            val result = authRepository.requestEmailChange(
                newEmail = newEmail.trim(),
                currentPassword = currentPasswordForEmail
            )
            isLoading = false

            if (result.isSuccess) {
                currentPasswordForEmail = ""
                showMessage("Un e-mail de confirmation a été envoyé à la nouvelle adresse. Reconnecte-toi après validation.")
                authRepository.logout()
                onEmailChangeRequiresLogout()
            } else {
                showMessage(mapFirebaseErrorMessage(result.exceptionOrNull()?.localizedMessage))
            }
        }
    }

    fun deleteAccount() {
        if (currentPasswordForDelete.isBlank()) {
            showMessage("Saisis ton mot de passe pour supprimer le compte.")
            return
        }

        scope.launch {
            isLoading = true
            message = null
            val result = authRepository.deleteCurrentAccount(currentPasswordForDelete)
            isLoading = false

            if (result.isSuccess) {
                onAccountDeleted()
            } else {
                showMessage(mapFirebaseErrorMessage(result.exceptionOrNull()?.localizedMessage))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Informations de compte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            EmailSummaryCard(currentEmail = currentEmail)

            SectionTitle(title = "Mot de passe")
            PasswordResetCard(
                isLoading = isLoading,
                onSendReset = ::sendPasswordReset
            )

            SectionTitle(title = "Adresse e-mail")
            ChangeEmailCard(
                newEmail = newEmail,
                currentPassword = currentPasswordForEmail,
                isLoading = isLoading,
                onNewEmailChange = { newEmail = it },
                onCurrentPasswordChange = { currentPasswordForEmail = it },
                onSubmit = ::changeEmail
            )

            SectionTitle(title = "Zone sensible")
            DeleteAccountCard(
                currentPassword = currentPasswordForDelete,
                isLoading = isLoading,
                onCurrentPasswordChange = { currentPasswordForDelete = it },
                onDelete = ::deleteAccount
            )

            if (!message.isNullOrBlank()) {
                InfoMessage(message = message!!)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmailSummaryCard(currentEmail: String) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "E-mail actuel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentEmail,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PasswordResetCard(
    isLoading: Boolean,
    onSendReset: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Envoie un e-mail pour réinitialiser ton mot de passe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !isLoading, onClick = onSendReset) {
                    Text("Réinitialiser le mot de passe")
                }
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ChangeEmailCard(
    newEmail: String,
    currentPassword: String,
    isLoading: Boolean,
    onNewEmailChange: (String) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Tu seras déconnecté après cette action.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = newEmail,
                onValueChange = onNewEmailChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nouvel e-mail") }
            )
            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Mot de passe actuel") },
                visualTransformation = PasswordVisualTransformation()
            )

            Button(enabled = !isLoading, onClick = onSubmit) {
                Text("Changer l'e-mail")
            }
        }
    }
}

@Composable
private fun DeleteAccountCard(
    currentPassword: String,
    isLoading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Cette action est irréversible. Tu seras déconnecté après la suppression.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            HorizontalDivider()
            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Confirmer avec le mot de passe") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                enabled = !isLoading,
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Supprimer mon compte")
            }
        }
    }
}

@Composable
private fun InfoMessage(message: String) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}