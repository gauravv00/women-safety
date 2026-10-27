package com.womensafety.sos.ui.screens.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.womensafety.sos.R
import com.womensafety.sos.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

enum class LoginStep {
    GOOGLE_SIGN_IN,
    PHONE_INPUT,
    OTP_INPUT,
    NAME_INPUT
}

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _step = MutableStateFlow(LoginStep.GOOGLE_SIGN_IN)
    val step: StateFlow<LoginStep> = _step.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _phoneNumber = MutableStateFlow("+91")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown: StateFlow<Int> = _resendCountdown.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var timerJob: Job? = null

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun setOtpCode(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _otpCode.value = otp
        }
    }

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun changePhoneNumber() {
        _otpCode.value = ""
        _errorMessage.value = null
        _step.value = LoginStep.PHONE_INPUT
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
                if (resId == 0) {
                    _errorMessage.value = "Google Sign-In is not configured in Firebase. Please use Phone / OTP login or Skip."
                    return@launch
                }
                val serverClientId = activity.getString(resId)
                val credentialManager = CredentialManager.create(activity)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                    val authResult = auth.signInWithCredential(authCredential).await()
                    val resolvedName = googleIdTokenCredential.displayName
                        ?: authResult.user?.displayName
                        ?: ""
                    _displayName.value = resolvedName
                    _step.value = LoginStep.PHONE_INPUT
                } else {
                    _errorMessage.value = "Unexpected credential type returned by Google Sign-In"
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the bottom sheet
                Log.d("LoginViewModel", "Google Sign-in cancelled by user")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Google Sign-in failed", e)
                _errorMessage.value = e.localizedMessage ?: "Google Sign-In failed. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendPhoneOtp(activity: Activity, isResend: Boolean = false) {
        val rawPhone = _phoneNumber.value.trim()
        val formattedPhone = when {
            rawPhone.startsWith("+") -> rawPhone.replace(" ", "").replace("-", "")
            rawPhone.length == 10 -> "+91$rawPhone"
            else -> "+91${rawPhone.replace(" ", "").replace("-", "")}"
        }

        if (formattedPhone.length < 10) {
            _errorMessage.value = "Please enter a valid phone number"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                linkPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _isLoading.value = false
                Log.e("LoginViewModel", "Phone verification failed", e)
                _errorMessage.value = e.localizedMessage ?: "Phone verification failed. Please check the number."
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _isLoading.value = false
                verificationId = vId
                resendToken = token
                _step.value = LoginStep.OTP_INPUT
                startResendTimer()
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (isResend && resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _resendCountdown.value = 30
        timerJob = viewModelScope.launch {
            while (_resendCountdown.value > 0) {
                delay(1000)
                _resendCountdown.value -= 1
            }
        }
    }

    fun verifyOtp() {
        val code = _otpCode.value.trim()
        if (code.length != 6) {
            _errorMessage.value = "Please enter the 6-digit OTP code"
            return
        }
        val vId = verificationId
        if (vId.isNullOrBlank()) {
            _errorMessage.value = "Verification ID missing. Please resend OTP."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val credential = PhoneAuthProvider.getCredential(vId, code)
        linkPhoneCredential(credential)
    }

    private fun linkPhoneCredential(credential: PhoneAuthCredential) {
        val user = auth.currentUser
        if (user == null) {
            _isLoading.value = false
            _errorMessage.value = "Session expired. Please sign in with Google again."
            _step.value = LoginStep.GOOGLE_SIGN_IN
            return
        }

        user.linkWithCredential(credential)
            .addOnSuccessListener {
                _isLoading.value = false
                _errorMessage.value = null
                _step.value = LoginStep.NAME_INPUT
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.e("LoginViewModel", "Error linking phone credential", e)
                _errorMessage.value = e.localizedMessage ?: "Invalid OTP or failed to link phone number."
            }
    }

    fun completeLogin(context: Context, name: String, onFinished: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            _errorMessage.value = "Name must be at least 2 characters"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // 1. Save display name to UserPreferencesRepository
                ServiceLocator.userPreferences.updateDisplayName(trimmed)
                // 2. Sync Firestore user document
                try {
                    ServiceLocator.authManager.syncUserDocument(trimmed)
                } catch (e: Exception) {
                    Log.w("LoginViewModel", "Failed to sync user doc during completeLogin: ${e.message}")
                }
                // 3. Mark login completed in the dedicated SharedPreferences file
                LoginPreferences.setLoginCompleted(context, true)
                _isLoading.value = false
                onFinished()
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("LoginViewModel", "Error completing login", e)
                _errorMessage.value = e.localizedMessage ?: "Failed to save profile. Please try again."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
