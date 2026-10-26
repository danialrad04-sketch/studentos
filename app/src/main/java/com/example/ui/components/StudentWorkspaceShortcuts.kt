package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

@Composable
fun StudentWorkspaceShortcuts(onNavigate:(AppTab)->Unit,modifier:Modifier=Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked=maxWidth<340.dp || LocalDensity.current.fontScale>=1.3f
        val card: @Composable (AppTab,String,Modifier)->Unit={tab,subtitle,cardModifier->
            val colors=MaterialTheme.colorScheme
            val ink=if(tab==AppTab.AGENDA)colors.onPrimaryContainer else colors.onSecondaryContainer
            Surface(onClick={onNavigate(tab)},modifier=cardModifier.heightIn(min=108.dp).testTag("home_${tab.name}").semantics { role=Role.Button },
                shape=MaterialTheme.shapes.large,color=if(tab==AppTab.AGENDA)colors.primaryContainer else colors.secondaryContainer,
                contentColor=ink,border=BorderStroke(1.dp,colors.outlineVariant)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Icon(if(tab==AppTab.AGENDA)Icons.Rounded.DateRange else Icons.Rounded.Description,null,tint=ink)
                    Text(tab.title,style=MaterialTheme.typography.titleMedium)
                    Text(subtitle,style=MaterialTheme.typography.bodySmall,color=ink.copy(alpha=0.9f))
                }
            }
        }
        if(stacked) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"رویدادهای هفته و خروجی تقویم",Modifier.fillMaxWidth())
            card(AppTab.NOTES,"نکته‌ها و خلاصه‌های درسی",Modifier.fillMaxWidth())
        } else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"رویدادهای هفته و خروجی تقویم",Modifier.weight(1f))
            card(AppTab.NOTES,"نکته‌ها و خلاصه‌های درسی",Modifier.weight(1f))
        }
    }
}
