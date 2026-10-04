package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.models.AppTab

/** One inset owner for app destinations; content never sits behind navigation. */
@Composable
fun StudentAppScaffold(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    onOpenMore: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
    val useRail = maxWidth >= androidx.compose.ui.unit.Dp(600f)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        bottomBar = { if (!useRail) FloatingIslandNavigationBar(selectedTab, onTabSelected, onOpenMore = onOpenMore) }
    ) { insets ->
        Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
        if (useRail) StudentAdaptiveNavigationRail(selectedTab, onTabSelected, onOpenMore = onOpenMore)
        Box(
            Modifier.weight(1f).fillMaxHeight(),
            content = content
        )
        }
    }
    }
}
