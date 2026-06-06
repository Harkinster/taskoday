package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Immutable
data class FantasySkinAsset(
    val resourceName: String,
)

object FantasySkinAssets {
    val headerWood = FantasySkinAsset("ui_header_wood")
    val panelParchment = FantasySkinAsset("ui_panel_parchment")
    val panelPurple = FantasySkinAsset("ui_panel_purple")
    val panelQuestEpic = FantasySkinAsset("ui_panel_quest_epic")
    val buttonGold = FantasySkinAsset("ui_button_gold")
    val buttonPurple = FantasySkinAsset("ui_button_purple")
    val navBarWood = FantasySkinAsset("ui_nav_bar_wood")
    val navTabActive = FantasySkinAsset("ui_nav_tab_active")
    val currencyFlameche = FantasySkinAsset("ui_currency_flameche")
    val currencyCrystal = FantasySkinAsset("ui_currency_crystal")
    val iconFrameGold = FantasySkinAsset("ui_icon_frame_gold")
    val bestiaryRow = FantasySkinAsset("ui_bestiary_row")
    val fabGold = FantasySkinAsset("ui_fab_gold")
    val badgeActive = FantasySkinAsset("ui_badge_active")
    val badgeLocked = FantasySkinAsset("ui_badge_locked")
}

@Composable
fun FantasySkinAsset.resourceIdOrNull(): Int? {
    val context = LocalContext.current
    return remember(context, resourceName) {
        context.resources
            .getIdentifier(resourceName, "drawable", context.packageName)
            .takeIf { id -> id != 0 }
    }
}

@Composable
fun FantasySkinSurface(
    asset: FantasySkinAsset,
    fallbackBrush: Brush,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    fallbackColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val assetResId = asset.resourceIdOrNull()
    Box(
        modifier =
            modifier
                .background(fallbackColor)
                .background(fallbackBrush),
    ) {
        if (assetResId != null) {
            Image(
                painter = painterResource(id = assetResId),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
            )
        }
        content()
    }
}
