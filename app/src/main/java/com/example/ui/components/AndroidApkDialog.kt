package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudentOsColors

@Composable
fun AndroidApkDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Bazaar guide

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 580.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .background(StudentOsColors.EmeraldNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = StudentOsColors.EmeraldNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "انتشار رسمی در کافه بازار و مارکت‌ها",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "راهنمای رفع خطای امضای دیباگ و تولید Release Key",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("مشخصات نسخه", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("حل خطای کافه بازار", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Specs & Features
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpecRow("نام بسته (Package Name):", "com.aistudio.studentos.appvzk")
                        SpecRow("کد نسخه (versionCode):", "1")
                        SpecRow("نام نسخه (versionName):", "1.0")
                        SpecRow("حداقل اندروید (minSdk):", "7.0 (API 24)")
                        SpecRow("اندروید هدف (targetSdk):", "15.0 (API 36)")
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        FeatureRow("پایگاه داده محلی آفلاین با Room (Zero Data Loss)")
                        FeatureRow("محاسبه خودکار معدل و شبیه‌ساز سقف ترم")
                        FeatureRow("رادار پایش هوشمند غیبت‌های ۳/۱۶ و ۴/۱۶")
                        FeatureRow("تایمر تمرکز پومودورو با بوق آلارم صوتی")
                        FeatureRow("دفترچه ذخیره آنی فرمول‌های امتحانی")
                    }
                }
            } else {
                // Bazaar Release Signing Guide
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudentOsColors.AmberGlow.copy(alpha = 0.14f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudentOsColors.AmberGlow.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ چرا کافه بازار این خطا را می‌دهد؟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StudentOsColors.AmberGlow
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "فایل base.apk خروجی تست/دیباگ بوده و با کلید پیش‌فرض تست اندروید امضا شده است. کافه بازار و تمام مارکت‌ها برای امنیت، فقط بسته‌های امضاشده با کلید اختصاصی انتشار (Release Keystore) را می‌پذیرند.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepHeader("روش سریع با Android Studio (پیشنهادی):")
                        StepItem("۱. دانلود سورس پروژه از منوی بالا (Export ZIP)")
                        StepItem("۲. باز کردن پروژه در اندروید استودیو")
                        StepItem("۳. منوی Build > Generate Signed Bundle / APK")
                        StepItem("۴. انتخاب APK > ساخت کلید اختصاصی (JKS)")
                        StepItem("۵. انتخاب Release و دریافت فایل نهایی app-release.apk")
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepHeader("روش امضا با ترمینال / keytool:")
                        Text(
                            text = "keytool -genkey -v -keystore release.jks -alias studentos -keyalg RSA -keysize 2048 -validity 10000",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StudentOsColors.ElectricBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        )
                        Text(
                            text = "apksigner sign --ks release.jks --out app-release.apk base.apk",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StudentOsColors.ElectricBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.ElectricBlue),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("متوجه شدم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Key,
            contentDescription = null,
            tint = StudentOsColors.ElectricBlue,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StepItem(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 15.sp,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = StudentOsColors.EmeraldNeon,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
