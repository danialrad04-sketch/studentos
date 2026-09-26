package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

/**
 * Reusable Academic Premium primitives.
 *
 * These are intentionally small and composable. They provide the shared visual
 * language without forcing existing feature screens into a wholesale rewrite.
 */
@Composable
fun AcademicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = StudentShapeTokens.Card
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    } else {
        Card(
            modifier = modifier.semantics { role = Role.Button },
            onClick = onClick,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    }
}

@Composable
fun AcademicSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Spacer(Modifier.height(StudentSpacing.Xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun AcademicPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable (() -> Unit))? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = StudentShapeTokens.Compact,
        contentPadding = PaddingValues(horizontal = StudentSpacing.Lg, vertical = StudentSpacing.Md)
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(StudentSpacing.Sm))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AcademicStatusChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: (@Composable (() -> Unit))? = null
) {
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(Modifier.width(StudentSpacing.Xs))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AcademicEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(StudentSpacing.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(StudentSpacing.Md))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(StudentSpacing.Sm))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(StudentSpacing.Lg))
            AcademicPrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun AcademicErrorState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    retryLabel: String = "تلاش مجدد",
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(StudentSpacing.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(StudentSpacing.Md))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(StudentSpacing.Sm))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        onRetry?.let {
            Spacer(Modifier.height(StudentSpacing.Lg))
            OutlinedButton(
                onClick = it,
                shape = StudentShapeTokens.Compact,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(retryLabel) }
        }
    }
}
