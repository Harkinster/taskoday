package com.taskoday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.taskoday.ui.theme.TaskodayColors
import com.taskoday.ui.theme.TaskodayDimens

data class TaskodayNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val DefaultTaskodayNavItems = listOf(
    TaskodayNavItem("accueil", "Accueil", Icons.Outlined.Home),
    TaskodayNavItem("missions", "Missions", Icons.Outlined.Checklist),
    TaskodayNavItem("quetes", "Quêtes", Icons.Outlined.Star),
    TaskodayNavItem("recompenses", "Récompenses", Icons.Outlined.EmojiEvents),
    TaskodayNavItem("profil", "Profil", Icons.Outlined.Person)
)

@Composable
fun FantasyBottomNavigation(
    currentRoute: String,
    items: List<TaskodayNavItem> = DefaultTaskodayNavItems,
    onNavigate: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskodayDimens.BottomNavHeight)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(TaskodayColors.Panel.copy(alpha = 0.85f), TaskodayColors.DeepSpace)
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item.route == currentRoute
            val color = if (selected) TaskodayColors.Cyan else TaskodayColors.NavInactive

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = color,
                    modifier = Modifier
                        .size(if (selected) 34.dp else 30.dp)
                        .then(if (selected) Modifier.neonGlow(TaskodayColors.Cyan.copy(alpha = 0.55f), 10.dp) else Modifier)
                )
                Text(
                    text = item.label,
                    color = color,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
