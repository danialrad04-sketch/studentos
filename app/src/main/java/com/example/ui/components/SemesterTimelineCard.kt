package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoLightAmber
import com.example.ui.theme.BentoLightAmberInk
import com.example.ui.theme.BentoLightAmberSoft
import com.example.ui.theme.BentoLightIndigo
import com.example.ui.theme.BentoLightMint
import com.example.ui.util.SemesterProgressInfo
import com.example.ui.util.SemesterTimelineHelper
import java.util.Date

@Composable
fun SemesterTimelineCard(
    currentDate: Date = Date(),
    modifier: Modifier = Modifier
) {
    val timelineInfo: SemesterProgressInfo = remember(currentDate) {
        SemesterTimelineHelper.calculateCurrentSemesterProgress(currentDate)
    }

    var isExpanded by remember { mutableStateOf(false) }

    val rawFraction = if (!timelineInfo.isStarted) 0.04f else (timelineInfo.progressFraction).coerceIn(0.04f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 800),
        label = "ruler_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tactileClickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top Row: Title + Countdown badge (HTML countdown-card style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سیر زمانی نیم‌سال اول",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = timelineInfo.subHeadline,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Countdown badge (HTML: .countdown-badge)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (!timelineInfo.isStarted) "شروع" else "پیشرفت",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = if (!timelineInfo.isStarted) "۱ مهر" else "${timelineInfo.progressPercent}٪",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realistic Bento Ruler with track, fill, tick marks and glowing dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Colored Ruler Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )

                // Ruler Ticks (7 subtle vertical ticks across the timeline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(9.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        )
                    }
                }

                // Glowing Head Dot positioned along progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Expand / Collapse Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "بستن جزئیات ایستگاه‌ها" else "مشاهده ایستگاه‌های ترم و فرجه‌ها",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded details & milestones
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MilestoneBadge(
                            title = "۱ مهر",
                            subtitle = "شروع کلاس‌ها",
                            isCurrent = !timelineInfo.isStarted,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        MilestoneBadge(
                            title = "آبان - آذر",
                            subtitle = "میان‌ترم‌ها",
                            isCurrent = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MilestoneBadge(
                            title = "۱۲ دی",
                            subtitle = "فرجه امتحانی",
                            isCurrent = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MilestoneBadge(
                            title = "۱۹ دی تا ۲ بهمن",
                            subtitle = "امتحانات نهایی",
                            isCurrent = false,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 تقویم آموزشی: تاریخ رسمی آغاز نیم‌سال اول دانشگاه‌ها ۱ مهرماه است و فرآیند ثبت نمرات تا اوایل بهمن ادامه دارد.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneBadge(
    title: String,
    subtitle: String,
    isCurrent: Boolean,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = subtitle,
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

