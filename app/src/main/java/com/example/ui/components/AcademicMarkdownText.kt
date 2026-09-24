package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonRose
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.StudentOsColors

/**
 * High-performance Persian/RTL Markdown parser and renderer for Academic Copilot.
 * Supports:
 * - Bold highlights (**text**)
 * - Bullet lists (• or -) with styled hanging bullets
 * - Numbered steps (1. , 2. ) with numbered badges
 * - Special Callouts (💡, ⚠️, 🚨, ⚖️, 📜, 🎓, 📌, ✅)
 * - Section Dividers
 */
@Composable
fun AcademicMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val lines = text.lines()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Render code block
                    CodeBlockCard(codeBuffer.toString())
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                continue
            }

            if (trimmed.isBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                continue
            }

            when {
                // Callouts with special emojis
                trimmed.startsWith("🚨") || trimmed.startsWith("⚠️") || trimmed.startsWith("💡") ||
                trimmed.startsWith("⚖️") || trimmed.startsWith("📜") || trimmed.startsWith("🎓") ||
                trimmed.startsWith("🛡️") || trimmed.startsWith("📌") || trimmed.startsWith("✅") -> {
                    CalloutBox(
                        text = trimmed,
                        isUser = isUser
                    )
                }

                // Headers (### or ## or #)
                trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("#") -> {
                    val headerText = trimmed.replace(Regex("^#+\\s*"), "")
                    HeaderLine(headerText = headerText, isUser = isUser)
                }

                // Bullet points (• or - or *)
                trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletText = trimmed.replace(Regex("^[•\\-*]\\s*"), "")
                    BulletLine(bulletText = bulletText, isUser = isUser, fontSize = fontSize)
                }

                // Numbered lists (1. 2. 3. etc)
                trimmed.matches(Regex("^[0-9]+[.)]\\s+.*")) -> {
                    val numberMatch = Regex("^([0-9]+)[.)]\\s+(.*)").find(trimmed)
                    val number = numberMatch?.groupValues?.getOrNull(1) ?: "•"
                    val content = numberMatch?.groupValues?.getOrNull(2) ?: trimmed
                    NumberedStepLine(number = number, content = content, isUser = isUser, fontSize = fontSize)
                }

                // Standard paragraph
                else -> {
                    val annotated = buildMarkdownAnnotatedString(
                        raw = trimmed,
                        defaultColor = if (isUser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                        boldColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = annotated,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.55).sp,
                        textAlign = TextAlign.Start,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                    )
                }
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            CodeBlockCard(codeBuffer.toString())
        }
    }
}

@Composable
private fun HeaderLine(headerText: String, isUser: Boolean) {
    Spacer(modifier = Modifier.height(4.dp))
    val annotated = buildMarkdownAnnotatedString(
        raw = headerText,
        defaultColor = MaterialTheme.colorScheme.onSurface,
        boldColor = MaterialTheme.colorScheme.primary
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(StudentOsColors.CyanAccent)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = annotated,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
        )
    }
}

@Composable
private fun BulletLine(
    bulletText: String,
    isUser: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val annotated = buildMarkdownAnnotatedString(
        raw = bulletText,
        defaultColor = MaterialTheme.colorScheme.onSurface,
        boldColor = MaterialTheme.colorScheme.primary
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isUser) MaterialTheme.colorScheme.primary else StudentOsColors.CyanAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = annotated,
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.5).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
        )
    }
}

@Composable
private fun NumberedStepLine(
    number: String,
    content: String,
    isUser: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val annotated = buildMarkdownAnnotatedString(
        raw = content,
        defaultColor = MaterialTheme.colorScheme.onSurface,
        boldColor = MaterialTheme.colorScheme.primary
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudentOsColors.CyanAccent.copy(alpha = 0.15f))
                .border(0.6.dp, StudentOsColors.CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = StudentOsColors.CyanAccent
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = annotated,
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.5).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
        )
    }
}

@Composable
private fun CalloutBox(
    text: String,
    isUser: Boolean
) {
    val isAlert = text.contains("🚨") || text.contains("⚠️")
    val isTip = text.contains("💡") || text.contains("✨")
    val isLaw = text.contains("⚖️") || text.contains("📜") || text.contains("🛡️")

    val bgBrush = when {
        isAlert -> Brush.horizontalGradient(
            listOf(CrimsonRose.copy(alpha = 0.14f), CrimsonRose.copy(alpha = 0.05f))
        )
        isTip -> Brush.horizontalGradient(
            listOf(AmberGlow.copy(alpha = 0.14f), AmberGlow.copy(alpha = 0.04f))
        )
        isLaw -> Brush.horizontalGradient(
            listOf(CyberViolet.copy(alpha = 0.14f), CyberViolet.copy(alpha = 0.05f))
        )
        else -> Brush.horizontalGradient(
            listOf(CyberCyan.copy(alpha = 0.12f), CyberCyan.copy(alpha = 0.03f))
        )
    }

    val borderColor = when {
        isAlert -> CrimsonRose.copy(alpha = 0.35f)
        isTip -> AmberGlow.copy(alpha = 0.35f)
        isLaw -> CyberViolet.copy(alpha = 0.35f)
        else -> CyberCyan.copy(alpha = 0.3f)
    }

    val annotated = buildMarkdownAnnotatedString(
        raw = text,
        defaultColor = MaterialTheme.colorScheme.onSurface,
        boldColor = if (isAlert) CrimsonRose else if (isTip) AmberGlow else MaterialTheme.colorScheme.primary
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(0.8.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = annotated,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
        )
    }
}

@Composable
private fun CodeBlockCard(codeText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = codeText.trim(),
            fontSize = 11.5.sp,
            color = Color(0xFF38BDF8),
            lineHeight = 17.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr)
        )
    }
}

/**
 * Builds annotated string replacing **bold** with Bold weight and colored styling.
 */
private fun buildMarkdownAnnotatedString(
    raw: String,
    defaultColor: Color,
    boldColor: Color
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val parts = raw.split("**")
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 == 1) {
                // Bold segment
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = boldColor
                    )
                ) {
                    append(part)
                }
            } else {
                // Normal segment
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Normal,
                        color = defaultColor
                    )
                ) {
                    append(part)
                }
            }
        }
    }
}
