package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object FirebaseAuthService {
    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "FirebaseAuth instance unavailable: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): Boolean {
        val auth = getAuth() ?: return false
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result, auth)
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Sign in failed", e)
            false
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse, auth: FirebaseAuth): Boolean {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
            return try {
                auth.signInWithCredential(firebaseCredential).await()
                true
            } catch (e: Exception) {
                Log.e("FirebaseAuthService", "Firebase auth failed", e)
                false
            }
        }
        return false
    }

    fun getCurrentUser() = getAuth()?.currentUser

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "Sign out error: ${e.message}")
        }
    }
}

