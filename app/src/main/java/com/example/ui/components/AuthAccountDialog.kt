package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserAccount
import com.example.ui.theme.StudentOsColors

@Composable
fun AuthAccountDialog(
    userAccount: UserAccount,
    onSignInEmail: (String, String) -> Unit,
    onSignUpEmail: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenUpgrade: () -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (userAccount.isGuest) 0 else 2) } // 0: Sign In, 1: Sign Up, 2: Account Details
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 560.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudentOsColors.ElectricBlue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = StudentOsColors.ElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "مدیریت حساب و همگام‌سازی",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userAccount.isGuest) "حساب کاربری مهمان (آفلاین)" else "حساب متصل ابری: ${userAccount.displayName}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (userAccount.isGuest) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            "ورود و ثبت‌نام فقط از صفحه اصلی",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "برای جلوگیری از دو رابط کاربری متفاوت، احراز هویت در همان صفحه ورود اصلی انجام می‌شود.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            } else {
                    // Sign Up Form
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام و نام خانوادگی", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ایمیل (دانشگاهی یا شخصی)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور (حداقل ۶ نویسه)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = { onSignUpEmail(name, email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.ElectricBlue),
                        enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6
                    ) {
                        Text("ثبت‌نام و حفظ دائمی اطلاعات", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            } else {
                // Logged In Account Dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StudentOsColors.ElectricBlue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = StudentOsColors.ElectricBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userAccount.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        userAccount.email?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudentOsColors.CyberViolet.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Rounded.Stars, contentDescription = null, modifier = Modifier.size(15.dp), tint = StudentOsColors.CyberViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = userAccount.subscription.tier.titleFa,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudentOsColors.CyberViolet
                            )
                        }
                    }
                }

                // Pro Upgrade Button
                OutlinedButton(
                    onClick = onOpenUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Stars, contentDescription = null, tint = StudentOsColors.EmeraldNeon, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ارتقا به نسخه پرو و امکانات ابری", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Sync Now Button
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.ElectricBlue)
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("همگام‌سازی فوری با سرور ابری", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                // Sign Out
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خروج از حساب کاربری", fontSize = 12.sp)
                }

                // Delete Account
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = StudentOsColors.CrimsonRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف دائمی حساب و پاکسازی داده‌ها", color = StudentOsColors.CrimsonRose, fontSize = 11.5.sp)
                }
            }

            if (showDeleteConfirm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudentOsColors.CrimsonRose.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudentOsColors.CrimsonRose.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "هشدار حذف حساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = StudentOsColors.CrimsonRose
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "با این کار تمام اطلاعات حساب، نمرات و پشتیبان‌ها پاک می‌شود. آیا مطمئن هستید؟",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("انصراف", fontSize = 11.5.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDeleteAccount()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.CrimsonRose)
                            ) {
                                Text("تأیید و حذف", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("بستن", fontSize = 12.sp)
                }
            }
        }
    }
}
