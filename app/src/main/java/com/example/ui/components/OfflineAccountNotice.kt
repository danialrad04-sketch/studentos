package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Honest local-access fallback; never attempts authentication or creates cloud credentials. */
@Composable
fun OfflineAccountNotice(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("استفاده بدون حساب", style = MaterialTheme.typography.titleLarge)
        Text("برای استفاده از برنامهٔ کلاسی، تکالیف، امتحان‌ها و کارنامه نیازی به حساب یا اینترنت ندارید. اطلاعات روی همین دستگاه ذخیره می‌شود؛ از بخش پشتیبان و بازیابی یک نسخهٔ پشتیبان نگه دارید.", style = MaterialTheme.typography.bodyLarge)
        Text("ورود و ثبت‌نام آنلاین فعلاً در این نسخه فعال نیست. همگام‌سازی ابری، خرید اشتراک و خدمات آنلاین نیازمند حساب نیز برای کاربران مهمان در دسترس نیستند.", style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.fillMaxWidth().testTag("primary_guest_entry")) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("continue_as_guest_button")) {
                Text("ادامه بدون حساب")
            }
        }
    }
}
