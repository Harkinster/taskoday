package com.example.taskoday.features.taskodayv4

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.taskoday.R
import com.example.taskoday.features.taskodayv4.FantasyCompactHeader
import com.example.taskoday.features.taskodayv4.FantasyPurpleButton
import com.example.taskoday.features.taskodayv4.FantasySummaryPanel
import com.example.taskoday.features.taskodayv4.FantasyTaskCardBackground
import com.example.taskoday.features.taskodayv4.FantasyTaskMedallion
import kotlin.math.roundToInt

private val FantasyInk = Color(0xFF21162F)
private val FantasyGold = Color(0xFFE4BB77)
private val FantasyParchment = Color(0xFFF1DEB9)
private val FantasyLavender = Color(0xFF4C2B60)

@Composable
fun FantasyNavigationPreview(modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val bar = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.v4_navigation_bar) }
    val entries = listOf(
        Triple("Maison", R.drawable.v4_nav_home, "Destination Maison"),
        Triple("Exploration", R.drawable.v4_nav_exploration, "Destination Exploration"),
        Triple("Suivi", R.drawable.v4_nav_followup, "Destination Suivi"),
        Triple("Le Nid", R.drawable.v4_nav_nest, "Destination Le Nid"),
    )
    Box(modifier.fillMaxWidth().aspectRatio(1985f / 386f)) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = bar,
                srcOffset = IntOffset(94, 169),
                srcSize = IntSize(1985, 386),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(58.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavDestination(entries[0], Modifier.weight(1f))
            NavDestination(entries[1], Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
            NavDestination(entries[2], Modifier.weight(1f))
            NavDestination(entries[3], Modifier.weight(1f))
        }
        Image(
            painter = painterResource(R.drawable.v4_plus_medallion),
            contentDescription = "Action Cr\u00e9er une action",
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopCenter)
                .offset(y = 1.dp)
                .clickable { },
        )
    }
}

@Composable
private fun NavDestination(entry: Triple<String, Int, String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(56.dp).clickable { }.semantics { contentDescription = entry.third },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(entry.second), contentDescription = null, modifier = Modifier.size(25.dp))
        Text(entry.first, color = FantasyGold, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun TaskodayV4AssetGallery() {
    Column(
        modifier = Modifier.fillMaxSize().background(FantasyInk).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Taskoday V4 · Galerie Android", color = FantasyGold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("DÉMO DEBUG · données fictives · aucun appel réseau", color = FantasyParchment, fontSize = 12.sp)
        GalleryHeading("Header compact · asset V4.1")
        FantasyCompactHeader()

        GalleryHeading("Résumé normal · DEBUG")
        FantasySummaryPanel(Modifier.heightIn(min = 118.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Aujourd'hui · aperçu", color = FantasyGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("3 actions prévues", color = FantasyParchment, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("1 à valider  ·  1 en retard", color = FantasyParchment, fontSize = 12.sp)
            }
        }
        GalleryHeading("Résumé vide · DEBUG")
        FantasySummaryPanel(Modifier.heightIn(min = 116.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Aujourd'hui · aperçu", color = FantasyGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Aucune action prévue", color = FantasyParchment, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("État vide fictif", color = FantasyParchment, fontSize = 12.sp)
            }
        }
        GalleryHeading("Résumé erreur · DEBUG")
        FantasySummaryPanel(Modifier.heightIn(min = 122.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Informations indisponibles", color = FantasyGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Aperçu d'erreur · aucun serveur", color = FantasyParchment, fontSize = 16.sp)
            }
        }

        GalleryHeading("Ornement supérieur · PNG indépendant")
        Image(
            painter = painterResource(R.drawable.v4_top_ornament),
            contentDescription = "Ornement supérieur fantasy Taskoday V4",
            modifier = Modifier.fillMaxWidth().height(74.dp),
        )
        GalleryHeading("Médaillon de tâche + icône séparée")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FantasyTaskMedallion(R.drawable.v4_task_mission, "Médaillon Mission DEBUG")
            FantasyTaskMedallion(R.drawable.v4_task_routine, "Médaillon Routine DEBUG")
            FantasyTaskMedallion(R.drawable.v4_task_house, "Médaillon Maison DEBUG")
            FantasyTaskMedallion(R.drawable.v4_task_quest, "Médaillon Quête DEBUG")
            Text("Icônes indépendantes du fond", color = FantasyParchment, fontSize = 12.sp)
        }

        GalleryHeading("Carte · titre court · DEBUG")
        FantasyTaskCardBackground(Modifier.heightIn(min = 132.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Préparer le dîner", color = FantasyInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("MISSION  ·  À faire", color = Color(0xFF83483B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Léo, Mila  ·  Aujourd'hui", color = Color(0xFF5F4E55), fontSize = 12.sp)
            }
            Box(Modifier.align(Alignment.CenterStart).offset(x = (-58).dp)) {
                FantasyTaskMedallion(R.drawable.v4_task_mission, "Mission fictive")
            }
        }
        GalleryHeading("Carte · titre long · DEBUG")
        FantasyTaskCardBackground(Modifier.heightIn(min = 164.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Préparer les décorations pour la maison familiale", color = FantasyInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("QUÊTE  ·  Sans échéance", color = Color(0xFF80602C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Léo, Mila  ·  Aperçu seulement", color = Color(0xFF5F4E55), fontSize = 12.sp)
            }
            Box(Modifier.align(Alignment.CenterStart).offset(x = (-58).dp)) {
                FantasyTaskMedallion(R.drawable.v4_task_quest, "Quête fictive")
            }
        }
        GalleryHeading("Carte · hauteur variable · DEBUG")
        FantasyTaskCardBackground(Modifier.heightIn(min = 194.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ranger l'entrée", color = FantasyInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("ROUTINE  ·  En cours", color = Color(0xFF397A74), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Aperçu de ligne complémentaire", color = Color(0xFF5F4E55), fontSize = 12.sp)
                Text("Contenu à hauteur variable, sans couper les ornements", color = Color(0xFF5F4E55), fontSize = 12.sp)
            }
            Box(Modifier.align(Alignment.CenterStart).offset(x = (-58).dp)) {
                FantasyTaskMedallion(R.drawable.v4_task_routine, "Routine fictive")
            }
        }

        GalleryHeading("Bouton violet V4 · texte Compose")
        FantasyPurpleButton("Action · DEBUG", Modifier.width(190.dp))
        GalleryHeading("Navigation Parent · quatre destinations + action")
        FantasyNavigationPreview()
        Text("Aperçu visuel uniquement. Aucun callback métier ni API n'est branché.", color = FantasyParchment, fontSize = 12.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun GalleryHeading(text: String) {
    Text(text, color = FantasyGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
}
