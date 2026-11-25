package com.f4f.arena.data.auth

import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Lightweight in-memory admin session manager storing the short-lived JWT returned by /api/v1/auth/login.
 */
object SessionManager {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _adminUsername = MutableStateFlow<String?>(null)
    val adminUsername: StateFlow<String?> = _adminUsername.asStateFlow()

    fun getBearerToken(): String {
        val current = _token.value ?: return ""
        return if (current.startsWith("Bearer ")) current else "Bearer $current"
    }

    fun isLoggedIn(): Boolean = !_token.value.isNullOrBlank()

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.authApi.login(LoginRequest(username.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                _token.value = response.body()!!.token
                _adminUsername.value = username.trim()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credentials (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        _token.value = null
        _adminUsername.value = null
    }
}
