package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.taskoday.R
import kotlin.math.roundToInt

/** Reuses the illustrated bevel; only its quiet middle stretches on a wide or tall control. */
@Composable
private fun CarvedControl(
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    selected: Boolean?,
    onClick: () -> Unit,
) {
    val bitmap=ImageBitmap.imageResource(R.drawable.v4_purple_button)
    val interaction=if(selected==null) Modifier.clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        else Modifier.selectable(selected=selected,enabled=enabled,role=Role.Tab,onClick=onClick)
    Box(modifier.heightIn(min=48.dp).then(interaction).semantics {contentDescription=label},
        contentAlignment=Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val cap=64
            val capWidth=(cap*size.height/bitmap.height).coerceAtMost(size.width/2).roundToInt()
            val width=size.width.roundToInt();val height=size.height.roundToInt()
            val alpha=if(!enabled) .4f else if(selected==false) .67f else 1f
            val regions=listOf(
                Triple(0,cap,0 to capWidth),
                Triple(cap,bitmap.width-cap,capWidth to (width-capWidth)),
                Triple(bitmap.width-cap,bitmap.width,(width-capWidth) to width),
            )
            for((start,end,destination) in regions) {
                if(destination.second>destination.first) drawImage(bitmap,
                    srcOffset=IntOffset(start,0),srcSize=IntSize(end-start,bitmap.height),
                    dstOffset=IntOffset(destination.first,0),dstSize=IntSize(destination.second-destination.first,height),
                    alpha=alpha,filterQuality=FilterQuality.High)
            }
        }
        Text(label,Modifier.padding(horizontal=28.dp,vertical=12.dp),
            color=if(selected==true) JourneyGold else JourneyParchment,
            style=MaterialTheme.typography.labelLarge,
            fontWeight=if(selected==true) FontWeight.Bold else FontWeight.SemiBold,
            textAlign=TextAlign.Center)
    }
}

@Composable
fun FantasyFramedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier=Modifier,
    enabled: Boolean=true,
) = CarvedControl(label,modifier,enabled,null,onClick)

@Composable
internal fun FantasyFramedTab(label: String,selected: Boolean,onClick: () -> Unit,modifier: Modifier=Modifier) =
    CarvedControl(label,modifier,true,selected,onClick)
