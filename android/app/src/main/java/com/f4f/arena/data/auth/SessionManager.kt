package com.f4f.arena.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.f4f.arena.data.api.NetworkClient
import com.f4f.arena.data.model.LoginRequest
import com.f4f.arena.data.model.LoginResponse
import com.f4f.arena.data.model.RegisterRequest
import com.f4f.arena.data.model.ResetPinRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Session manager storing the JWT, role (ADMIN / PLAYER), and player profile ID.
 * Persists credentials in app-private SharedPreferences when initialized from Application.
 */
object SessionManager {
    private const val PREFS_NAME = "f4f_arena_session"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "admin_username"
    private const val KEY_ROLE = "user_role"
    private const val KEY_PLAYER_ID = "player_id"
    private const val KEY_DISPLAY_NAME = "display_name"

    private var prefs: SharedPreferences? = null

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _adminUsername = MutableStateFlow<String?>(null)
    val adminUsername: StateFlow<String?> = _adminUsername.asStateFlow()

    private val _role = MutableStateFlow<String?>(null)
    val role: StateFlow<String?> = _role.asStateFlow()

    private val _playerId = MutableStateFlow<String?>(null)
    val playerId: StateFlow<String?> = _playerId.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _authPromptRequired = MutableStateFlow(false)
    val authPromptRequired: StateFlow<Boolean> = _authPromptRequired.asStateFlow()

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _token.value = sp.getString(KEY_TOKEN, null)
        _adminUsername.value = sp.getString(KEY_USERNAME, null)
        _role.value = sp.getString(KEY_ROLE, null)
        _playerId.value = sp.getString(KEY_PLAYER_ID, null)
        _displayName.value = sp.getString(KEY_DISPLAY_NAME, null)
    }

    fun getBearerToken(): String {
        val current = _token.value ?: return ""
        return if (current.startsWith("Bearer ")) current else "Bearer $current"
    }

    fun isLoggedIn(): Boolean = !_token.value.isNullOrBlank()

    fun isAdmin(): Boolean = isLoggedIn() && _role.value == "ADMIN"

    private fun saveSession(username: String, body: LoginResponse) {
        val resolvedRole = body.role ?: "ADMIN"
        val resolvedName = body.displayName ?: username
        _token.value = body.token
        _adminUsername.value = username
        _role.value = resolvedRole
        _playerId.value = body.playerId
        _displayName.value = resolvedName
        _authPromptRequired.value = false
        prefs?.edit()
            ?.putString(KEY_TOKEN, body.token)
            ?.putString(KEY_USERNAME, username)
            ?.putString(KEY_ROLE, resolvedRole)
            ?.putString(KEY_PLAYER_ID, body.playerId)
            ?.putString(KEY_DISPLAY_NAME, resolvedName)
            ?.apply()
    }

    suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedUser = username.trim()
            val response = NetworkClient.authApi.login(LoginRequest(trimmedUser, password))
            if (response.isSuccessful && response.body() != null) {
                saveSession(trimmedUser, response.body()!!)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credentials (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        displayName: String,
        phone: String,
        age: Int,
        pin: String,
        firebaseIdToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val phoneDigits = phone.filter { it.isDigit() }
            val derivedUser = if (phoneDigits.length >= 10) phoneDigits.takeLast(10) else displayName.trim().lowercase().replace(" ", "_")
            val response = NetworkClient.authApi.register(
                RegisterRequest(
                    displayName = displayName.trim(),
                    username = derivedUser,
                    phone = phone.trim(),
                    age = age,
                    password = pin,
                    firebaseIdToken = firebaseIdToken
                )
            )
            if (response.isSuccessful && response.body() != null) {
                saveSession(derivedUser, response.body()!!)
                Result.success(Unit)
            } else if (response.code() == 409) {
                Result.failure(Exception("Phone number is already registered. Try signing in instead."))
            } else {
                Result.failure(Exception("Sign up failed (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPin(
        username: String,
        phone: String,
        newPin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.authApi.resetPin(
                ResetPinRequest(
                    username = username.trim(),
                    phone = phone.trim(),
                    newPassword = newPin
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "PIN updated successfully")
            } else {
                Result.failure(Exception("Reset failed (HTTP ${response.code()}): check username & phone"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.authApi.changePassword(
                bearerToken = getBearerToken(),
                request = com.f4f.arena.data.model.ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Password updated successfully")
            } else if (response.code() == 401) {
                Result.failure(Exception("Current password is incorrect"))
            } else {
                Result.failure(Exception("Update failed (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun requestLoginPrompt() {
        _authPromptRequired.value = true
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
        _role.value = null
        _playerId.value = null
        _displayName.value = null
        prefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_USERNAME)
            ?.remove(KEY_ROLE)
            ?.remove(KEY_PLAYER_ID)
            ?.remove(KEY_DISPLAY_NAME)
            ?.apply()
    }
}
