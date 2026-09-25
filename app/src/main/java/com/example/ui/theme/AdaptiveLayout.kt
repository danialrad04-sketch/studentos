package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StudentWindowWidth {
    Compact,
    Medium,
    Expanded
}

data class StudentAdaptiveMetrics(
    val width: StudentWindowWidth,
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp,
    val isDense: Boolean
)

@Composable
fun rememberStudentAdaptiveMetrics(): StudentAdaptiveMetrics {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> StudentAdaptiveMetrics(
            width = StudentWindowWidth.Compact,
            horizontalPadding = 16.dp,
            contentMaxWidth = 840.dp,
            isDense = false
        )
        widthDp < 840 -> StudentAdaptiveMetrics(
            width = StudentWindowWidth.Medium,
            horizontalPadding = 24.dp,
            contentMaxWidth = 960.dp,
            isDense = false
        )
        else -> StudentAdaptiveMetrics(
            width = StudentWindowWidth.Expanded,
            horizontalPadding = 32.dp,
            contentMaxWidth = 1200.dp,
            isDense = true
        )
    }
}
