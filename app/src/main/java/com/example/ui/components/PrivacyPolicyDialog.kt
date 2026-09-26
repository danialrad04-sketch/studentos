package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive

/**
 * In-App Persian Privacy Policy screen complying with Google Play Developer Program policies.
 * Outlines local Room storage, Firebase Auth, Cloud Firestore sync, data retention, and account deletion.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 580.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Policy,
                        contentDescription = "سیاست حفظ حریم خصوصی",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "سیاست حفظ حریم خصوصی و امنیت داده‌ها",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "شفافیت در نگهداری، حفاظت و حق پاکسازی داده‌های دانشجویی",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Collected Data
            PrivacySectionCard(
                icon = Icons.Rounded.Security,
                title = "۱. چه داده‌هایی ذخیره می‌شوند؟",
                description = "اپلیکیشن دستیار دانشجو داده‌های ضروری تحصیلی شامل نام دانشجو، شماره دانشجویی، دانشگاه و رشته، فهرست دروس، نمرات، وضعیت حضور و غیاب، یادداشت‌ها و تکالیف را ذخیره می‌کند. این داده‌ها صرفاً جهت سازماندهی امور تحصیلی و هشدار غیبت در اختیار شما قرار می‌گیرند."
            )

            // Section 2: Storage & Firebase
            PrivacySectionCard(
                icon = Icons.Rounded.CloudDone,
                title = "۲. ذخیره‌سازی ابری و احراز هویت (Firebase)",
                description = "در حالت عادی تمام اطلاعات در دیتابیس محلی (Room) روی حافظه دستگاه ذخیره می‌شود. در صورت ثبت‌نام یا ورود، هویت حساب در Firebase Authentication مدیریت می‌شود و داده‌های همگام‌شده می‌توانند در Cloud Firestore و سرور اختصاصی Student OS نگهداری شوند تا با تغییر دستگاه اطلاعات حفظ شود."
            )

            // Section 3: Third Party Sharing
            PrivacySectionCard(
                icon = Icons.Rounded.Lock,
                title = "۳. عدم اشتراک‌گذاری با اشخاص ثالث",
                description = "داده‌های شما برای تبلیغات یا بازاریابی فروخته یا استفاده نمی‌شوند. سرویس‌های موردنیاز برنامه مانند Firebase Authentication، Cloud Firestore و در حالت اتصال حساب، سرور اختصاصی Student OS برای احراز هویت و همگام‌سازی استفاده می‌شوند و نوع استفاده از داده در بخش‌های بعدی توضیح داده شده است."
            )

            // Section 4: Account Deletion (Google Play Mandate)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = StudentShapeTokens.Card,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    StudentOsColors.CrimsonRose.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = StudentOsColors.CrimsonRose,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "۴. حق حذف کامل حساب کاربری و تمامی داده‌ها",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StudentOsColors.CrimsonRose
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "شما می‌توانید از داخل حساب کاربری درخواست حذف کنید. در این فرایند، حذف داده‌های ابری تا زمانی موفق تلقی نمی‌شود که سرویس مربوطه نتیجه موفقیت‌آمیز برگرداند؛ سپس داده‌های محلی حساب نیز پاکسازی می‌شوند.",
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_policy_dismiss_button"),
                shape = StudentShapeTokens.Compact
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("متوجه شدم", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PrivacySectionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
