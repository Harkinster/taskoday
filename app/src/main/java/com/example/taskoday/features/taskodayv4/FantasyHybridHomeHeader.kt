package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
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

/** The approved A silhouette, shared by the study reference and the final Maison plaque. */
internal fun homePlaqueOutline(width:Float,height:Float,inset:Float,notch:Float,pad:Float=0f)=Path().apply {
    moveTo(inset+notch+pad,inset+pad);lineTo(width-inset-notch-pad,inset+pad)
    lineTo(width-inset-pad,inset+notch+pad);lineTo(width-inset-pad,height-inset-notch-pad)
    lineTo(width-inset-notch-pad,height-inset-pad);lineTo(inset+notch+pad,height-inset-pad)
    lineTo(inset+pad,height-inset-notch-pad);lineTo(inset+pad,inset+notch+pad);close()
}

/** A's compact proportions, with the existing Taskoday wood, purple channel, gilding and foliage. */
@Composable
internal fun FantasyHybridHomeHeader(
    title:String,
    subtitle:String?,
    modifier:Modifier,
    trailingContent:@Composable BoxScope.()->Unit,
) {
    val artwork=ImageBitmap.imageResource(R.drawable.v4_header_compact)
    val crest=ImageBitmap.imageResource(R.drawable.v4_top_ornament)
    Box(modifier.fillMaxWidth().heightIn(min=64.dp).testTag("home-header-hybrid"),contentAlignment=Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            fun outline(pad:Float=0f)=homePlaqueOutline(size.width,size.height,4.dp.toPx(),10.dp.toPx(),pad)
            val shape=outline()
            fun ornament(x:Int,y:Int,w:Int,h:Int,left:Float,top:Float,width:Float,height:Float) {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(left,top,left+width,top+height),Paint())
                    drawImage(artwork,srcOffset=IntOffset(x,y),srcSize=IntSize(w,h),
                        dstOffset=IntOffset(left.roundToInt(),top.roundToInt()),
                        dstSize=IntSize(width.roundToInt(),height.roundToInt()),filterQuality=FilterQuality.High)
                    // Feather the inlays into the plaque, including their underlying wood.
                    drawRect(Brush.horizontalGradient(0f to Color.Transparent,.14f to Color.White,.75f to Color.White,1f to Color.Transparent,
                        startX=left,endX=left+width),Offset(left,top),Size(width,height),blendMode=BlendMode.DstIn)
                    drawRect(Brush.verticalGradient(0f to Color.Transparent,.18f to Color.White,.5f to Color.White,1f to Color.Transparent,
                        startY=top,endY=top+height),Offset(left,top),Size(width,height),blendMode=BlendMode.DstIn)
                    canvas.restore()
                }
            }
            clipPath(shape) {
                drawImage(artwork,srcOffset=IntOffset(250,70),srcSize=IntSize(638,95),
                    dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),filterQuality=FilterQuality.High)
                // Bevels and the violet inlay stay inside A's straight, chamfered outline.
                drawPath(shape,Brush.linearGradient(listOf(Color(0xFFFFE7A0),Color(0xFFB17A31),Color(0xFFE3BA69))),style=Stroke(3.dp.toPx()))
                drawPath(outline(2.5.dp.toPx()),Color(0xFF54245F),style=Stroke(3.dp.toPx()))
                drawPath(outline(4.3.dp.toPx()),JourneyGold,style=Stroke(.9.dp.toPx()))
                // Native fragments of the current plaque, without its old rounded silhouette.
                ornament(40,0,300,68,6.dp.toPx(),3.dp.toPx(),80.dp.toPx(),(80f*68/300).dp.toPx())
                ornament(798,0,300,68,size.width-86.dp.toPx(),3.dp.toPx(),80.dp.toPx(),(80f*68/300).dp.toPx())
                ornament(65,157,295,70,8.dp.toPx(),size.height-20.dp.toPx(),72.dp.toPx(),(72f*70/295).dp.toPx())
                ornament(778,157,295,70,size.width-80.dp.toPx(),size.height-20.dp.toPx(),72.dp.toPx(),(72f*70/295).dp.toPx())
                val crestWidth=48.dp.toPx();val crestHeight=crestWidth*crest.height/crest.width
                drawImage(crest,dstOffset=IntOffset(14.dp.roundToPx(),((size.height-crestHeight)/2).roundToInt()),
                    dstSize=IntSize(crestWidth.roundToInt(),crestHeight.roundToInt()),filterQuality=FilterQuality.High)
            }
        }
        Column(Modifier.padding(start=66.dp,end=66.dp,top=12.dp,bottom=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("TASKODAY",color=JourneyGold,fontSize=9.sp,lineHeight=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
            Text(title,Modifier.semantics {heading()},color=JourneyParchment,fontSize=22.sp,lineHeight=27.sp,
                fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            subtitle?.takeIf(String::isNotBlank)?.let {Text(it,color=JourneyParchment,fontSize=11.sp)}
        }
        trailingContent()
    }
}
