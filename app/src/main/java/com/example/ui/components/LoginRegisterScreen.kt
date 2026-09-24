package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentOsGlassTokens
import com.example.ui.theme.StudentOsShapes
import com.example.ui.theme.StudentOsSpacing
import com.example.ui.theme.cardBorderStroke

@Composable
fun LoginRegisterScreen(
    onSignInBackend: (String, String, (Boolean, String) -> Unit) -> Unit,
    onSignUpBackend: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onSignInFirebase: (String, String, (Boolean, String) -> Unit) -> Unit,
    onSignUpFirebase: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onForgotPassword: (String) -> Unit,
    onGoogleSignIn: ((Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var selectedProvider by remember { mutableIntStateOf(0) } // 0: Custom Node.js Backend, 1: Firebase Auth
    
    // Form Inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Visibility/State
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(StudentOsSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.lg)
        ) {
            // App Identity Logo Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            brush = StudentOsGlassTokens.heroPassport
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = "دانشجو OS",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(StudentOsSpacing.xs))
                Text(
                    text = "سیستم‌عامل تحصیلی دانشجویان",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "همیار هوشمند، منسجم و همیشگی دوران تحصیلی شما",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = StudentOsSpacing.sm)
                )
            }
            
            // Card Content Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        border = MaterialTheme.cardBorderStroke,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(StudentOsSpacing.lg)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Custom Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "ورود به حساب",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "ثبت‌نام جدید",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(StudentOsSpacing.xs))

                    // Backend Provider Selector (Node.js/PostgreSQL vs Firebase)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedProvider == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedProvider = 0
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedProvider == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "سرور اختصاصی",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedProvider == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedProvider == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedProvider = 1
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedProvider == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ابر فایربیس",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedProvider == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = if (selectedProvider == 0)
                            "⚡ سرور مستقل Node.js & PostgreSQL (همگام‌سازی آفلاین خودکار)"
                        else
                            "☁️ احراز هویت با شبکه ابری Google Firebase",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(StudentOsSpacing.xs))
                    
                    // Live Error Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(StudentOsSpacing.sm)
                            ) {
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // inputs based on selected tab
                    if (selectedTab == 1) {
                        // Register Fields
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام و نام خانوادگی") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )
                    }
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("آدرس ایمیل") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AlternateEmail,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isPasswordVisible = !isPasswordVisible
                            }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "پنهان‌سازی" else "نمایش"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )
                    
                    if (selectedTab == 1) {
                        // Confirm Password for registration
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("تکرار رمز عبور") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                                }) {
                                    Icon(
                                        imageVector = if (isConfirmPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = if (isConfirmPasswordVisible) "پنهان‌سازی" else "نمایش"
                                    )
                                }
                            },
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )
                    }
                    
                    if (selectedTab == 0) {
                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "فراموشی رمز عبور؟",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable {
                                        if (email.isBlank() || !email.contains("@")) {
                                            errorMessage = "لطفاً ابتدا آدرس ایمیل خود را در کادر مربوطه وارد نمایید."
                                        } else {
                                            onForgotPassword(email)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("forgot_password_button")
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(StudentOsSpacing.xs))
                    
                    // Main Action Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isLoading) return@Button
                            
                            // Client Validation
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "آدرس ایمیل وارد شده معتبر نمی‌باشد."
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "رمز عبور باید حداقل شامل ۶ کاراکتر باشد."
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = null
                            
                            if (selectedProvider == 0) {
                                // Custom Node.js & PostgreSQL Backend
                                if (selectedTab == 0) {
                                    onSignInBackend(email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        } else {
                                            Toast.makeText(context, "ورود به سرور اختصاصی با موفقیت انجام شد 🌱", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    if (name.isBlank()) {
                                        isLoading = false
                                        errorMessage = "لطفاً نام و نام خانوادگی خود را وارد نمایید."
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        isLoading = false
                                        errorMessage = "رمز عبور و تکرار آن با یکدیگر مطابقت ندارند."
                                        return@Button
                                    }

                                    onSignUpBackend(name, email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        } else {
                                            Toast.makeText(context, "حساب کاربری در سرور اختصاصی ایجاد شد ✨", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                // Cloud Firebase Auth
                                if (selectedTab == 0) {
                                    onSignInFirebase(email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        } else {
                                            Toast.makeText(context, "با موفقیت وارد شدید 🌱", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    if (name.isBlank()) {
                                        isLoading = false
                                        errorMessage = "لطفاً نام و نام خانوادگی خود را وارد نمایید."
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        isLoading = false
                                        errorMessage = "رمز عبور و تکرار آن با یکدیگر مطابقت ندارند."
                                        return@Button
                                    }

                                    onSignUpFirebase(name, email, password) { success, msg ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = msg
                                        } else {
                                            Toast.makeText(context, "حساب کاربری با موفقیت ساخته شد ✨", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(StudentOsShapes.button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = when {
                                    selectedTab == 0 && selectedProvider == 0 -> "ورود به سرور اختصاصی دانشجو OS"
                                    selectedTab == 0 && selectedProvider == 1 -> "ورود با ایمیل ابری فایربیس"
                                    selectedTab == 1 && selectedProvider == 0 -> "ثبت‌نام در سرور اختصاصی Node.js"
                                    else -> "ایجاد حساب در ابر فایربیس"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                    
                    // Google Sign In Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = StudentOsSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Text(
                            text = "یا",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = StudentOsSpacing.sm)
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                    }
                    
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            if (isLoading) return@OutlinedButton
                            isLoading = true
                            errorMessage = null
                            onGoogleSignIn { success, msg ->
                                isLoading = false
                                if (!success) {
                                    errorMessage = msg
                                } else {
                                    Toast.makeText(context, "ورود با حساب گوگل با موفقیت انجام شد ✨", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = MaterialTheme.cardBorderStroke,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_auth_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google Branding Dots
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4285F4))) // Blue
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34A853))) // Green
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFBBC05))) // Yellow
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEA4335))) // Red
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ورود با حساب کاربری گوگل",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
