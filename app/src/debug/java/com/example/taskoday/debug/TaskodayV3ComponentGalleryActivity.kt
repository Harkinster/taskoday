package com.example.taskoday.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3ActionKind
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3ActionTypeBadge
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3FamilyAvatar
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3InfoBlock
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3PrimaryButton
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3SelectableChip
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3SecondaryButton
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3StatusBadge
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TaskCard
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TaskStatus
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TextField
import com.example.taskoday.core.ui.theme.TaskodayV3UtilityTheme
import com.example.taskoday.core.ui.theme.taskodayV3Palette

/** Build-variant-only visual harness. It has no route from the production app. */
class TaskodayV3ComponentGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskodayV3UtilityTheme { ComponentGallery() } }
    }
}

@Composable
private fun ComponentGallery() {
    val palette = MaterialTheme.taskodayV3Palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Taskoday V3 · Galerie", style = MaterialTheme.typography.headlineMedium, color = palette.textPrimary)
        Text("Aperçu DEBUG · données fictives", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskodayV3PrimaryButton("Créer une action", {})
            TaskodayV3SecondaryButton("Secondaire", {})
        }
        TaskodayV3TextField("Mettre la table", {}, label = "Titre de l'action")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskodayV3SelectableChip("Aujourd'hui", selected = true, onClick = {})
            TaskodayV3SelectableChip("Demain", selected = false, onClick = {})
        }
        Text("Types et statuts", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskodayV3ActionTypeBadge("Routine", TaskodayV3ActionKind.ROUTINE)
            TaskodayV3ActionTypeBadge("Mission", TaskodayV3ActionKind.MISSION)
            TaskodayV3ActionTypeBadge("Quête", TaskodayV3ActionKind.QUEST)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskodayV3StatusBadge("En cours", TaskodayV3TaskStatus.IN_PROGRESS)
            TaskodayV3StatusBadge("À valider", TaskodayV3TaskStatus.PENDING_VALIDATION)
            TaskodayV3StatusBadge("En retard", TaskodayV3TaskStatus.OVERDUE)
        }
        TaskodayV3TaskCard(
            title = "Mettre la table",
            typeLabel = "Mission",
            kind = TaskodayV3ActionKind.MISSION,
            statusLabel = "À valider",
            status = TaskodayV3TaskStatus.PENDING_VALIDATION,
            supportingText = "Prévu pour : Léa · Aujourd'hui, 19 h",
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskodayV3FamilyAvatar("L", "Léa", Color(0xFFD9EEF4))
            TaskodayV3FamilyAvatar("M", "Mila", Color(0xFFEAE3FF))
            TaskodayV3FamilyAvatar("P", "Parent", Color(0xFFF8D8C6))
        }
        TaskodayV3InfoBlock("Petit rappel", "Les choix de cette galerie ne sont pas enregistrés.")
    }
}
