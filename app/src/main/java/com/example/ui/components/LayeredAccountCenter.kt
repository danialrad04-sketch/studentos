package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.UserAccount
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

@Composable
fun LayeredAccountCenter(
    userAccount: UserAccount,
    onSyncNow: () -> Unit,
    syncState: SyncUiState = SyncUiState.Idle,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StudentSpacing.Md)) {
        TabRow(
            selectedTabIndex = section,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            contentColor = AcademicNavy
        ) {
            Tab(selected = section == 0, onClick = { section = 0 }, text = { Text("Identity") }, icon = {
                Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            })
            Tab(selected = section == 1, onClick = { section = 1 }, text = { Text("Security") }, icon = {
                Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            })
            Tab(selected = section == 2, onClick = { section = 2 }, text = { Text("Data") }, icon = {
                Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            })
        }

        when (section) {
            0 -> {
                AccountInfoCard(userAccount)
                OutlinedButton(onClick = onOpenUpgrade, modifier = Modifier.fillMaxWidth()) {
                    Text("مدیریت اشتراک و امکانات")
                }
            }
            1 -> {
                SecurityInfoCard()
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("خروج از حساب")
                }
            }
            2 -> {
                DataTransparencyCard()
                DataControlsCard(
                    onSyncNow = onSyncNow,
                    onDeleteAccount = onDeleteAccount,
                    syncState = syncState
                )
            }
        }
    }
}

@Composable
private fun DataTransparencyCard() {
    Card(
        shape = StudentShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(StudentSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(StudentSpacing.Sm)
        ) {
            Text("شفافیت داده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "داده‌های تحصیلی برای برنامه‌ریزی، تحلیل و همگام‌سازی استفاده می‌شوند. اطلاعات هویت حساب از داده‌های تحصیلی جدا نگه داشته می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("• دروس، برنامه و تکالیف → داده‌های تحصیلی", style = MaterialTheme.typography.bodySmall)
            Text("• ایمیل و ورود → داده‌های هویتی/امنیتی", style = MaterialTheme.typography.bodySmall)
            Text("• همگام‌سازی → فقط برای حساب متصل", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AccountInfoCard(userAccount: UserAccount) {
    Card(
        shape = StudentShapeTokens.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(StudentSpacing.Xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = AcademicNavy)
                Spacer(Modifier.size(StudentSpacing.Md))
                Column {
                    Text(userAccount.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    userAccount.email?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(StudentSpacing.Md))
            Text(
                "این بخش فقط اطلاعات هویت حساب را نمایش می‌دهد؛ داده‌های تحصیلی در Academic Profile مدیریت می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SecurityInfoCard() {
    Card(shape = StudentShapeTokens.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
        Column(modifier = Modifier.padding(StudentSpacing.Xl)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = AcademicNavy)
            Spacer(Modifier.height(StudentSpacing.Md))
            Text("امنیت حساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(StudentSpacing.Sm))
            Text(
                "احراز هویت، نشست فعال و بازیابی حساب از لایه هویت جدا نگه داشته می‌شوند. خروج از حساب فقط نشست فعلی را می‌بندد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DataControlsCard(
    onSyncNow: () -> Unit,
    onDeleteAccount: () -> Unit,
    syncState: SyncUiState
) {
    Card(shape = StudentShapeTokens.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
        Column(modifier = Modifier.padding(StudentSpacing.Xl), verticalArrangement = Arrangement.spacedBy(StudentSpacing.Md)) {
            Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = AcademicOlive)
            Text("داده و همگام‌سازی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "همگام‌سازی داده‌های تحصیلی جدا از اطلاعات هویت مدیریت می‌شود. عملیات حذف داده باید آگاهانه و برگشت‌ناپذیر تلقی شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (syncState) {
                SyncUiState.Idle -> Unit
                SyncUiState.Syncing -> Text(
                    "در حال همگام‌سازی اطلاعات…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicNavy
                )
                is SyncUiState.Success -> Text(
                    syncState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicOlive
                )
                is SyncUiState.Error -> Text(
                    syncState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(onClick = onSyncNow, modifier = Modifier.fillMaxWidth(), shape = StudentShapeTokens.Compact) {
                Text("همگام‌سازی اکنون")
            }
            OutlinedButton(onClick = onDeleteAccount, modifier = Modifier.fillMaxWidth(), shape = StudentShapeTokens.Compact) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(StudentSpacing.Sm))
                Text("حذف حساب و داده‌ها")
            }
        }
    }
}
