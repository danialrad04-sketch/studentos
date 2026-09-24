package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicBadge
import com.example.domain.model.BadgeCategory
import com.example.domain.model.BadgeTier
import com.example.domain.model.StudentGamificationProfile
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import com.example.ui.theme.studentColors

/**
 * Phase v5: Student OS Gamification & Badges Hub.
 * Dynamic Level progress bar, continuous daily streak fire, achievement tier badges,
 * and academic XP tracking.
 */
@Composable
fun StudentGamificationHub(
    profile: StudentGamificationProfile,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<BadgeCategory?>(null) }

    val filteredBadges = remember(profile.badges, selectedCategory) {
        if (selectedCategory == null) profile.badges
        else profile.badges.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Level, XP and Streak Hero Card
        StudentLevelAndStreakHeroCard(profile = profile)

        // 2. Quick Stat Counters Row
        GamificationStatCountersRow(profile = profile)

        // 3. Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = {
                        Text(
                            text = "همه مدال‌ها (${profile.badges.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandIndigo600,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(BadgeCategory.entries) { category ->
                val isSelected = selectedCategory == category
                val countInCat = profile.badges.count { it.category == category }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = "${category.icon} ${category.title} ($countInCat)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandIndigo600,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 4. Badges Grid / List with Empty State
        if (filteredBadges.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎖️", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "هیچ مدالی در این دسته‌بندی وجود ندارد",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "با انجام تسک‌ها، حضور منظم و پیشرفت در دروس مدال‌های جدید آنلاک کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredBadges.forEach { badge ->
                    AcademicBadgeCard(badge = badge)
                }
            }
        }
    }
}

@Composable
private fun StudentLevelAndStreakHeroCard(
    profile: StudentGamificationProfile,
    modifier: Modifier = Modifier
) {
    val targetProgress = (profile.currentXp.toFloat() / profile.nextLevelXp.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "xp_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 700f
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Row: Level Title & Streak Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(BrandIndigo600, MaterialTheme.colorScheme.primary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L${profile.level}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.levelTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "سطح ${profile.level} · سیستم دستاوردهای دانشجویی",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Streak Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.studentColors.streakFire.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.9.dp,
                            MaterialTheme.studentColors.streakFire.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "استریک پیوسته مطالعه: ${profile.studyStreakDays} روز"
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔥", style = MaterialTheme.typography.titleSmall)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${profile.studyStreakDays} روز",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.studentColors.streakFire
                                )
                                Text(
                                    text = "استریک پیوسته",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.studentColors.streakFire
                                )
                            }
                        }
                    }
                }

                // XP Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "امتیاز تجربه آکادمیک (XP)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${profile.currentXp} / ${profile.nextLevelXp} XP تا لول بعدی",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = BrandIndigo600
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BrandIndigo600,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun GamificationStatCountersRow(
    profile: StudentGamificationProfile,
    modifier: Modifier = Modifier
) {
    val unlockedCount = profile.badges.count { it.isUnlocked }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MiniMetricPill(
            icon = "🏆",
            title = "مدال‌های فتح‌شده",
            value = "$unlockedCount از ${profile.badges.size}",
            modifier = Modifier.weight(1f)
        )
        MiniMetricPill(
            icon = "⏱️",
            title = "تمرکز ثبت‌شده",
            value = "${profile.focusHoursTotal} ساعت",
            modifier = Modifier.weight(1f)
        )
        MiniMetricPill(
            icon = "📋",
            title = "تکالیف تحویلی",
            value = "${profile.completedTasksCount} مورد",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniMetricPill(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AcademicBadgeCard(
    badge: AcademicBadge,
    modifier: Modifier = Modifier
) {
    val tierColor = when (badge.tier) {
        BadgeTier.BRONZE -> Amber600
        BadgeTier.SILVER -> MaterialTheme.colorScheme.outline
        BadgeTier.GOLD -> Amber500
        BadgeTier.PLATINUM -> CyanNeon
        BadgeTier.DIAMOND -> BrandIndigo600
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "مدال ${badge.title}، سطح ${badge.tier.title}، ${if (badge.isUnlocked) "فعال شده" else "قفل شده، پاداش ${badge.xpReward} امتیاز تجربه"}"
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (badge.isUnlocked) tierColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Icon Circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) tierColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (badge.isUnlocked) tierColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge.iconEmoji, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tierColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badge.tier.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress or Unlock Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { badge.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (badge.isUnlocked) Emerald600 else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = badge.progressText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right side Unlock Pill or XP
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (badge.isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald600.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "فعال",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Emerald600
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "قفل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "+${badge.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
