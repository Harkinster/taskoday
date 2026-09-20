package com.example.taskoday.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Actions kept reachable while a long form is scrolled or the keyboard is open. */
@Composable
fun FormBottomBar(
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    backLabel: String = "Retour",
) {
    Surface(shadowElevation = 6.dp, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(backLabel) }
            Button(onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.weight(1f)) {
                Text(primaryLabel)
            }
        }
    }
}
