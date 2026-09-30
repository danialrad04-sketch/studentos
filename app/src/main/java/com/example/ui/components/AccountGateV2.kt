package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountGateV2(
    onSignIn: (String, String, (Boolean, String) -> Unit) -> Unit,
    onSignUp: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onForgotPassword: (String) -> Unit,
    onGoogleSignIn: ((Boolean, String) -> Unit) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun resultHandler(ok: Boolean, text: String) {
        loading = false
        message = text
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Icon(
                        Icons.Rounded.School,
                        contentDescription = "Student OS",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(13.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Student OS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "سیستم‌عامل تحصیلی شخصی تو",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "حساب و همگام‌سازی",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "برای همگام‌سازی بین دستگاه‌ها وارد شو؛ برای شروع فوری می‌توانی بدون حساب هم ادامه بدهی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TabRow(
                            selectedTabIndex = mode,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            divider = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = mode == 0,
                                onClick = { mode = 0; message = null },
                                text = { Text("ورود", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = mode == 1,
                                onClick = { mode = 1; message = null },
                                text = { Text("ساخت حساب", fontWeight = FontWeight.Bold) }
                            )
                        }

                        if (mode == 1) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("نام و نام خانوادگی") },
                                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("ایمیل") },
                            leadingIcon = { Icon(Icons.Rounded.CloudDone, null) },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("رمز عبور") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )

                        message?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                loading = true
                                message = null
                                if (mode == 0) {
                                    onSignIn(email.trim(), password, ::resultHandler)
                                } else {
                                    onSignUp(name.trim(), email.trim(), password, ::resultHandler)
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.width(20.dp)
                                )
                            } else {
                                Text(
                                    if (mode == 0) "ورود به Student OS" else "ساخت حساب",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (mode == 0) {
                            TextButton(
                                onClick = { onForgotPassword(email.trim()) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("رمز عبور را فراموش کردم")
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(Modifier.weight(1f))
                            Text(
                                "  یا  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Divider(Modifier.weight(1f))
                        }

                        OutlinedButton(
                            onClick = {
                                loading = true
                                message = null
                                onGoogleSignIn { ok, text ->
                                    loading = false
                                    message = if (ok) null else text
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("ادامه با حساب Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_as_guest_button")
                ) {
                    Text(
                        "فعلاً بدون حساب ادامه بده",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Security,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 5.dp)
                    )
                    Text(
                        "ورود Google با Credential Manager انجام می‌شود و اطلاعات رمز عبور در برنامه ذخیره نمی‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
