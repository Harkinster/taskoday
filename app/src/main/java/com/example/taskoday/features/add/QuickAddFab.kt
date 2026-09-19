package com.example.taskoday.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.ParchmentLight

@Composable
fun QuickAddFab(
    uiState: QuickAddUiState,
    onRefresh: () -> Unit,
    onCreateTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(MagicViolet)
            .clickable(enabled = uiState.canOpenQuickAdd) {
                onRefresh()
                onCreateTask()
            }
            .border(1.dp, ParchmentLight.copy(alpha = 0.22f), CircleShape)
            .testTag(TaskodayTestTags.TasksAddFab),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Ajouter une tâche",
            modifier = Modifier.size(18.dp),
            tint = ParchmentLight,
        )
    }
}
