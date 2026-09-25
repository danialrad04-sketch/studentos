package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

/**
 * Shared module hub used by both compact bottom navigation and expanded navigation rail.
 * Keeps every academic destination reachable without duplicating navigation logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentModuleHubSheet(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("همه بخش‌ها و ماژول‌های تحصیلی", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "دسترسی سریع به کلیه ابزارهای هوشمند Student OS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "۱۳ بخش فعال",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val modules = listOf(
                HubItem(AppTab.DASHBOARD, AppEmojiType.HOME, "داشبورد مرکزی", "نمای بنتو و خلاصه آمار"),
                HubItem(AppTab.COPILOT, AppEmojiType.COPILOT, "دستیار Copilot", "مشاور تحصیلی و هوش مصنوعی"),
                HubItem(AppTab.ACADEMIC_INTELLIGENCE, AppEmojiType.CHART, "هوش تحصیلی", "خلاصه و تحلیل وضعیت تحصیلی"),
                HubItem(AppTab.SCHEDULE, AppEmojiType.CALENDAR, "برنامه هفتگی", "تقویم کلاس‌ها و تداخل‌ها"),
                HubItem(AppTab.ATTENDANCE, AppEmojiType.TARGET, "رادار حضور و غیاب", "مانیتور حضور و غیاب"),
                HubItem(AppTab.TASKS, AppEmojiType.CHECK, "مدیریت تکالیف", "اسپرینت و پروژه‌های درسی"),
                HubItem(AppTab.EXAMS, AppEmojiType.BELL, "امتحانات و موعدها", "شمارش معکوس و یادآورها"),
                HubItem(AppTab.GRADES, AppEmojiType.CHART, "کارنامه و معدل", "محاسبه معدل و شبیه‌ساز"),
                HubItem(AppTab.SEMESTER_PLANNER, AppEmojiType.BOOK, "برنامه‌ریز ترم", "برنامه‌ریزی نیم‌سال"),
                HubItem(AppTab.POMODORO, AppEmojiType.POMODORO, "تمرکز پومودورو", "تایمر مطالعه و یادداشت"),
                HubItem(AppTab.CURRICULUM, AppEmojiType.BOOK, "چارت دروس مصوب", "پیش‌نیازها و ترم‌بندی"),
                HubItem(AppTab.PASSPORT, AppEmojiType.GRAD_CAP, "پاسپورت تحصیلی", "گزارش و هویت آکادمیک"),
                HubItem(AppTab.GAMIFICATION, AppEmojiType.TROPHY, "تالار افتخارات", "استریک و دستاوردهای آکادمیک"),
                HubItem(AppTab.HISTORY, AppEmojiType.BOOK, "سوابق ترم‌ها", "مرور تاریخچه تحصیلی")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(StudentSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(StudentSpacing.Sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(modules) { item ->
                    val selected = currentTab == item.tab
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactileClickable {
                                onSelectTab(item.tab)
                                onDismiss()
                            },
                        shape = StudentShapeTokens.Card,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(StudentSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppEmoji(
                                type = item.emojiType,
                                size = 36.dp,
                                shapeRadiusRatio = 0.28f,
                                elevation = 2.dp
                            )
                            Spacer(Modifier.width(StudentSpacing.Sm))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HubItem(
    val tab: AppTab,
    val emojiType: AppEmojiType,
    val title: String,
    val subtitle: String
)
