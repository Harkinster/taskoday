package com.example.taskoday.core.ui.component

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.RoyalPurpleDark

/** Explicit contrast for selectors placed directly on the aubergine canvas. */
@Composable
fun DarkSurfaceFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = ParchmentCream,
            selectedContainerColor = RoyalPurpleDark,
            selectedLabelColor = ParchmentLight,
        ),
    )
}
