package com.f4f.arena.data.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

object FirebasePhoneAuthHelper {

    fun isConfigured(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeE164(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.filter { it.isDigit() }
        }
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.length == 10) "+91$digits" else "+$digits"
    }

    fun sendOtp(
        activity: Activity,
        rawPhone: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerifiedIdToken: (idToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured(activity)) {
            onError("Firebase Phone Auth is not configured yet (missing google-services.json).")
            return
        }
        val auth = FirebaseAuth.getInstance()
        val phoneE164 = normalizeE164(rawPhone)
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(auth, credential, onAutoVerifiedIdToken, onError)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.localizedMessage ?: "Failed to send OTP")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        context: Context,
        verificationId: String,
        code: String,
        onVerifiedIdToken: (idToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured(context)) {
            onError("Firebase Phone Auth is not configured yet.")
            return
        }
        val auth = FirebaseAuth.getInstance()
        val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
        signInWithCredential(auth, credential, onVerifiedIdToken, onError)
    }

    private fun signInWithCredential(
        auth: FirebaseAuth,
        credential: PhoneAuthCredential,
        onVerifiedIdToken: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onError("Phone verification failed: no user")
                    return@addOnSuccessListener
                }
                user.getIdToken(false)
                    .addOnSuccessListener { tokenResult ->
                        val idToken = tokenResult.token.orEmpty()
                        if (idToken.isBlank()) {
                            onError("Could not obtain verification token")
                        } else {
                            onVerifiedIdToken(idToken)
                        }
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Token retrieval failed")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Invalid OTP code")
            }
    }
}
