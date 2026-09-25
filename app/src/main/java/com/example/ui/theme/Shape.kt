package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Restrained Academic Premium shape system. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object StudentShapeTokens {
    val Compact = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(16.dp)
    val Hero = RoundedCornerShape(20.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}
