package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

/**
 * 2026 Redesigned Floating Island Navigation Bar with Squircle Emoji Pack
 * Features:
 * - Fluid spring bounce physics
 * - Rich squircle emoji icons from the custom emoji pack
 * - Pure Material 3 Expressive colorScheme tokens & surfaceContainer tones
 * - Instant access to all secondary hubs via "بیشتر" modal sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }

    // Primary Tabs + "بیشتر" hub as specified by 2026 Student OS Design System
    val primaryTabs = listOf(
        Triple(AppTab.DASHBOARD, AppEmojiType.HOME, "داشبورد"),
        Triple(AppTab.SCHEDULE, AppEmojiType.CALENDAR, "برنامه"),
        Triple(AppTab.TASKS, AppEmojiType.CHECK, "تسک‌ها"),
        Triple(AppTab.GRADES, AppEmojiType.CHART, "کارنامه"),
        Triple(AppTab.COPILOT, AppEmojiType.COPILOT, "کوپایلت")
    )

    val isOtherTab = primaryTabs.none { it.first == selectedTab }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryTabs.forEach { (tab, emojiType, label) ->
                    val isSelected = selectedTab == tab

                    val tabScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tab_spring_$label"
                    )

                    val activeContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    val activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    val activeTextColor = MaterialTheme.colorScheme.primary
                    val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val activeDotColor = MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(tabScale)
                            .clip(MaterialTheme.shapes.large)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(activeContainerColor)
                                        .border(
                                            1.dp,
                                            activeBorderColor,
                                            MaterialTheme.shapes.large
                                        )
                                } else Modifier
                            )
                            .tactileClickable { onTabSelected(tab) }
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AppEmoji(
                                type = emojiType,
                                size = if (isSelected) 28.dp else 24.dp,
                                shapeRadiusRatio = 0.30f,
                                elevation = if (isSelected) 3.dp else 0.dp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activeTextColor else inactiveTextColor
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(activeDotColor)
                                )
                            }
                        }
                    }
                }

                // "بیشتر" Hub trigger for secondary modules
                val isMoreSelected = isOtherTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large)
                        .then(
                            if (isMoreSelected) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.shapes.large
                                    )
                            } else Modifier
                        )
                        .tactileClickable { showAllModulesSheet = true }
                        .minimumInteractiveComponentSize()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppEmoji(
                            type = AppEmojiType.BOOK,
                            size = if (isMoreSelected) 28.dp else 24.dp,
                            shapeRadiusRatio = 0.30f,
                            elevation = if (isMoreSelected) 3.dp else 0.dp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "بیشتر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMoreSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAllModulesSheet) {
        AllModulesModalSheet(
            currentTab = selectedTab,
            onSelectTab = { tab ->
                showAllModulesSheet = false
                onTabSelected(tab)
            },
            onDismiss = { showAllModulesSheet = false }
        )
    }
}

/**
 * All Modules & Academic Hubs Bottom Sheet with New Emoji Pack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllModulesModalSheet(
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
                    Text(
                        text = "همه بخش‌ها و ماژول‌های تحصیلی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "دسترسی سریع به کلیه ابزارهای هوشمند Student OS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "۱۱ بخش فعال",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hubModules = listOf(
                HubItem(AppTab.DASHBOARD, AppEmojiType.HOME, "داشبورد مرکزی", "نمای بنتو و خلاصه آمار"),
                HubItem(AppTab.COPILOT, AppEmojiType.COPILOT, "دستیار Copilot", "مشاور تحصیلی و هوش مصنوعی"),
                HubItem(AppTab.SCHEDULE, AppEmojiType.CALENDAR, "برنامه هفتگی", "تقویم کلاس‌ها و تداخل‌ها"),
                HubItem(AppTab.ATTENDANCE, AppEmojiType.TARGET, "رادار حضور و غیاب", "مانیتور قانون ۳/۱۶ غیبت"),
                HubItem(AppTab.TASKS, AppEmojiType.CHECK, "مدیریت تکالیف", "اسپرینت و پروژه‌های درسی"),
                HubItem(AppTab.EXAMS, AppEmojiType.BELL, "امتحانات و موعدها", "شمارش معکوس و یادآورها"),
                HubItem(AppTab.GRADES, AppEmojiType.CHART, "کارنامه و معدل", "محاسبه معدل الف و شبیه‌ساز"),
                HubItem(AppTab.POMODORO, AppEmojiType.POMODORO, "تمرکز پومودورو", "تایمر مطالعه و یادداشت"),
                HubItem(AppTab.CURRICULUM, AppEmojiType.BOOK, "چارت دروس مصوب", "پیش‌نیازها و ترم‌بندی"),
                HubItem(AppTab.PASSPORT, AppEmojiType.GRAD_CAP, "پاسپورت تحصیلی", "گواهی پیشرفت آکادمیک"),
                HubItem(AppTab.GAMIFICATION, AppEmojiType.TROPHY, "تالار افتخارات", "استریک و مدال‌های آکادمیک")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(StudentSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(hubModules) { item ->
                    val isSelected = currentTab == item.tab
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactileClickable { onSelectTab(item.tab) },
                        shape = StudentShapeTokens.Card,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                            1.5.dp, MaterialTheme.colorScheme.primary
                        ) else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(StudentSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppEmoji(
                                type = item.emojiType,
                                size = 36.dp,
                                shapeRadiusRatio = 0.28f,
                                elevation = 2.dp
                            )

                            Spacer(modifier = Modifier.width(StudentSpacing.Sm))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subtitle,
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

@Preview(name = "Floating Nav Light", showBackground = true)
@Composable
private fun FloatingIslandNavPreviewLight() {
    MyApplicationTheme(darkTheme = false) {
        FloatingIslandNavigationBar(
            selectedTab = AppTab.DASHBOARD,
            onTabSelected = {}
        )
    }
}

@Preview(name = "Floating Nav Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FloatingIslandNavPreviewDark() {
    MyApplicationTheme(darkTheme = true) {
        FloatingIslandNavigationBar(
            selectedTab = AppTab.DASHBOARD,
            onTabSelected = {}
        )
    }
}

