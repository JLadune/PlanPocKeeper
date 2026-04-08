package com.example.planpockeeper.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val colorScheme = MaterialTheme.colorScheme

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onModeChange(AuthMode.SIGN_UP) },
            enabled = selectedMode != AuthMode.SIGN_UP,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            Text("Inscription")
        }
        Button(
            onClick = { onModeChange(AuthMode.LOGIN) },
            enabled = selectedMode != AuthMode.LOGIN,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
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
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Prénom") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.primary,
            cursorColor = colorScheme.primary,
            focusedTextColor = colorScheme.primary,
            unfocusedTextColor = colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = surname,
        onValueChange = onSurnameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Nom") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.primary,
            cursorColor = colorScheme.primary,
            focusedTextColor = colorScheme.primary,
            unfocusedTextColor = colorScheme.primary
        )
    )
}

@Composable
fun CredentialsFields(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.primary,
            cursorColor = colorScheme.primary,
            focusedTextColor = colorScheme.primary,
            unfocusedTextColor = colorScheme.primary
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Mot de passe") },
        visualTransformation = PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.primary,
            cursorColor = colorScheme.primary,
            focusedTextColor = colorScheme.primary,
            unfocusedTextColor = colorScheme.primary
        )
    )
}

@Composable
fun AuthAction(
    mode: AuthMode,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Button(
        enabled = !isLoading,
        onClick = onSubmit,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary
        )
    ) {
        Text(if (mode == AuthMode.SIGN_UP) "Créer un compte" else "Se connecter")
    }

    if (isLoading) {
        Spacer(modifier = Modifier.height(10.dp))
        CircularProgressIndicator(color = colorScheme.primary)
    }
}

@Composable
fun StatusMessage(message: String?) {
    if (!message.isNullOrBlank()) {
        val colorScheme = MaterialTheme.colorScheme
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = colorScheme.primary)
    }
}

@Composable
private fun GoogleBrandedButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val siwgResId = remember(context) {
        context.resources.getIdentifier("siwg_button", "drawable", context.packageName)
    }

    if (siwgResId != 0) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = siwgResId),
                contentDescription = text,
                contentScale = ContentScale.Fit,
                colorFilter = if (enabled) null else ColorFilter.tint(androidx.compose.ui.graphics.Color(0x66000000)),
                modifier = Modifier
                    .height(52.dp)
                    .widthIn(max = 460.dp)
                    .clickable(enabled = enabled, onClick = onClick)
            )
        }
    } else {
        OutlinedButton(
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text)
        }
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
    onResendVerificationEmail: () -> Unit,
    onGoogleSignIn: () -> Unit
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
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AuthAction(mode = state.mode, isLoading = state.isLoading, onSubmit = onSubmit)

        Spacer(modifier = Modifier.height(8.dp))
        GoogleBrandedButton(
            text = if (state.mode == AuthMode.SIGN_UP) "Sign in with Google" else "Sign in with Google",
            enabled = !state.isLoading,
            onClick = onGoogleSignIn
        )

        val canResendVerification =
            state.mode == AuthMode.LOGIN ||
                (state.mode == AuthMode.SIGN_UP &&
                    state.statusMessage == "Inscription réussie. Vérifie ton email puis connecte-toi.")

        if (canResendVerification) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                enabled = !state.isLoading,
                onClick = onResendVerificationEmail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Renvoyer l'e-mail de vérification")
            }
        }

        StatusMessage(state.statusMessage)
    }
}
