package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberReducedMotion

private data class NavItem(
    val tab: AppTab,
    val outlineIcon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    val items = remember {
        listOf(
            NavItem(AppTab.DASHBOARD, Icons.Outlined.Home, Icons.Rounded.Home, "خانه"),
            NavItem(AppTab.SCHEDULE, Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth, "برنامه"),
            NavItem(AppTab.TASKS, Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle, "کارها"),
            NavItem(AppTab.GRADES, Icons.Outlined.BarChart, Icons.Rounded.BarChart, "کارنامه")
        )
    }
    val isMoreSelected = items.none { it.tab == selectedTab }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
            ModernNavItem(items[0], selectedTab == items[0].tab, { onTabSelected(items[0].tab) }, reducedMotion, Modifier.weight(1f))
            ModernNavItem(items[1], selectedTab == items[1].tab, { onTabSelected(items[1].tab) }, reducedMotion, Modifier.weight(1f))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                CenterActionButton({ showAllModulesSheet = true }, reducedMotion)
            }
            ModernNavItem(items[2], selectedTab == items[2].tab, { onTabSelected(items[2].tab) }, reducedMotion, Modifier.weight(1f))
            ModernNavItem(
                NavItem(AppTab.DASHBOARD, Icons.Outlined.MoreHoriz, Icons.Outlined.MoreHoriz, "بیشتر"),
                isMoreSelected,
                { showAllModulesSheet = true },
                reducedMotion,
                Modifier.weight(1f)
            )
            }
        }
    }

    if (showAllModulesSheet) {
        StudentModuleHubSheet(
            currentTab = selectedTab,
            onSelectTab = {
                onTabSelected(it)
                showAllModulesSheet = false
            },
            onDismiss = { showAllModulesSheet = false }
        )
    }
}

@Composable
private fun ModernNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = if (reducedMotion) tween(120) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_scale_${item.label}"
    )

    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .scale(scale)
            .semantics { role = Role.Tab }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 34.dp)
                .background(
                    if (selected) primary.copy(alpha = if (isSystemInDarkTheme()) 0.20f else 0.12f) else Color.Transparent,
                    RoundedCornerShape(17.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.outlineIcon,
                contentDescription = item.label,
                tint = if (selected) primary else onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = item.label,
            color = if (selected) primary else onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun CenterActionButton(
    onClick: () -> Unit,
    reducedMotion: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = if (reducedMotion) tween(120) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "center_action_scale"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "امکانات و ابزارهای تحصیلی",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Preview(name = "Modern Navigation Light", showBackground = true)
@Composable
private fun NavigationLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        FloatingIslandNavigationBar(AppTab.DASHBOARD, {})
    }
}

@Preview(name = "Modern Navigation Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun NavigationDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        FloatingIslandNavigationBar(AppTab.DASHBOARD, {})
    }
}
