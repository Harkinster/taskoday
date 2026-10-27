package com.example.taskoday.core.ui.component.forms

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.taskoday.R
import com.example.taskoday.core.ui.theme.*
import com.example.taskoday.features.taskodayv4.FantasyFramedButton

internal val FormViolet = Color(0xFF542870)
internal val FormInk = Color(0xFF35213E)
internal val FormError = Color(0xFFA32936)
private val FormGold = Color(0xFFB68A3C)

/** Scoped to forms: the validated main screens retain their own theme. */
@Composable
fun FantasyFormTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        onDispose {
            // Navigation transitions can outlive the Compose text input session.
            // Finish dismissing the IME when this form has actually left composition.
            val token = view.windowToken
            val input = view.context.getSystemService(InputMethodManager::class.java)
            if (token != null) view.post { input?.hideSoftInputFromWindow(token, 0) }
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(
        primary = FormViolet, onPrimary = ParchmentLight,
        secondary = FormViolet, onSecondary = ParchmentLight,
        surface = ParchmentCream, onSurface = FormInk,
        surfaceVariant = Color(0xFFF0E3CE), onSurfaceVariant = InkMuted,
        background = RoyalPurpleDark, onBackground = ParchmentLight,
        outline = FormGold, error = FormError, onError = Color.White,
    ), content = content)
}

/** Quiet parchment for typing; decoration stays around the edges. */
@Composable
fun FantasyFormPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        color = ParchmentCream, contentColor = FormInk,
        border = BorderStroke(1.dp, FormGold), shadowElevation = 2.dp) {
        Column(Modifier.background(Brush.verticalGradient(listOf(ParchmentLight, ParchmentCream)))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            title?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✦", color = FormViolet, style = MaterialTheme.typography.labelMedium)
                    Text(it, Modifier.weight(1f).semantics { heading() }, color = FormInk,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(color = FormGold.copy(alpha = .4f))
            }
            content()
        }
    }
}

@Composable
fun FantasyFormHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth(), shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
        color = RoyalPurpleDark, contentColor = ParchmentLight, border = BorderStroke(1.dp, SoftGold)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour", tint = SoftGoldPale)
            } else Image(painterResource(R.drawable.v4_task_medallion), null, Modifier.size(40.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, Modifier.semantics { heading() }, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = SoftGoldPale, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FantasyFormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
) {
    OutlinedTextField(value, onValueChange, modifier, enabled = enabled,
        label = label, placeholder = placeholder, singleLine = singleLine,
        minLines = minLines, maxLines = maxLines, visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FormInk, unfocusedTextColor = FormInk,
            focusedContainerColor = ParchmentLight, unfocusedContainerColor = ParchmentLight,
            focusedBorderColor = FormViolet, unfocusedBorderColor = FormGold,
            focusedLabelColor = FormViolet, unfocusedLabelColor = FormInk,
            cursorColor = FormViolet,
        ))
}

@Composable
fun FantasyFormChoice(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    Surface(modifier.clip(shape).selectable(selected = selected, enabled = enabled, role = Role.Checkbox, onClick = onClick),
        shape = shape, color = if (selected) FormViolet else Color(0xFFF2E8D5),
        contentColor = if (selected) ParchmentLight else FormInk,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SoftGold else FormGold.copy(alpha = .65f))) {
        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = SoftGoldPale)
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
        }
    }
}

@Composable
fun FantasyFormOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = shape, border = BorderStroke(1.dp, FormGold),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FormViolet, containerColor = ParchmentLight),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), content = content)
}

@Composable
fun FantasyFormFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = FormViolet, contentColor = ParchmentLight),
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = shape, colors = colors, border = BorderStroke(1.dp, SoftGold),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), content = content)
}

/** Separate from the app-wide bottom bar so other validated flows do not change. */
@Composable
fun FantasyFormBottomBar(
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    backLabel: String = "Retour",
) {
    Surface(color = RoyalPurpleDark, contentColor = ParchmentLight, shadowElevation = 6.dp) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            HorizontalDivider(color = SoftGold.copy(alpha = .6f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FantasyFormOutlineButton(onBack, Modifier.weight(1f)) { Text(backLabel) }
                FantasyFramedButton(primaryLabel, onPrimary, Modifier.weight(1.65f), primaryEnabled)
            }
        }
    }
}
