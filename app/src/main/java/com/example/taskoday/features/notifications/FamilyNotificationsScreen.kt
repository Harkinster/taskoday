package com.example.taskoday.features.notifications

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.R
import com.example.taskoday.core.ui.component.forms.*
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground

@Composable
fun FamilyNotificationsScreen(viewModel: FamilyNotificationsViewModel,onBack: ()->Unit) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var permissionGeneration by remember { mutableStateOf("") }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted)viewModel.setEnabled(true,permissionGeneration) else viewModel.permissionDenied(permissionGeneration)
    }
    fun pick(value: Int,selected: (Int,Int)->Unit) {
        TimePickerDialog(context,R.style.Theme_Taskoday_FormPicker,{ _,h,m -> selected(h,m) },value/60,value%60,true).show()
    }
    FantasyFormTheme {
        Scaffold(containerColor=androidx.compose.ui.graphics.Color.Transparent,contentWindowInsets=WindowInsets(0,0,0,0),
            topBar={ Column(Modifier.statusBarsPadding().padding(16.dp)) { FantasyFormHeader("Mes notifications","Des rappels discrets, pour ce compte",onBack) } }) { padding ->
            FantasyScreenBackground(Modifier.padding(padding)) {
                NotificationPreferencesContent(ui,
                    onEnabled={ enabled ->
                        if(enabled && !notificationPermissionGranted(context)) {
                            if(Build.VERSION.SDK_INT>=33) { permissionGeneration=ui.generation;launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            else viewModel.permissionDenied()
                        } else viewModel.setEnabled(enabled)
                    },onCategory=viewModel::category,onQuiet=viewModel::setQuiet,
                    onQuietTime={ start -> pick(if(start)ui.settings.quietStartMinute else ui.settings.quietEndMinute) {h,m->viewModel.quietTime(start,h,m)} },
                    onSummaryTime={pick(ui.settings.hour*60+ui.settings.minute,viewModel::summaryTime)},
                    onCheck=viewModel::checkNow,onRetry=viewModel::refresh,
                    onSystemSettings={
                        val intent=if(Build.VERSION.SDK_INT>=26)Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
                            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    })
            }
        }
    }
}

@Composable
internal fun NotificationPreferencesContent(ui: FamilyNotificationsUiState,onEnabled:(Boolean)->Unit,onCategory:(NoticeKind,Boolean)->Unit,
    onQuiet:(Boolean)->Unit,onQuietTime:(Boolean)->Unit,onSummaryTime:()->Unit,onCheck:()->Unit,onRetry:()->Unit,onSystemSettings:()->Unit) {
    val s=ui.settings
    val ready=!ui.isBusy && ui.owner!=null
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { FantasyFormPanel(title="Ce compte") {
            Text(if(ui.parent) "Compte Parent · famille active" else "Compte personnel · famille active")
            PreferenceSwitch("Activer les notifications",s.enabled,ready,onEnabled)
            Text("Aucun nom de membre ni titre de tâche n’est affiché dans les alertes.",style=MaterialTheme.typography.bodySmall)
            if(!ui.hasFamily && ready)Text("Choisis une famille active pour recevoir ses rappels.")
            if(ui.isBusy)CircularProgressIndicator()
        } }
        item { FantasyFormPanel(title="Mes rappels") {
            PreferenceSwitch("Échéances personnelles",s.personalDue,ready) { onCategory(NoticeKind.PERSONAL_DUE,it) }
            if(ui.parent)PreferenceSwitch("Missions à valider",s.validationNeeded,ready) { onCategory(NoticeKind.VALIDATION_NEEDED,it) }
            PreferenceSwitch("Missions validées ou à reprendre",s.missionDecision,ready) { onCategory(NoticeKind.MISSION_VALIDATED,it) }
            PreferenceSwitch("Retards importants",s.importantOverdue,ready) { onCategory(NoticeKind.IMPORTANT_OVERDUE,it) }
        } }
        item { FantasyFormPanel(title="Moments de tranquillité") {
            PreferenceSwitch("Plage silencieuse",s.quietEnabled,ready,onQuiet)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FantasyFormOutlineButton({onQuietTime(true)},Modifier.weight(1f),ready) { Text("Début · ${formatFamilyNotificationTime(s.quietStartMinute/60,s.quietStartMinute%60)}") }
                FantasyFormOutlineButton({onQuietTime(false)},Modifier.weight(1f),ready) { Text("Fin · ${formatFamilyNotificationTime(s.quietEndMinute/60,s.quietEndMinute%60)}") }
            }
            Text("Les alertes encore utiles attendent la fin de cette plage. Les heures suivent le fuseau de cet appareil.",style=MaterialTheme.typography.bodySmall)
        } }
        if(ui.parent)item { FantasyFormPanel(title="Résumé du foyer") {
            PreferenceSwitch("Résumé quotidien",s.dailySummaryEnabled,ready) {onCategory(NoticeKind.DAILY_SUMMARY,it)}
            FantasyFormOutlineButton(onSummaryTime,enabled=ready) {Text("Heure · ${formatFamilyNotificationTime(s.hour,s.minute)}")}
        } }
        item { FantasyFormPanel {
            Text("Android peut différer les rappels. Une connexion est nécessaire pour vérifier les changements ; il ne s’agit pas d’une alarme exacte.",style=MaterialTheme.typography.bodySmall)
            FantasyFormFilledButton(onCheck,Modifier.fillMaxWidth(),ready && s.enabled) {Text("Vérifier maintenant")}
            FantasyFormOutlineButton(onSystemSettings,Modifier.fillMaxWidth()) {Text("Réglages Android")}
            ui.errorMessage?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            ui.successMessage?.let {Text(it)}
            if(ui.owner==null && !ui.isBusy)FantasyFormOutlineButton(onRetry) {Text("Réessayer")}
        } }
    }
}

@Composable private fun PreferenceSwitch(label:String,checked:Boolean,enabled:Boolean,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label,Modifier.weight(1f));Switch(checked,change,enabled=enabled,modifier=Modifier.semantics {contentDescription=label})
    }
}
