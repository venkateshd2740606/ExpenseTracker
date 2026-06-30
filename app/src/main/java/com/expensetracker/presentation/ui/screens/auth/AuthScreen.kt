package com.expensetracker.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.expensetracker.R
import com.expensetracker.presentation.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(state.session) { if (state.session != null) onAuthenticated() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ExpenseTracker Pro", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Spacer(Modifier.height(32.dp))

        TabRow(selectedTabIndex = mode.ordinal) {
            AuthMode.entries.forEachIndexed { _, m ->
                Tab(selected = mode == m, onClick = { mode = m; otp = "" }, text = { Text(m.label) })
            }
        }
        Spacer(Modifier.height(16.dp))

        when (mode) {
            AuthMode.LOGIN, AuthMode.REGISTER -> {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }
                )
                if (mode == AuthMode.REGISTER) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                }
            }
            AuthMode.OTP_EMAIL -> {
                if (state.otpChallenge == null) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    Text(
                        stringResource(R.string.email_otp_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    state.otpDisplayCode?.let { code ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.email_otp_code_label), style = MaterialTheme.typography.labelMedium)
                                Text(code, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.email_otp_code_note), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Enter OTP") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (new users)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                when (mode) {
                    AuthMode.LOGIN -> viewModel.login(email, password)
                    AuthMode.REGISTER -> viewModel.register(email, password, name)
                    AuthMode.OTP_EMAIL -> if (state.otpChallenge == null) viewModel.sendEmailOtp(email) else viewModel.verifyOtp(otp, name.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text(mode.actionLabel(state.otpChallenge != null))
        }

        if (mode == AuthMode.LOGIN) {
            TextButton(onClick = { viewModel.resetPassword(email) }) { Text("Forgot Password?") }
        }
    }
}

private enum class AuthMode(val label: String) {
    LOGIN("Login"), REGISTER("Sign Up"), OTP_EMAIL("Email OTP");
    fun actionLabel(hasOtp: Boolean) = when (this) {
        LOGIN -> "Login"
        REGISTER -> "Create Account"
        OTP_EMAIL -> if (hasOtp) "Verify OTP" else "Send OTP"
    }
}
