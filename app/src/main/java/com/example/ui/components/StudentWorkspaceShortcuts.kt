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
import com.example.ui.theme.studentStickerColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

@Composable
fun StudentWorkspaceShortcuts(onNavigate:(AppTab)->Unit,modifier:Modifier=Modifier,showLibrary:Boolean=false) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked=maxWidth<300.dp || LocalDensity.current.fontScale>=1.3f
        val card: @Composable (AppTab,String,Modifier)->Unit={tab,subtitle,cardModifier->
            val colors=MaterialTheme.colorScheme
            val palette=studentStickerColors(studentDestinationSticker(tab))
            val ink=palette.ink
            Surface(onClick={onNavigate(tab)},modifier=cardModifier.heightIn(min=64.dp).testTag("home_${tab.name}").semantics { role=Role.Button },
                shape=MaterialTheme.shapes.large,color=palette.container,
                contentColor=ink,border=BorderStroke(1.dp,ink.copy(alpha=0.15f))) {
                Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    AppEmoji(studentDestinationSticker(tab),size=28.dp)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(if(tab==AppTab.AGENDA) "برنامهٔ هفته" else if(tab==AppTab.LIBRARY) "کتابخانه" else "یادداشت‌ها",style=MaterialTheme.typography.titleSmall)
                        Text(subtitle,style=MaterialTheme.typography.bodySmall,color=ink)
                    }
                }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {

        if(stacked) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"کلاس‌ها و کارهای هفته",Modifier.fillMaxWidth())
            card(AppTab.NOTES,"خلاصه‌های هر درس",Modifier.fillMaxWidth())
        } else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            card(AppTab.AGENDA,"کلاس‌ها و کارهای هفته",Modifier.weight(1f))
            card(AppTab.NOTES,"خلاصه‌های هر درس",Modifier.weight(1f))
        }
        if(showLibrary) card(AppTab.LIBRARY,"جزوه‌ها و ادامهٔ مطالعه",Modifier.fillMaxWidth())
        }
    }
}
