package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Device-local presentation preference; contains no account or academic data. */
@Composable
fun StudentInlineHelp(id: String, text: String) {
    val context = LocalContext.current.applicationContext
    val preferences =
        remember(context) {
            context.getSharedPreferences("student_inline_help", Context.MODE_PRIVATE)
        }
    var expanded by rememberSaveable(id) { mutableStateOf(!preferences.getBoolean(id, false)) }
    if (expanded) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        preferences.edit().putBoolean(id, true).apply()
                        expanded = false
                    },
                    modifier = Modifier.testTag("help_close_$id"),
                ) {
                    Text("متوجه شدم")
                }
            }
        }
    } else {
        TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("help_open_$id")) {
            Text("راهنمای این بخش")
        }
    }
}
