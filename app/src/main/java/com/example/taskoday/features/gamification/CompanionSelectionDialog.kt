package com.example.taskoday.features.gamification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taskoday.R
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.data.remote.dto.LineageDto
import com.example.taskoday.data.remote.dto.PersonalCompanionDto
import com.example.taskoday.core.ui.theme.SoftGold
import java.text.Normalizer

private data class LineageLook(
    val name: String,
    val region: String,
    val affinity: String,
    val description: String,
    val tint: Color,
    val suggestions: List<String>,
)

private val lineageLooks = mapOf(
    "FULMIO" to LineageLook("Fulmio", "Hautes-Tempêtes", "Tempête", "Éclairs et ciel immense", Color(0xFF4B9FE8), listOf("Voltis", "Azurio", "Éclairin", "Célesto", "Bruméo")),
    "SYLVYN" to LineageLook("Sylvyn", "Sylveracine", "Racines", "Forêt vivante et feuillage", Color(0xFF70A958), listOf("Mousseau", "Verdel", "Lianor", "Feuillio", "Racino")),
    "PHENOR" to LineageLook("Phenor", "Soléa", "Solaire", "Lumière douce et chaleur", Color(0xFFE8A94F), listOf("Solino", "Doram", "Aurélo", "Rayonnel", "Soleïo")),
    "LUNARYS" to LineageLook("Lunarys", "Clairlune", "Lunaire", "Lune claire et ciel étoilé", Color(0xFF8F82D7), listOf("Noctélia", "Sélune", "Astrel", "Lunéo", "Miralys")),
    "PYRON" to LineageLook("Pyron", "Braisecime", "Braise", "Roches chaudes et braises", Color(0xFFE36F50), listOf("Flamaris", "Braséo", "Cendrel", "Ardéo", "Rougeo")),
    "CHRONYX" to LineageLook("Chronyx", "Chronéa", "Temps", "Cuivre ancien et temps suspendu", Color(0xFF4CAFAE), listOf("Tictéo", "Cuivrin", "Éonel", "Sablio", "Tempo")),
    "AMBRIO" to LineageLook("Ambrio", "Ambrelande", "Ambre", "Prairies fleuries et miel", Color(0xFFD39B46), listOf("Mielin", "Ambrélo", "Fleuro", "Douciel", "Pollen")),
    "CRISTAO" to LineageLook("Cristao", "Cristalia", "Cristal", "Cristaux clairs et lumière", Color(0xFF62BDE0), listOf("Azuriel", "Clairin", "Opaléo", "Givrel", "Célestio")),
)

internal val companionLineageIds = lineageLooks.keys.toList()

internal fun lineageSuggestions(lineageId: String): List<String> =
    lineageLooks[lineageId.uppercase()]?.suggestions.orEmpty()

internal fun alternateNameSuggestion(lineageId: String, current: String): String? =
    lineageSuggestions(lineageId).filterNot { it.equals(current.trim(), ignoreCase = true) }.randomOrNull()

internal fun companionNameError(input: String): String? {
    val value = Normalizer.normalize(input.trim(), Normalizer.Form.NFC)
    if (value.isEmpty()) return "Entre un nom pour ton dragon."
    if (value.length > 32) return "Le nom ne peut pas dépasser 32 caractères."
    val allowedPunctuation = setOf(' ', '-', '\'', '’')
    if (value.any { char -> !(char.isLetterOrDigit() || Character.getType(char).toInt() in setOf(6, 7, 8) || char in allowedPunctuation) }) {
        return "Utilise des lettres, des chiffres, des espaces ou des tirets."
    }
    if (value.none(Char::isLetter)) return "Le nom doit contenir au moins une lettre."
    return null
}

private fun companionStageLabel(stage: String): String = when (stage.uppercase()) {
    "BABY" -> "Bébé"
    "YOUNG" -> "Jeune"
    "MEDIUM" -> "Adolescent"
    "LARGE" -> "Adulte"
    "LEGENDARY" -> "Légendaire"
    else -> "Stade inconnu"
}

@Composable
internal fun CompanionSelectionDialog(
    companions: List<PersonalCompanionDto>,
    activeState: String,
    lineages: List<LineageDto>,
    isLoadingLineages: Boolean,
    isSubmitting: Boolean,
    message: String?,
    onLoadLineages: () -> Unit,
    onCreateStarter: (String, String) -> Unit,
    onActivate: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { onLoadLineages() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = Color.Transparent,
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF244B42), Color(0xFF102E32))))) {
                Image(
                    painter = painterResource(R.drawable.chronodria_nest_environment_v1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x663C6455), Color(0xCC102E32)))))
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Le Nid", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Fermer", tint = Color.White) }
                    }
                    if (companions.isNotEmpty()) {
                        CompanionRoster(
                            companions, activeState, isSubmitting, message, onActivate, onRename, onDismiss,
                        )
                    } else if (!isLoadingLineages && lineages.size == 8) {
                        StarterFlow(lineages, isSubmitting, message, onCreateStarter, onDismiss)
                    } else if (isLoadingLineages) {
                        Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SoftGold)
                            Text("Les lignées se préparent…", color = Color.White, modifier = Modifier.padding(top = 14.dp))
                        }
                    } else {
                        Text(message ?: "Impossible de charger les lignées. Vérifie ta connexion puis réessaie.", color = Color.White, modifier = Modifier.padding(top = 32.dp))
                        Button(onClick = onLoadLineages, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 14.dp)) { Text("Réessayer") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterFlow(
    lineages: List<LineageDto>,
    isSubmitting: Boolean,
    message: String?,
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val selected = lineages.getOrNull(page)
    val look = selected?.let { lineageLooks[it.id] }
    LaunchedEffect(selected?.id) {
        name = selected?.let { lineageSuggestions(it.id).randomOrNull().orEmpty() }.orEmpty()
    }
    val nameError = companionNameError(name)

    if (phase == 0 && selected != null && look != null) {
        Text("Choisis ta lignée", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Chaque jeune dragon porte une étincelle différente.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE7F0DF), modifier = Modifier.padding(top = 4.dp))
        LineageCarousel(lineages, page, onPageChange = { page = it })
        LineagePreview(selected, look)
        message?.let { Text(it, color = Color(0xFFFFD7A4), modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { phase = 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF382710))) {
            Text("Continuer avec la lignée ${look.name}")
        }
        TextButtonLike("Plus tard", onDismiss)
    } else if (phase == 1 && selected != null && look != null) {
        BackHeader("Donne-lui un nom", onBack = { phase = 0 })
        LineagePreview(selected, look, compact = true)
        Text("Voici une idée pour ton compagnon.", color = Color(0xFFE7F0DF), modifier = Modifier.padding(top = 6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(64) },
            label = { Text("Nom de ton dragon") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            supportingText = { Text(nameError ?: "Jusqu’à 32 caractères", color = if (nameError == null) Color(0xFFDCE8DF) else Color(0xFFFFB1A1)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = SoftGold,
                unfocusedLabelColor = Color(0xFFE7F0DF),
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = .72f),
                cursorColor = SoftGold,
            ),
        )
        OutlinedButton(
            onClick = {
                name = alternateNameSuggestion(selected.id, name) ?: name
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(top = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) { Icon(Icons.Outlined.Refresh, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Autre nom") }
        message?.let { Text(it, color = Color(0xFFFFD7A4), modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { if (nameError == null) phase = 2 }, enabled = nameError == null, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF382710))) { Text("Continuer") }
    } else if (selected != null && look != null) {
        BackHeader("Ton compagnon est prêt", onBack = { phase = 1 })
        LineagePreview(selected, look)
        Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp), color = Color(0x663C6455), border = androidx.compose.foundation.BorderStroke(1.dp, SoftGold.copy(alpha = .8f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name.trim(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Lignée ${look.name} · ${look.region}", color = Color(0xFFE7F0DF))
                Text("Aperçu illustré · stade bébé à l’accueil", color = Color(0xFFCDE0D2), style = MaterialTheme.typography.bodySmall)
            }
        }
        message?.let { Text(it, color = Color(0xFFFFD7A4), modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { if (!isSubmitting && nameError == null) onCreate(selected.id, name.trim()) }, enabled = !isSubmitting && nameError == null, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF382710))) {
            if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF382710)) else Text("Accueillir mon dragon")
        }
        Text("La création aura lieu après ta confirmation.", color = Color(0xFFCDE0D2), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun LineageCarousel(lineages: List<LineageDto>, selected: Int, onPageChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onPageChange((selected - 1 + lineages.size) % lineages.size) }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Lignée précédente", tint = Color.White) }
        Text("${selected + 1} / ${lineages.size}", color = Color.White, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onPageChange((selected + 1) % lineages.size) }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Lignée suivante", tint = Color.White) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(2) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { column ->
                    val index = row * 4 + column
                    val lineage = lineages.getOrNull(index)
                    if (lineage != null) {
                        val look = lineageLooks[lineage.id]
                        Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                            .background((look?.tint ?: Color.Gray).copy(alpha = if (index == selected) .75f else .40f))
                            .border(if (index == selected) 2.dp else 1.dp, if (index == selected) SoftGold else Color.White.copy(alpha = .55f), RoundedCornerShape(14.dp))
                            .clickable { onPageChange(index) }, contentAlignment = Alignment.Center) {
                            Text(look?.name ?: lineage.founderName, color = Color.White,
                                fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineagePreview(lineage: LineageDto, look: LineageLook, compact: Boolean = false) {
    val hasSprite = lineage.id != "FULMIO" && lineage.id != "CHRONYX"
    Surface(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(24.dp), color = Color(0x3DFFFFFF), border = androidx.compose.foundation.BorderStroke(1.dp, look.tint.copy(alpha = .8f))) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasSprite) {
                Image(painterResource(NestAssets.dragonAsset(lineage.id.lowercase(), "baby")), contentDescription = "Aperçu du jeune dragon de la lignée ${look.name}", modifier = Modifier.size(if (compact) 120.dp else 176.dp), contentScale = ContentScale.Fit)
            } else {
                Box(Modifier.size(if (compact) 120.dp else 176.dp).clip(CircleShape).background(Brush.radialGradient(listOf(look.tint.copy(alpha = .95f), Color(0x22374655)))).border(2.dp, SoftGold, CircleShape), contentAlignment = Alignment.Center) {
                    Text(look.name.take(1), style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Text("Lignée ${look.name}", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(lineage.regionName, style = MaterialTheme.typography.titleMedium, color = SoftGold)
            Text("${affinityLabel(lineage.affinity)} · ${look.description}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE7F0DF), textAlign = TextAlign.Center)
        }
    }
}

private fun affinityLabel(affinity: String): String = when (affinity.lowercase()) {
    "tempest" -> "Tempête"
    "root" -> "Racines"
    "solar" -> "Solaire"
    "lunar" -> "Lunaire"
    "ember" -> "Braise"
    "chronos" -> "Temps"
    "heart" -> "Ambre"
    "crystal" -> "Cristal"
    else -> "Affinité magique"
}

@Composable
private fun CompanionRoster(
    companions: List<PersonalCompanionDto>, activeState: String, isSubmitting: Boolean, message: String?,
    onActivate: (Long) -> Unit, onRename: (Long, String) -> Unit, onDismiss: () -> Unit,
) {
    var renameId by remember { mutableStateOf<Long?>(null) }
    var renameValue by remember { mutableStateOf("") }
    Text("Tes compagnons", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
    if (activeState == "LEGACY_CONFLICT") Text("Plusieurs compagnons historiques sont actifs. Aucun choix n’a été fait automatiquement.", color = Color(0xFFFFD7A4), modifier = Modifier.padding(top = 8.dp))
    companions.forEach { companion ->
        val lineageName = companion.lineage?.founderName ?: "Lignée inconnue"
        Surface(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(18.dp), color = Color(0x553F6657), border = androidx.compose.foundation.BorderStroke(1.dp, if (companion.active) SoftGold else Color.White.copy(alpha = .28f))) {
            Column(Modifier.padding(12.dp)) {
                Text(companion.displayName ?: "Compagnon sans nom", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(if (companion.lineageStatus == "LEGACY_UNKNOWN") "Lignée historique inconnue · ${companionStageLabel(companion.stage)}" else "Lignée $lineageName · ${companion.lineage?.regionName.orEmpty()} · ${companionStageLabel(companion.stage)}", color = Color(0xFFE7F0DF))
                Text(if (companion.active) "Compagnon actif" else "Dans ta collection", color = if (companion.active) SoftGold else Color(0xFFCDE0D2), modifier = Modifier.padding(top = 2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (!companion.active) OutlinedButton(onClick = { onActivate(companion.dragonId) }, enabled = !isSubmitting, modifier = Modifier.weight(1f).heightIn(min = 48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Activer") }
                    OutlinedButton(onClick = { renameId = companion.dragonId; renameValue = companion.displayName.orEmpty() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Renommer") }
                }
                if (renameId == companion.dragonId) {
                    OutlinedTextField(renameValue, { renameValue = it.take(64) }, label = { Text("Nouveau nom") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    Button(onClick = { if (companionNameError(renameValue) == null && !isSubmitting) { onRename(companion.dragonId, renameValue.trim()); renameId = null } }, enabled = companionNameError(renameValue) == null && !isSubmitting, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Enregistrer le nom") }
                }
            }
        }
    }
    message?.let { Text(it, color = Color(0xFFFFD7A4), modifier = Modifier.padding(top = 8.dp)) }
    TextButtonLike("Retour au Nid", onDismiss)
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour", tint = Color.White) }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(text) }
}
