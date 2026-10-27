package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskoday.R
import kotlin.math.roundToInt

/** Comparison only. No preference is persisted; release and ordinary launches keep Current. */
enum class HomeHeaderStudyVariant {
    Current, RefinedWood, Parchment;
    companion object {
        fun fromRequest(qaDebug: Boolean,request: String?): HomeHeaderStudyVariant =
            if(!qaDebug) Current else when(request) {
                "wood-refined" -> RefinedWood
                "parchment" -> Parchment
                else -> Current
            }
    }
}

val LocalHomeHeaderStudyVariant=staticCompositionLocalOf {HomeHeaderStudyVariant.Current}

@Composable
internal fun HomeHeaderStudy(
    variant: HomeHeaderStudyVariant,
    title: String,
    subtitle: String?,
    modifier: Modifier,
    trailingContent: @Composable BoxScope.() -> Unit,
) {
    val paper=variant==HomeHeaderStudyVariant.Parchment
    val texture=ImageBitmap.imageResource(if(paper) R.drawable.ui_panel_parchment else R.drawable.v4_header_compact)
    Box(modifier.fillMaxWidth().heightIn(min=64.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val inset=4.dp.toPx();val notch=10.dp.toPx()
            fun outline(pad: Float)=homePlaqueOutline(size.width,size.height,inset,notch,pad)
            val shape=outline(0f)
            clipPath(shape) {
                drawImage(texture,
                    srcOffset=if(paper) IntOffset(70,100) else IntOffset(250,70),
                    srcSize=if(paper) IntSize(260,125) else IntSize(638,95),
                    dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),filterQuality=FilterQuality.High)
                drawRect(if(paper) Color(0x18FFF7DF) else Color(0x60342530))
            }
            drawPath(shape,if(paper) Color(0xFF76518C) else Color(0xFFB39461),style=Stroke(1.4.dp.toPx()))
            drawPath(outline(3.dp.toPx()),JourneyGold.copy(alpha=if(paper) .85f else .42f),style=Stroke(.7.dp.toPx()))
            // Small engraved corner marks reuse the plaque's geometry, with no large crest.
            for(x in listOf(inset+notch,size.width-inset-notch)) {
                val gem=Path().apply {
                    moveTo(x,7.dp.toPx());lineTo(x+3.dp.toPx(),10.dp.toPx())
                    lineTo(x,13.dp.toPx());lineTo(x-3.dp.toPx(),10.dp.toPx());close()
                }
                drawPath(gem,if(paper) Color(0xFF76518C) else JourneyGold.copy(alpha=.65f))
            }
        }
        Column(Modifier.padding(horizontal=66.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("TASKODAY",color=if(paper) Color(0xFF76518C) else JourneyGold,
                fontSize=9.sp,lineHeight=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
            Text(title,Modifier.semantics {heading()},color=if(paper) JourneyInk else JourneyParchment,
                fontSize=22.sp,lineHeight=27.sp,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            subtitle?.takeIf(String::isNotBlank)?.let {Text(it,color=if(paper) JourneyMuted else JourneyParchment,fontSize=11.sp)}
        }
        trailingContent()
    }
}
