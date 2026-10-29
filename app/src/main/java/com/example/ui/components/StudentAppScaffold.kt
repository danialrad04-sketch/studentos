package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.studentWorkspaceBackground

/** Scroll containers apply this inside their scroll content, not to the viewport. */
val LocalStudentNavigationClearance = staticCompositionLocalOf { 0.dp }

/** Phone navigation overlays the full canvas; the final scroll item can still clear it. */
@Composable
fun StudentAppScaffold(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    onOpenMore: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var overlayHeight by remember { mutableStateOf(96.dp) }
    BoxWithConstraints(modifier.fillMaxSize().studentWorkspaceBackground()) {
        val useRail = maxWidth >= 600.dp
        val clearance = if (useRail) 0.dp else overlayHeight
        val backdrop = if (useRail) null else rememberStudentNavigationBackdrop()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets =
                if (useRail) ScaffoldDefaults.contentWindowInsets
                else
                    ScaffoldDefaults.contentWindowInsets.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
            snackbarHost = { Box(Modifier.padding(bottom = clearance)) { snackbarHost() } },
        ) { insets ->
            Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                if (useRail)
                    StudentAdaptiveNavigationRail(
                        selectedTab,
                        onTabSelected,
                        onOpenMore = onOpenMore,
                    )
                CompositionLocalProvider(LocalStudentNavigationClearance provides clearance) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().captureNavigationBackdrop(backdrop),
                        content = content,
                    )
                }
            }
        }
        if (!useRail)
            CompositionLocalProvider(LocalStudentNavigationBackdrop provides backdrop) {
                FloatingIslandNavigationBar(
                    selectedTab,
                    onTabSelected,
                    Modifier.align(Alignment.BottomCenter).onSizeChanged { size ->
                        val measured = with(density) { size.height.toDp() }
                        if (measured != overlayHeight) overlayHeight = measured
                    },
                )
            }
    }
}
