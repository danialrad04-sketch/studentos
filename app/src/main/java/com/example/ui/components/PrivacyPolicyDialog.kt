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
 * In-app Persian disclosure of actual local and optional connected account behavior.
 * Outlines local Room storage, Firebase Auth, Cloud Firestore sync, data retention, and account deletion.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    StudentGlassModalSheet(
        title = "حریم خصوصی",
        subtitle = "نحوهٔ نگهداری و استفاده از اطلاعات",
        onDismiss = onDismiss,
        maxWidth = 580.dp
    ) {
        val dismissModal = LocalStudentModalDismiss.current ?: onDismiss

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header


            // Section 1: Collected Data
            PrivacySectionCard(
                icon = Icons.Rounded.Security,
                title = "۱. چه داده‌هایی ذخیره می‌شوند؟",
                description = "اپلیکیشن دستیار دانشجو داده‌های ضروری تحصیلی شامل نام دانشجو، شماره دانشجویی، دانشگاه و رشته، فهرست دروس، نمرات، وضعیت حضور و غیاب، یادداشت‌ها و تکالیف را ذخیره می‌کند. این داده‌ها صرفاً جهت سازماندهی امور تحصیلی و هشدار غیبت در اختیار شما قرار می‌گیرند."
            )

            // Section 2: Storage & Firebase
            PrivacySectionCard(
                icon = Icons.Rounded.CloudDone,
                title = "۲. اطلاعات محلی و حساب‌های قبلی",
                description = "اطلاعات تحصیلی در پایگاه دادهٔ محلی دستگاه نگهداری می‌شود. در این نسخه ورود، ثبت‌نام و خرید اشتراک جدید غیرفعال است. برای حساب متصل از نسخه‌های قبلی، Firebase Authentication، Cloud Firestore و سرور Student OS می‌توانند برای هویت حساب و همگام‌سازی اطلاعات تحصیلی استفاده شوند. نسخهٔ پشتیبان سیستم اندروید، در صورت فعال‌بودن در تنظیمات دستگاه، ممکن است داده‌های محلی را منتقل کند؛ اطلاعات امن ورود و رسیدهای ذخیره‌شده از پشتیبان برنامه مستثنا هستند."
            )

            // Section 3: Third Party Sharing
            PrivacySectionCard(
                icon = Icons.Rounded.Lock,
                title = "۳. خدمات آنلاین و استفاده از داده",
                description = "داده‌ها برای تبلیغات فروخته نمی‌شوند. قابلیت‌های آنلاین حساب‌های قبلی ممکن است متن درخواست، اطلاعات تحصیلی لازم و عکس انتخاب‌شده برای اسکن را برای پردازش به سرویس‌های Firebase و هوش مصنوعی Google ارسال کنند. تحلیل آفلاین به ارسال درخواست آنلاین نیاز ندارد. خرید و بازیابی خرید بازار در این نسخه انجام نمی‌شود؛ برنامه اطلاعات کارت بانکی شما را دریافت نمی‌کند."
            )

            PrivacySectionCard(
                icon = Icons.Rounded.Policy,
                title = "دسترسی‌های برنامه",
                description = "اینترنت برای خدمات آنلاین حساب متصل، اعلان‌ها برای یادآوری امتحان و تکلیف، و هشدار دقیق برای زمان‌بندی یادآورها استفاده می‌شود. رد کردن اجازهٔ اعلان، مانع استفاده از برنامه نیست. اگر هشدار دقیق مجاز نباشد، یادآور با زمان تقریبی ثبت می‌شود. انتخاب عکس و فایل پشتیبان با انتخاب‌گر سیستم انجام می‌شود؛ برنامه دسترسی عمومی به مخاطبین، پیامک، مکان، میکروفون یا همهٔ فایل‌های دستگاه درخواست نمی‌کند."
            )

            // Local deletion and existing account deletion.
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
                            text = "۴. حذف اطلاعات محلی و حساب قبلی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = StudentOsColors.CrimsonRose
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای پاک‌کردن اطلاعات این دستگاه، از تنظیمات ← داده‌ها و پشتیبان ← پاکسازی اطلاعات محلی استفاده کنید. حساب متصل قبلی گزینهٔ حذف دائمی حساب را در بخش حساب دارد؛ حذف ابری تنها پس از پاسخ موفق سرویس تأیید می‌شود. فایل‌های پشتیبان و تصاویر خروجی که خودتان ذخیره یا به اشتراک گذاشته‌اید باید جداگانه حذف شوند. برای پرسش‌های حریم خصوصی، از راه تماس توسعه‌دهنده در صفحهٔ برنامه در بازار استفاده کنید.",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = dismissModal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_policy_dismiss_button"),
                shape = StudentShapeTokens.Compact
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("متوجه شدم", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
