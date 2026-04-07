package com.example.planpockeeper.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp


@Composable
fun ConnectedUserCard(
    email: String,
    onLogout: () -> Unit
) {
    Text("Connecté: $email")
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onLogout) {
        Text("Se déconnecter")
    }
}

@Composable
fun AuthModeSelector(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onModeChange(AuthMode.SIGN_UP) },
            enabled = selectedMode != AuthMode.SIGN_UP,
            modifier = Modifier.weight(1f)
        ) {
            Text("Inscription")
        }
        Button(
            onClick = { onModeChange(AuthMode.LOGIN) },
            enabled = selectedMode != AuthMode.LOGIN,
            modifier = Modifier.weight(1f)
        ) {
            Text("Connexion")
        }
    }
}

@Composable
fun SignUpFields(
    name: String,
    surname: String,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Prénom") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = surname,
        onValueChange = onSurnameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Nom") }
    )
}

@Composable
fun CredentialsFields(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Mot de passe") },
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
fun AuthAction(
    mode: AuthMode,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    Button(enabled = !isLoading, onClick = onSubmit) {
        Text(if (mode == AuthMode.SIGN_UP) "Créer un compte" else "Se connecter")
    }

    if (isLoading) {
        Spacer(modifier = Modifier.height(10.dp))
        CircularProgressIndicator()
    }
}

@Composable
fun StatusMessage(message: String?) {
    if (!message.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(message)
    }
}

@Composable
fun AuthForm(
    state: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResendVerificationEmail: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AuthModeSelector(selectedMode = state.mode, onModeChange = onModeChange)
        Spacer(modifier = Modifier.height(12.dp))

        if (state.mode == AuthMode.SIGN_UP) {
            SignUpFields(
                name = state.name,
                surname = state.surname,
                onNameChange = onNameChange,
                onSurnameChange = onSurnameChange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        CredentialsFields(
            email = state.email,
            password = state.password,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange
        )

        if (state.mode == AuthMode.SIGN_UP) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Confirmer le mot de passe") },
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AuthAction(mode = state.mode, isLoading = state.isLoading, onSubmit = onSubmit)

        val canResendVerification =
            state.mode == AuthMode.LOGIN ||
                (state.mode == AuthMode.SIGN_UP &&
                    state.statusMessage == "Inscription réussie. Vérifie ton email puis connecte-toi.")

        if (canResendVerification) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                enabled = !state.isLoading,
                onClick = onResendVerificationEmail
            ) {
                Text("Renvoyer l'e-mail de vérification")
            }
        }

        StatusMessage(state.statusMessage)
    }
}
