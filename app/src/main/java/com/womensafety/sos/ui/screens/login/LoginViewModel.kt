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
import com.womensafety.sos.data.util.PhoneUtils
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
    EMAIL_INPUT,
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

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown: StateFlow<Int> = _resendCountdown.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var timerJob: Job? = null

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = PhoneUtils.extract10DigitNumber(phone)
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

    fun setEmail(email: String) {
        _email.value = email
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    fun setIsRegistering(isRegister: Boolean) {
        _isRegistering.value = isRegister
        _errorMessage.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun goToEmailInput() {
        _errorMessage.value = null
        _step.value = LoginStep.EMAIL_INPUT
    }

    fun goToPhoneInput() {
        _errorMessage.value = null
        _step.value = LoginStep.PHONE_INPUT
    }

    fun goToGoogleSignIn() {
        _errorMessage.value = null
        _step.value = LoginStep.GOOGLE_SIGN_IN
    }

    fun signInWithEmail() {
        val email = _email.value.trim()
        val password = _password.value.trim()

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (_isRegistering.value) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    _isLoading.value = false
                    val defaultName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    _displayName.value = defaultName
                    _step.value = LoginStep.NAME_INPUT
                } else {
                    try {
                        val result = auth.signInWithEmailAndPassword(email, password).await()
                        _isLoading.value = false
                        val resolvedName = result.user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                        _displayName.value = resolvedName
                        _step.value = LoginStep.NAME_INPUT
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: ""
                        if (msg.contains("user-not-found", ignoreCase = true) || msg.contains("no user record", ignoreCase = true)) {
                            val regResult = auth.createUserWithEmailAndPassword(email, password).await()
                            _isLoading.value = false
                            val defaultName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                            _displayName.value = defaultName
                            _step.value = LoginStep.NAME_INPUT
                        } else {
                            throw e
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("LoginViewModel", "Email sign-in failed", e)
                val msg = e.localizedMessage ?: "Email authentication failed."
                _errorMessage.value = when {
                    msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                    msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                    msg.contains("operation is not allowed", ignoreCase = true) ||
                    msg.contains("provider is disabled", ignoreCase = true) ->
                        "Email/Password is disabled in Firebase Console. Go to Firebase Console > Authentication > Sign-in method > Email/Password: Turn ON 'Email/Password' (leave 'Email link' OFF) and click Save."
                    msg.contains("wrong-password", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true) ->
                        "Incorrect password or invalid credentials. Please check your password or switch to 'Register'."
                    msg.contains("email-already-in-use", ignoreCase = true) ->
                        "An account with this email already exists. Switch to 'Sign In' or check your password."
                    msg.contains("weak-password", ignoreCase = true) ->
                        "Password is too weak. Please use at least 6 characters."
                    else -> msg
                }
            }
        }
    }

    fun changePhoneNumber() {
        _otpCode.value = ""
        _errorMessage.value = null
        _step.value = LoginStep.PHONE_INPUT
    }

    fun skipPhoneVerification() {
        _errorMessage.value = null
        _step.value = LoginStep.NAME_INPUT
    }

    fun skipLogin(context: Context, onFinished: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ServiceLocator.authManager.ensureAuthenticated()
            } catch (e: Exception) {
                Log.w("LoginViewModel", "Anonymous auth on skip: ${e.message}")
            }
            LoginPreferences.setLoginSkipped(context)
            _isLoading.value = false
            onFinished()
        }
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
                Log.d("LoginViewModel", "Google Sign-in cancelled by user")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Google Sign-in failed", e)
                val msg = e.localizedMessage ?: ""
                _errorMessage.value = when {
                    msg.contains("No credentials", ignoreCase = true) ->
                        "No Google account found on device, or debug SHA-1 not yet saved in Firebase. Please add a Google account in phone Settings or add SHA-1 to Firebase."
                    else -> msg.ifBlank { "Google Sign-In failed. Please try again or use Phone / OTP." }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun formatPhoneNumber(input: String): String {
        val digits = PhoneUtils.extract10DigitNumber(input)
        return if (digits.length == 10) "+91$digits" else "+91${input.filter { it.isDigit() }}"
    }

    fun sendPhoneOtp(activity: Activity, isResend: Boolean = false) {
        val rawPhone = _phoneNumber.value.trim()
        val digits = PhoneUtils.extract10DigitNumber(rawPhone)
        if (digits.length != 10) {
            _errorMessage.value = "Please enter an exact 10-digit mobile number"
            return
        }
        val formattedPhone = "+91$digits"

        _isLoading.value = true
        _errorMessage.value = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                linkOrSignInPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _isLoading.value = false
                Log.e("LoginViewModel", "Phone verification failed", e)
                val msg = e.localizedMessage ?: "Phone verification failed."
                _errorMessage.value = when {
                    msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) || msg.contains("billing", ignoreCase = true) ->
                        "Firebase SMS sending requires a Blaze plan or Test Numbers. Please add this number under Firebase Console > Authentication > Phone > 'Phone numbers for testing' (with test OTP 123456) or tap 'Skip Phone Verification' below."
                    msg.contains("TOO_MANY_REQUESTS", ignoreCase = true) || msg.contains("quota", ignoreCase = true) ->
                        "SMS quota exceeded or too many requests. Please try again later or use Google Sign-In."
                    msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                        "Phone Auth is disabled in Firebase Console. Enable Phone provider under Authentication > Sign-in method."
                    msg.contains("API_KEY_INVALID", ignoreCase = true) ->
                        "Invalid Firebase API Key. Please replace app/google-services.json with your actual Firebase project file."
                    msg.contains("App verification failed", ignoreCase = true) ->
                        "App verification failed. Please add your debug SHA-1 fingerprint to Firebase Console > Project Settings."
                    else -> msg
                }
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
        linkOrSignInPhoneCredential(credential)
    }

    private fun linkOrSignInPhoneCredential(credential: PhoneAuthCredential) {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            // Already signed in via Google, link phone number
            user.linkWithCredential(credential)
                .addOnSuccessListener {
                    _isLoading.value = false
                    _errorMessage.value = null
                    _step.value = LoginStep.NAME_INPUT
                }
                .addOnFailureListener { e ->
                    // If credential already tied to an existing account, sign into it directly
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            _isLoading.value = false
                            _errorMessage.value = null
                            _step.value = LoginStep.NAME_INPUT
                        }
                        .addOnFailureListener { e2 ->
                            _isLoading.value = false
                            Log.e("LoginViewModel", "Error with phone credential", e2)
                            _errorMessage.value = e2.localizedMessage ?: "Invalid OTP or failed to verify phone number."
                        }
                }
        } else {
            // Direct standalone Phone & OTP Login
            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    _isLoading.value = false
                    _errorMessage.value = null
                    _step.value = LoginStep.NAME_INPUT
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    Log.e("LoginViewModel", "Error signing in with phone credential", e)
                    _errorMessage.value = e.localizedMessage ?: "Invalid OTP or failed to verify phone number."
                }
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
                // 3. Mark login completed
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
