package com.taskoday.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.taskoday.ui.theme.TaskodayColors
import com.taskoday.ui.theme.TaskodayDimens

/**
 * Remplace les ressources par tes vrais assets :
 * - R.drawable.taskoday_mascot_transparent
 * - R.drawable.taskoday_wordmark_transparent
 * - R.drawable.avatar_default
 */
@Composable
fun TaskodayHeader(
    modifier: Modifier = Modifier,
    mascotRes: Int,
    wordmarkRes: Int,
    avatarRes: Int? = null,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TaskodayDimens.HeaderHeight)
            .padding(horizontal = TaskodayDimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(mascotRes),
            contentDescription = "Logo Taskoday",
            modifier = Modifier.size(58.dp)
        )

        Image(
            painter = painterResource(wordmarkRes),
            contentDescription = "Taskoday",
            modifier = Modifier
                .height(42.dp)
                .padding(start = 8.dp)
                .weight(1f)
        )

        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = TaskodayColors.TextPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(TaskodayColors.Magenta)
                    .align(Alignment.TopEnd)
            )
        }

        Spacer(Modifier.size(12.dp))

        Box(
            modifier = Modifier
                .size(TaskodayDimens.AvatarSize)
                .neonGlow(TaskodayColors.NeonPurple.copy(alpha = 0.55f), 18.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(TaskodayColors.Cyan, TaskodayColors.NeonPurple)),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(TaskodayColors.PanelAlt),
            contentAlignment = Alignment.Center
        ) {
            if (avatarRes != null) {
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(TaskodayDimens.AvatarSize)
                )
            } else {
                Text("A", color = TaskodayColors.TextPrimary)
            }
        }
    }
}
