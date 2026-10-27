package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.taskoday.R
import kotlin.math.roundToInt

private val FantasyInk = Color(0xFF21162F)
private val FantasyGold = Color(0xFFE4BB77)
private val FantasyParchment = Color(0xFFF1DEB9)
private val FantasyLavender = Color(0xFF4C2B60)

internal data class SliceGridSpec(
    val width: Int,
    val height: Int,
    val xCuts: List<Int>,
    val yCuts: List<Int>,
    val stretchedColumns: Set<Int>,
    val stretchedRows: Set<Int>,
) {
    val columns: Int get() = xCuts.size - 1
    val rows: Int get() = yCuts.size - 1
}

internal data class AxisSliceSpan(
    val sourceStart: Int,
    val sourceEnd: Int,
    val destinationStart: Int,
    val destinationEnd: Int,
) {
    val sourceLength: Int get() = sourceEnd - sourceStart
    val destinationLength: Int get() = destinationEnd - destinationStart
}

internal fun calculateAxisSlices(
    cuts: List<Int>,
    targetLength: Int,
    baseScale: Float,
    stretchedBands: Set<Int>,
): List<AxisSliceSpan> {
    require(cuts.size >= 2 && cuts.first() == 0 && cuts.zipWithNext().all { it.first < it.second })
    require(targetLength > 0 && baseScale > 0f)
    val sourceLength = cuts.last()
    val extra = targetLength - sourceLength * baseScale
    require(extra >= -1.0f) { "Target smaller than the uniformly scaled fixed geometry" }
    if (extra > 1.0f) require(stretchedBands.isNotEmpty()) { "No authorized band can absorb extension" }
    val stretchWeight = stretchedBands.sumOf { cuts[it + 1] - cuts[it] }.toFloat()
    require(stretchWeight > 0f || extra <= 1.0f)
    var cursor = 0
    return (0 until cuts.lastIndex).map { index ->
        val sourceStart = cuts[index]
        val sourceEnd = cuts[index + 1]
        val sourceBand = sourceEnd - sourceStart
        val allocated = if (index in stretchedBands && stretchWeight > 0f) {
            extra.coerceAtLeast(0f) * sourceBand / stretchWeight
        } else 0f
        val idealEnd = cursor + sourceBand * baseScale + allocated
        val end = if (index == cuts.lastIndex - 1) targetLength else idealEnd.roundToInt()
        val span = AxisSliceSpan(sourceStart, sourceEnd, cursor, end)
        cursor = end
        span
    }
}

private val CardGrid = SliceGridSpec(
    width = 1386,
    height = 462,
    xCuts = listOf(0, 215, 1171, 1386),
    yCuts = listOf(0, 127, 183, 280, 335, 462),
    stretchedColumns = setOf(1),
    stretchedRows = setOf(1, 3),
)

private val SummaryGrid = SliceGridSpec(
    width = 2115,
    height = 630,
    xCuts = listOf(0, 376, 808, 1307, 1739, 2115),
    yCuts = listOf(0, 170, 432, 630),
    stretchedColumns = setOf(1, 3),
    stretchedRows = setOf(1),
)

private val CardSliceIds = listOf(
    R.drawable.v4_card_r0_c0, R.drawable.v4_card_r0_c1, R.drawable.v4_card_r0_c2,
    R.drawable.v4_card_r1_c0, R.drawable.v4_card_r1_c1, R.drawable.v4_card_r1_c2,
    R.drawable.v4_card_r2_c0, R.drawable.v4_card_r2_c1, R.drawable.v4_card_r2_c2,
    R.drawable.v4_card_r3_c0, R.drawable.v4_card_r3_c1, R.drawable.v4_card_r3_c2,
    R.drawable.v4_card_r4_c0, R.drawable.v4_card_r4_c1, R.drawable.v4_card_r4_c2,
)

private val SummarySliceIds = listOf(
    R.drawable.v4_summary_r0_c0, R.drawable.v4_summary_r0_c1, R.drawable.v4_summary_r0_c2,
    R.drawable.v4_summary_r0_c3, R.drawable.v4_summary_r0_c4,
    R.drawable.v4_summary_r1_c0, R.drawable.v4_summary_r1_c1, R.drawable.v4_summary_r1_c2,
    R.drawable.v4_summary_r1_c3, R.drawable.v4_summary_r1_c4,
    R.drawable.v4_summary_r2_c0, R.drawable.v4_summary_r2_c1, R.drawable.v4_summary_r2_c2,
    R.drawable.v4_summary_r2_c3, R.drawable.v4_summary_r2_c4,
)

@Composable
private fun FantasyMultiSlicePanel(
    spec: SliceGridSpec,
    sliceIds: List<Int>,
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable BoxScope.() -> Unit,
) {
    require(sliceIds.size == spec.rows * spec.columns)
    val resources = LocalContext.current.resources
    val slices = remember(sliceIds, resources) { sliceIds.map { ImageBitmap.imageResource(resources, it) } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val minHeight = maxWidth * (spec.height.toFloat() / spec.width)
        Box(Modifier.fillMaxWidth().heightIn(min = minHeight)) {
            Canvas(Modifier.matchParentSize()) {
                drawMultiSlice(spec, slices)
            }
            Box(Modifier.fillMaxWidth().padding(contentPadding), content = content)
        }
    }
}

private fun DrawScope.drawMultiSlice(spec: SliceGridSpec, slices: List<ImageBitmap>) {
    val baseScale = size.width / spec.width.toFloat()
    val x = calculateAxisSlices(spec.xCuts, size.width.roundToInt(), baseScale, spec.stretchedColumns)
    val y = calculateAxisSlices(spec.yCuts, size.height.roundToInt(), baseScale, spec.stretchedRows)
    for (row in 0 until spec.rows) {
        for (column in 0 until spec.columns) {
            val source = slices[row * spec.columns + column]
            val horizontal = x[column]
            val vertical = y[row]
            drawImage(
                image = source,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(source.width, source.height),
                dstOffset = IntOffset(horizontal.destinationStart, vertical.destinationStart),
                dstSize = IntSize(horizontal.destinationLength, vertical.destinationLength),
                filterQuality = FilterQuality.High,
            )
        }
    }
}

@Composable
fun FantasyTaskCardBackground(
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = 80.dp,
    verticalContentPadding: Dp = 13.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    FantasyMultiSlicePanel(
        spec = CardGrid,
        sliceIds = CardSliceIds,
        modifier = modifier,
        contentPadding = PaddingValues(start = contentStartPadding, end = 18.dp, top = verticalContentPadding, bottom = verticalContentPadding),
        content = content,
    )
}

@Composable
fun FantasySummaryPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    FantasyMultiSlicePanel(
        spec = SummaryGrid,
        sliceIds = SummarySliceIds,
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 16.dp),
        content = content,
    )
}

@Composable
fun FantasyTaskMedallion(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    medallionSize: Dp = 54.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier.size(medallionSize).semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.v4_task_medallion),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun FantasyCompactHeader(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    title: String = "Ma Maison",
    trailingContent: @Composable BoxScope.() -> Unit = {},
) {
    val study=LocalHomeHeaderStudyVariant.current
    if(title=="Ma Maison") {
        if(study==HomeHeaderStudyVariant.Current) FantasyHybridHomeHeader(title,subtitle,modifier,trailingContent)
        else HomeHeaderStudy(study,title,subtitle,modifier,trailingContent)
        return
    }
    // Keep enlarged text inside the carved title area without reducing the user's font scale.
    val enlargedText = LocalDensity.current.fontScale > 1.15f
    Box(modifier.fillMaxWidth().aspectRatio(1138f / 250f), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.v4_header_compact),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("TASKODAY", color = FantasyGold, fontSize = 10.sp, lineHeight = if (enlargedText) 12.sp else TextUnit.Unspecified, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(title, modifier = Modifier.semantics { heading() }, color = FantasyParchment, fontSize = 23.sp, lineHeight = if (enlargedText) 26.sp else TextUnit.Unspecified, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            subtitle?.takeIf(String::isNotBlank)?.let { Text(it, color = FantasyParchment, fontSize = 11.sp, lineHeight = if (enlargedText) 14.sp else TextUnit.Unspecified, maxLines = 1) }
        }
        trailingContent()
    }
}

@Composable
fun FantasyPurpleButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .widthIn(min = 150.dp)
            .height(52.dp)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painterResource(R.drawable.v4_purple_button), contentDescription = null, modifier = Modifier.fillMaxSize())
        Text(label, color = FantasyParchment, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}



@Composable
fun FantasyHomeNavigationBar(
    destinations: List<com.example.taskoday.navigation.TaskodayDestination>,
    currentRoute: String?,
    quickAddEnabled: Boolean,
    onNavigate: (com.example.taskoday.navigation.TaskodayDestination) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val bottomInset = with(density) { androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(this).toDp() }
    val entries = destinations.mapNotNull { destination ->
        val icon = when (destination.route) {
            com.example.taskoday.navigation.TaskodayDestination.FamilyHome.route -> R.drawable.v4_nav_home
            com.example.taskoday.navigation.TaskodayDestination.Exploration.route -> R.drawable.v4_nav_exploration
            com.example.taskoday.navigation.TaskodayDestination.FollowUp.route -> R.drawable.v4_nav_followup
            com.example.taskoday.navigation.TaskodayDestination.Nest.route -> R.drawable.v4_nav_nest
            else -> null
        }
        icon?.let { Triple(destination, it, destination.route == currentRoute) }
    }
    val bar = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.v4_navigation_bar) }
    Box(modifier.fillMaxWidth().height(78.dp + bottomInset)) {
        Canvas(Modifier.fillMaxWidth().height(78.dp).align(Alignment.TopCenter)) {
            drawImage(bar, IntOffset(94, 169), IntSize(1985, 386), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.High)
        }
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            entries.take(2).forEach { (destination, icon, selected) ->
                FantasyNavDestination(fantasyDestinationLabel(destination), icon, selected, Modifier.weight(1f)) { onNavigate(destination) }
            }
            if (quickAddEnabled) Spacer(Modifier.weight(1f))
            entries.drop(2).forEach { (destination, icon, selected) ->
                FantasyNavDestination(fantasyDestinationLabel(destination), icon, selected, Modifier.weight(1f)) { onNavigate(destination) }
            }
        }
        if (quickAddEnabled) Image(
            painter = painterResource(R.drawable.v4_plus_medallion),
            contentDescription = if (quickAddEnabled) "Cr\u00e9er une action" else "Cr\u00e9ation d'action indisponible",
            modifier = Modifier.size(54.dp).align(Alignment.TopCenter).alpha(if (quickAddEnabled) 1f else 0.48f).clickable(enabled = quickAddEnabled, role = Role.Button, onClick = onCreate)
                .semantics { contentDescription = if (quickAddEnabled) "Cr\u00e9er une action" else "Cr\u00e9ation indisponible" },
        )
    }
}

private fun fantasyDestinationLabel(destination: com.example.taskoday.navigation.TaskodayDestination): String = when (destination.route) {
    com.example.taskoday.navigation.TaskodayDestination.FamilyHome.route -> "Maison"
    com.example.taskoday.navigation.TaskodayDestination.Nest.route -> "Le Nid"
    else -> destination.label
}

@Composable
private fun FantasyNavDestination(label: String, iconRes: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.heightIn(min = 64.dp).selectable(selected, role = Role.Tab, onClick = onClick).semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(if (selected) 30.dp else 26.dp), alpha = if (selected) 1f else 0.82f)
        Text(label, color = if (selected) FantasyGold else FantasyParchment, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1)
    }
}
