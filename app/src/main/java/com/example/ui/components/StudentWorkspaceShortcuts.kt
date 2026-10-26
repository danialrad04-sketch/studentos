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
        val stacked=maxWidth<300.dp || LocalDensity.current.fontScale>=1.3f
        val card: @Composable (AppTab,String,Modifier)->Unit={tab,subtitle,cardModifier->
            val colors=MaterialTheme.colorScheme
            val ink=colors.onSurface
            Surface(onClick={onNavigate(tab)},modifier=cardModifier.heightIn(min=108.dp).testTag("home_${tab.name}").semantics { role=Role.Button },
                shape=MaterialTheme.shapes.large,color=colors.surface,
                contentColor=ink,border=BorderStroke(1.dp,colors.outlineVariant)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    AppEmoji(if(tab==AppTab.AGENDA)AppEmojiType.CALENDAR else AppEmojiType.BOOK, size=36.dp)
                    Text(if(tab==AppTab.AGENDA)"برنامهٔ هفته"else"یادداشت‌ها",style=MaterialTheme.typography.titleSmall)
                    Text(subtitle,style=MaterialTheme.typography.bodySmall,color=ink.copy(alpha=0.9f))
                }
            }
        }
        if(stacked) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"کلاس‌ها و کارهای هفته",Modifier.fillMaxWidth())
            card(AppTab.NOTES,"خلاصه‌های هر درس",Modifier.fillMaxWidth())
        } else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"کلاس‌ها و کارهای هفته",Modifier.weight(1f))
            card(AppTab.NOTES,"خلاصه‌های هر درس",Modifier.weight(1f))
        }
    }
}
