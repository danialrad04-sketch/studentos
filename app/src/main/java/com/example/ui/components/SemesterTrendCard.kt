package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SemesterEntity

@Composable
fun SemesterTrendCard(semesters: List<SemesterEntity>) {
    val history = semesters.filter { it.isArchived && it.gpa != null && it.gpa.isFinite() && it.gpa in 0.0..20.0 }
        .sortedWith(compareBy<SemesterEntity> { it.academicYear }.thenBy { it.termNumber }.thenBy { it.createdAt })
    if (history.isEmpty()) return
    val points = history.takeLast(6)
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("روند معدل ترم‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium)
            if (points.size >= 2) {
                val delta = points.last().gpa!! - points[points.lastIndex - 1].gpa!!
                Text("تغییر آخرین ترم: ${java.lang.String.format(java.util.Locale.US, "%+.2f", delta)}", style = MaterialTheme.typography.bodyLarge)
                Canvas(Modifier.fillMaxWidth().height(100.dp).semantics { contentDescription = points.joinToString("؛ ") { "${it.title}: معدل ${it.gpa}" } }) {
                    drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    val padding = 5.dp.toPx()
                    val positions = points.mapIndexed { index, semester -> Offset(padding + (size.width - padding * 2) * index / (points.size - 1), padding + (size.height - padding * 2) * (1f - semester.gpa!!.toFloat() / 20f)) }
                    positions.zipWithNext().forEach { (a, b) -> drawLine(accent, a, b, strokeWidth = 3.dp.toPx()) }
                    positions.forEach { drawCircle(accent, 4.dp.toPx(), it) }
                }
            }
            points.forEach { semester -> Text("${semester.title} · ${java.lang.String.format(java.util.Locale.US, "%.2f", semester.gpa)} · ${semester.totalUnits} واحد", style = MaterialTheme.typography.bodyMedium) }
            Text("فقط ترم‌هایی که معدل دارند در نمودار محاسبه شده‌اند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
