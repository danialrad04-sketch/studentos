package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.models.AppTab

@Composable
fun AppBottomNavigation(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    FloatingIslandNavigationBar(selectedTab, onTabSelected, modifier)
}
