package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskoday.R

val JourneyGold = Color(0xFFE4BB77)
val JourneyParchment = Color(0xFFF1DEB9)
val JourneyInk = Color(0xFF38263D)
val JourneyMuted = Color(0xFF705C68)
// Text on parchment needs a darker red than the glow used against dark backgrounds.
val JourneyDangerInk = Color(0xFF8C3835)

/** Profile entry, deliberately distinct from member initials used to identify assignees. */
@Composable
fun FantasyProfileMedallion(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "Ouvrir mon profil" }, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.v4_task_medallion), null, Modifier.fillMaxSize())
        Icon(Icons.Outlined.Person, null, Modifier.size(24.dp), tint = JourneyParchment)
    }
}

@Composable
fun FantasyPageHeader(title: String, subtitle: String, onOpenProfile: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FantasyCompactHeader(title = title, trailingContent = {
            FantasyProfileMedallion(onOpenProfile, Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
        })
        Text(subtitle, color = JourneyParchment, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
fun FantasyTrailTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FantasyFramedTab(label,selected,onClick,modifier)
}

@Composable
fun FantasySectionHeading(title: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✦", color = JourneyGold, style = MaterialTheme.typography.labelSmall)
        Text(title, Modifier.weight(1f).semantics { heading() }, color = JourneyParchment, fontFamily = FontFamily.Serif,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        detail?.let { Text(it, color = JourneyGold, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
fun FantasyParchmentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    FantasyTaskCardBackground(modifier.fillMaxWidth(), contentStartPadding = 18.dp, verticalContentPadding = 16.dp) {
        CompositionLocalProvider(LocalContentColor provides JourneyInk) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}
