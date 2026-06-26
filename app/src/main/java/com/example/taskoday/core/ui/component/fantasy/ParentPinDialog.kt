package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun ParentPinDialog(
    title: String = "PIN parent",
    message: String = "Saisis le PIN parent a 4 chiffres.",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            tone = NeonTone.Cyan,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = StarWhite,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { value -> pin = value.filter(Char::isDigit).take(PARENT_PIN_LENGTH) },
                label = { Text("PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                NeonButton(
                    text = "Annuler",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.Outline,
                )
                NeonButton(
                    text = "Valider",
                    onClick = { onConfirm(pin) },
                    modifier = Modifier.weight(1f),
                    enabled = pin.length == PARENT_PIN_LENGTH,
                )
            }
        }
    }
}

@Composable
fun ParentPinStatusMessage(
    hasParentPin: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text =
            if (hasParentPin) {
                "PIN parent defini sur cet appareil."
            } else {
                "Aucun PIN parent defini. Definis un PIN avant le mode enfant."
            },
        style = MaterialTheme.typography.bodySmall,
        color = if (hasParentPin) NeonCyan else MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

const val PARENT_PIN_LENGTH: Int = 4
