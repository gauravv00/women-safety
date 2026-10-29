package com.womensafety.sos.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.womensafety.sos.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.womensafety.sos.ui.components.JhumkaWatermarkBackground
import com.womensafety.sos.ui.theme.JhumkaBlushBackground
import com.womensafety.sos.ui.theme.JhumkaBorder
import com.womensafety.sos.ui.theme.JhumkaCrimsonRed
import com.womensafety.sos.ui.theme.JhumkaPinkContainer
import com.womensafety.sos.ui.theme.JhumkaPinkPrimary
import com.womensafety.sos.ui.theme.JhumkaSafetyGreen
import com.womensafety.sos.ui.theme.JhumkaSurface
import com.womensafety.sos.ui.theme.JhumkaSurfaceVariant
import com.womensafety.sos.ui.theme.JhumkaTextPrimary
import com.womensafety.sos.ui.theme.JhumkaTextSecondary
import com.womensafety.sos.ui.theme.JhumkaTextTertiary
import androidx.compose.material.icons.filled.Spa
import com.womensafety.sos.ui.theme.AlertRed
import com.womensafety.sos.ui.theme.DarkBackground
import com.womensafety.sos.ui.theme.DarkCardBorder
import com.womensafety.sos.ui.theme.DarkSurface
import com.womensafety.sos.ui.theme.DarkSurfaceVariant
import com.womensafety.sos.ui.theme.SafetyGreen
import com.womensafety.sos.ui.theme.TrustBlue

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val step by viewModel.step.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val resendCountdown by viewModel.resendCountdown.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isRegistering by viewModel.isRegistering.collectAsState()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsState()

    Scaffold(
        containerColor = JhumkaBlushBackground
    ) { paddingValues ->
        JhumkaWatermarkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header / Branding
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(JhumkaSurface)
                        .border(1.5.dp, JhumkaBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Jhumka Logo",
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "JHUMKA",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaPinkPrimary,
                    letterSpacing = 2.0.sp
                )

                Text(
                    text = "Your Silent Guardian • Emergency & Safety Network",
                    style = MaterialTheme.typography.bodySmall,
                    color = JhumkaTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Progress Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepChip(number = "1", label = "Sign In", active = step == LoginStep.GOOGLE_SIGN_IN || step == LoginStep.EMAIL_INPUT, completed = step != LoginStep.GOOGLE_SIGN_IN && step != LoginStep.EMAIL_INPUT)
                    StepDivider(completed = step != LoginStep.GOOGLE_SIGN_IN && step != LoginStep.EMAIL_INPUT)
                    StepChip(number = "2", label = "Phone OTP", active = step == LoginStep.PHONE_INPUT || step == LoginStep.OTP_INPUT, completed = step == LoginStep.NAME_INPUT)
                    StepDivider(completed = step == LoginStep.NAME_INPUT)
                    StepChip(number = "3", label = "Profile", active = step == LoginStep.NAME_INPUT, completed = false)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Error Banner
                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = AlertRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Step Content
                when (step) {
                    LoginStep.GOOGLE_SIGN_IN -> {
                        GoogleSignInStep(
                            isLoading = isLoading,
                            onSignInClick = {
                                activity?.let { viewModel.signInWithGoogle(it) }
                            },
                            onEmailSignInClick = {
                                viewModel.goToEmailInput()
                            },
                            onPhoneSignInClick = {
                                viewModel.goToPhoneInput()
                            },
                            onSkipClick = {
                                viewModel.skipLogin(context, onLoginSuccess)
                            }
                        )
                    }
                    LoginStep.EMAIL_INPUT -> {
                        EmailInputStep(
                            email = email,
                            password = password,
                            isRegistering = isRegistering,
                            isPasswordVisible = isPasswordVisible,
                            isLoading = isLoading,
                            onEmailChange = { viewModel.setEmail(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onToggleRegister = { viewModel.setIsRegistering(it) },
                            onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
                            onSignInClick = { viewModel.signInWithEmail() },
                            onGoogleSignInClick = {
                                activity?.let { viewModel.signInWithGoogle(it) }
                            },
                            onBackClick = { viewModel.goToGoogleSignIn() }
                        )
                    }
                    LoginStep.PHONE_INPUT -> {
                        PhoneInputStep(
                            phoneNumber = phoneNumber,
                            isLoading = isLoading,
                            onPhoneNumberChange = { viewModel.setPhoneNumber(it) },
                            onSendOtpClick = {
                                activity?.let { viewModel.sendPhoneOtp(it, isResend = false) }
                            },
                            onSkipClick = {
                                viewModel.skipPhoneVerification()
                            },
                            onBackClick = {
                                viewModel.goToGoogleSignIn()
                            }
                        )
                    }
                    LoginStep.OTP_INPUT -> {
                        OtpInputStep(
                            phoneNumber = phoneNumber,
                            otpCode = otpCode,
                            isLoading = isLoading,
                            resendCountdown = resendCountdown,
                            onOtpChange = { viewModel.setOtpCode(it) },
                            onVerifyClick = { viewModel.verifyOtp() },
                            onResendClick = {
                                activity?.let { viewModel.sendPhoneOtp(it, isResend = true) }
                            },
                            onChangePhoneClick = { viewModel.changePhoneNumber() }
                        )
                    }
                    LoginStep.NAME_INPUT -> {
                        NameInputStep(
                            displayName = displayName,
                            isLoading = isLoading,
                            onNameChange = { viewModel.setDisplayName(it) },
                            onCompleteClick = {
                                viewModel.completeLogin(context, displayName, onLoginSuccess)
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun StepChip(
    number: String,
    label: String,
    active: Boolean,
    completed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        completed -> JhumkaSafetyGreen
                        active -> JhumkaPinkPrimary
                        else -> JhumkaSurfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = number,
                    color = if (active) Color.White else JhumkaTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            color = if (active || completed) JhumkaTextPrimary else JhumkaTextTertiary,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StepDivider(completed: Boolean) {
    Spacer(modifier = Modifier.width(8.dp))
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(if (completed) JhumkaSafetyGreen else JhumkaBorder)
    )
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun GoogleSignInStep(
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    onEmailSignInClick: () -> Unit,
    onPhoneSignInClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = JhumkaPinkPrimary,
                modifier = Modifier.size(36.dp)
            )

            Text(
                text = "Welcome to Jhumka Safety",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JhumkaTextPrimary
            )

            Text(
                text = "Sign in to securely register emergency broadcasting, live GPS synchronization, and trusted guardian links.",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Google Sign-In Button
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = JhumkaPinkPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Gmail",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            // 2. Email Sign-In Button
            Button(
                onClick = onEmailSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JhumkaPinkContainer,
                    contentColor = JhumkaPinkPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaPinkPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = JhumkaPinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Email",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = JhumkaPinkPrimary
                    )
                }
            }

            // Divider with "OR"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.DarkGray.copy(alpha = 0.6f))
                )
                Text(
                    text = "OR",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.DarkGray.copy(alpha = 0.6f))
                )
            }

            // 3. Direct Continue with Phone & OTP Button
            Button(
                onClick = onPhoneSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JhumkaSurfaceVariant,
                    contentColor = JhumkaPinkPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaPinkPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = JhumkaPinkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Login through Phone & OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = JhumkaPinkPrimary
                    )
                }
            }

            // 4. Skip for Now Button (Guest / Anonymous Mode)
            TextButton(
                onClick = onSkipClick,
                enabled = !isLoading
            ) {
                Text(
                    text = "Skip for Now (Anonymous Mode)",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmailInputStep(
    email: String,
    password: String,
    isRegistering: Boolean,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleRegister: (Boolean) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignInClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = JhumkaPinkPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isRegistering) "Create Account with Email" else "Sign In with Email",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )
            }

            Text(
                text = if (isRegistering)
                    "Enter your email and create a password (at least 6 characters) to register."
                else
                    "Enter your email and password to log in. If you are new, you can switch to Register.",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary,
                lineHeight = 18.sp
            )

            // Sign In / Register Tab Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JhumkaSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { onToggleRegister(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isRegistering) JhumkaPinkPrimary else Color.Transparent,
                        contentColor = if (!isRegistering) Color.White else JhumkaTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { onToggleRegister(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistering) JhumkaPinkPrimary else Color.Transparent,
                        contentColor = if (isRegistering) Color.White else JhumkaTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Register", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                placeholder = { Text("name@example.com") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = JhumkaPinkPrimary)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JhumkaPinkPrimary,
                    unfocusedBorderColor = JhumkaBorder,
                    focusedTextColor = JhumkaTextPrimary,
                    unfocusedTextColor = JhumkaTextPrimary,
                    focusedLabelColor = JhumkaPinkPrimary,
                    unfocusedLabelColor = JhumkaTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = JhumkaPinkPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = JhumkaTextSecondary
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!isLoading && email.isNotBlank() && password.length >= 6) {
                            onSignInClick()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JhumkaPinkPrimary,
                    unfocusedBorderColor = JhumkaBorder,
                    focusedTextColor = JhumkaTextPrimary,
                    unfocusedTextColor = JhumkaTextPrimary,
                    focusedLabelColor = JhumkaPinkPrimary,
                    unfocusedLabelColor = JhumkaTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSignInClick,
                enabled = !isLoading && email.trim().contains("@") && password.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isRegistering) "Create Account & Continue" else "Sign In & Continue",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Divider with "OR"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                )
                Text(
                    text = "OR",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                )
            }

            // Fast Sign in with Gmail / Google Button
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = JhumkaPinkPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Gmail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Back to Sign-in options",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PhoneInputStep(
    phoneNumber: String,
    isLoading: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    onSendOtpClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = SafetyGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Verify Phone Number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )
            }

            Text(
                text = "We'll send an SMS verification code to verify your phone number. Default country code is +91.",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                label = { Text("Phone Number") },
                placeholder = { Text("+91 9876543210") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!isLoading) onSendOtpClick()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JhumkaPinkPrimary,
                    unfocusedBorderColor = JhumkaBorder,
                    focusedTextColor = JhumkaTextPrimary,
                    unfocusedTextColor = JhumkaTextPrimary,
                    focusedLabelColor = JhumkaPinkPrimary,
                    unfocusedLabelColor = JhumkaTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSendOtpClick,
                enabled = !isLoading && phoneNumber.trim().length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Verification OTP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Skip button so user is never blocked by SMS delivery
            OutlinedButton(
                onClick = onSkipClick,
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JhumkaPinkPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaPinkPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Skip Phone Verification & Continue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // Explanatory Note regarding Firebase Spark plan test numbers
            Card(
                colors = CardDefaults.cardColors(containerColor = JhumkaSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = JhumkaPinkPrimary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tip: Sending real SMS requires a Firebase Blaze plan. For testing without billing, add your number in Firebase Console > Authentication > Phone > 'Phone numbers for testing' with OTP 123456.",
                        style = MaterialTheme.typography.labelSmall,
                        color = JhumkaTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            TextButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Back to Sign-in options",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun OtpInputStep(
    phoneNumber: String,
    otpCode: String,
    isLoading: Boolean,
    resendCountdown: Int,
    onOtpChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onChangePhoneClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = JhumkaPinkPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Enter 6-Digit OTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )
            }

            Text(
                text = "Enter the 6-digit verification code sent to $phoneNumber",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary
            )

            OutlinedTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                label = { Text("6-Digit OTP") },
                placeholder = { Text("123456") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!isLoading && otpCode.length == 6) onVerifyClick()
                    }
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    color = JhumkaTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JhumkaPinkPrimary,
                    unfocusedBorderColor = JhumkaBorder,
                    focusedTextColor = JhumkaTextPrimary,
                    unfocusedTextColor = JhumkaTextPrimary,
                    focusedLabelColor = JhumkaPinkPrimary,
                    unfocusedLabelColor = JhumkaTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onVerifyClick,
                enabled = !isLoading && otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Verify OTP & Continue",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onChangePhoneClick,
                    enabled = !isLoading
                ) {
                    Text(text = "Change Number", color = Color.Gray, fontSize = 13.sp)
                }

                if (resendCountdown > 0) {
                    Text(
                        text = "Resend OTP in ${resendCountdown}s",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                } else {
                    TextButton(
                        onClick = onResendClick,
                        enabled = !isLoading
                    ) {
                        Text(text = "Resend OTP", color = JhumkaPinkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NameInputStep(
    displayName: String,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onCompleteClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isValid = displayName.trim().length >= 2

    Card(
        colors = CardDefaults.cardColors(containerColor = JhumkaSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, JhumkaBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = JhumkaPinkPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Confirm Your Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JhumkaTextPrimary
                )
            }

            Text(
                text = "This name will be shown to your paired guardians and emergency contacts when an SOS alert is broadcast.",
                style = MaterialTheme.typography.bodySmall,
                color = JhumkaTextSecondary,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = onNameChange,
                label = { Text("Display Name *") },
                placeholder = { Text("e.g. Sarah Connor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (!isLoading && isValid) onCompleteClick()
                    }
                ),
                supportingText = {
                    if (displayName.isNotBlank() && !isValid) {
                        Text(text = "Minimum 2 characters required", color = AlertRed)
                    }
                },
                isError = displayName.isNotBlank() && !isValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JhumkaPinkPrimary,
                    unfocusedBorderColor = JhumkaBorder,
                    focusedTextColor = JhumkaTextPrimary,
                    unfocusedTextColor = JhumkaTextPrimary,
                    focusedLabelColor = JhumkaPinkPrimary,
                    unfocusedLabelColor = JhumkaTextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onCompleteClick,
                enabled = !isLoading && isValid,
                colors = ButtonDefaults.buttonColors(containerColor = JhumkaPinkPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
