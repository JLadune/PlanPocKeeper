package com.example.planpockeeper.ui.auth

enum class AuthMode {
    SIGN_UP,
    LOGIN
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_UP,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val statusMessage: String? = null,
    val isLoading: Boolean = false,
    val connectedEmail: String? = null
)
