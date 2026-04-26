package com.taskoday.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import com.taskoday.ui.theme.TaskodayColors
import com.taskoday.ui.theme.TaskodayDimens

val NeonBorderBrush = Brush.linearGradient(
    colors = listOf(
        TaskodayColors.Magenta,
        TaskodayColors.NeonPurple,
        TaskodayColors.Cyan
    )
)

val PanelBrush = Brush.verticalGradient(
    colors = listOf(
        TaskodayColors.PanelAlt.copy(alpha = 0.92f),
        TaskodayColors.PanelDark.copy(alpha = 0.96f)
    )
)

val BackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        TaskodayColors.Night,
        TaskodayColors.DeepSpace,
        TaskodayColors.RoyalBlue
    )
)

val ProgressBrush = Brush.horizontalGradient(
    colors = listOf(
        TaskodayColors.NeonPurple,
        TaskodayColors.Cyan
    )
)

/**
 * Carte principale du skin Taskoday.
 * À utiliser pour les hero cards, sections de routine, cartes mission, cartes quête et profil.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = TaskodayDimens.CardRadius,
    borderWidth: Dp = TaskodayDimens.CardBorder,
    padding: PaddingValues = PaddingValues(TaskodayDimens.CardPadding),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .neonGlow()
            .background(PanelBrush, shape)
            .border(BorderStroke(borderWidth, NeonBorderBrush), shape)
            .padding(padding),
        content = content
    )
}

/**
 * Glow léger. Compose ne gère pas nativement le vrai blur comme Figma,
 * mais cette couche donne une base visuelle proche du skin.
 */
fun Modifier.neonGlow(
    color: Color = TaskodayColors.Cyan.copy(alpha = 0.28f),
    radius: Dp = 12.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().setShadowLayer(
                radius.toPx(),
                0f,
                0f,
                color.copy(alpha = 0.45f).toArgb()
            )
        }
        canvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            radius.toPx(),
            radius.toPx(),
            paint
        )
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
