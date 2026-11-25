package com.f4f.arena.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Admin session manager storing the JWT returned by /api/v1/auth/login.
 * Persists credentials in app-private SharedPreferences when initialized from Application.
 */
object SessionManager {
    private const val PREFS_NAME = "f4f_arena_session"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "admin_username"

    private var prefs: SharedPreferences? = null

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _adminUsername = MutableStateFlow<String?>(null)
    val adminUsername: StateFlow<String?> = _adminUsername.asStateFlow()

    private val _authPromptRequired = MutableStateFlow(false)
    val authPromptRequired: StateFlow<Boolean> = _authPromptRequired.asStateFlow()

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _token.value = sp.getString(KEY_TOKEN, null)
        _adminUsername.value = sp.getString(KEY_USERNAME, null)
    }

    fun getBearerToken(): String {
        val current = _token.value ?: return ""
        return if (current.startsWith("Bearer ")) current else "Bearer $current"
    }

    fun isLoggedIn(): Boolean = !_token.value.isNullOrBlank()

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedUser = username.trim()
            val response = NetworkClient.authApi.login(LoginRequest(trimmedUser, password))
            if (response.isSuccessful && response.body() != null) {
                val jwt = response.body()!!.token
                _token.value = jwt
                _adminUsername.value = trimmedUser
                _authPromptRequired.value = false
                prefs?.edit()
                    ?.putString(KEY_TOKEN, jwt)
                    ?.putString(KEY_USERNAME, trimmedUser)
                    ?.apply()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credentials (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun onUnauthorized() {
        logout()
        _authPromptRequired.value = true
    }

    fun consumeAuthPrompt() {
        _authPromptRequired.value = false
    }

    fun logout() {
        _token.value = null
        _adminUsername.value = null
        prefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_USERNAME)
            ?.apply()
    }
}
