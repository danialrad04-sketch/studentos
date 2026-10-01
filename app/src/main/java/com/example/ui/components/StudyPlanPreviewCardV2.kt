package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.StudySessionRecommendation
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.AcademicNavy

@Composable
fun StudyPlanPreviewCardV2(
    recommendations: List<StudySessionRecommendation>,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        )
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AcademicOlive.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = AcademicOlive,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "پیشنهاد مطالعه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "بر اساس امتحان‌ها و کارهای باز اولویت‌بندی شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            recommendations.take(3).forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp, max = 88.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (index == 0) {
                        AcademicOlive.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (index == 0) AcademicOlive.copy(alpha = 0.14f) else AcademicNavy.copy(alpha = 0.09f)
                        ) {
                            Text(
                                (index + 1).toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (index == 0) AcademicOlive else AcademicNavy
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                item.courseName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.priorityReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            item.recommendedDurationMinutes.toString() + " دقیقه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AcademicOlive,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = onStartFocus,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AcademicOlive)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("شروع پیشنهاد اول")
            }
        }
    }
}

