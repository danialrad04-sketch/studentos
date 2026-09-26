package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberReducedMotion

/**
 * Compact navigation shell. Secondary destinations are delegated to the
 * shared StudentModuleHubSheet so the compact and expanded shells expose
 * exactly the same module set.
 */
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllModulesSheet = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val primaryTabs = listOf(
        Triple(AppTab.DASHBOARD, AppEmojiType.HOME, "داشبورد"),
        Triple(AppTab.SCHEDULE, AppEmojiType.CALENDAR, "برنامه"),
        Triple(AppTab.TASKS, AppEmojiType.CHECK, "تسک‌ها"),
        Triple(AppTab.GRADES, AppEmojiType.CHART, "کارنامه"),
        Triple(AppTab.COPILOT, AppEmojiType.COPILOT, "کوپایلت")
    )
    val isOtherTab = primaryTabs.none { it.first == selectedTab }
    val reducedMotion = rememberReducedMotion()

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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryTabs.forEach { (tab, emojiType, label) ->
                    val selected = selectedTab == tab
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = if (reducedMotion) {
                            androidx.compose.animation.core.snap()
                        } else {
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        },
                        label = "tab_spring_$label"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(MaterialTheme.shapes.large)
                            .then(
                                if (selected) Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                                else Modifier
                            )
                            .tactileClickable { onTabSelected(tab) }
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppEmoji(
                                type = emojiType,
                                size = if (selected) 28.dp else 24.dp,
                                shapeRadiusRatio = 0.30f,
                                elevation = if (selected) 3.dp else 0.dp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selected) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    Modifier.width(12.dp).height(3.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.large)
                        .then(
                            if (isOtherTab) Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                            else Modifier
                        )
                        .tactileClickable { showAllModulesSheet.value = true }
                        .minimumInteractiveComponentSize()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppEmoji(
                            type = AppEmojiType.BOOK,
                            size = if (isOtherTab) 28.dp else 24.dp,
                            shapeRadiusRatio = 0.30f,
                            elevation = if (isOtherTab) 3.dp else 0.dp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "بیشتر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOtherTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOtherTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAllModulesSheet.value) {
        StudentModuleHubSheet(
            currentTab = selectedTab,
            onSelectTab = onTabSelected,
            onDismiss = { showAllModulesSheet.value = false }
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
