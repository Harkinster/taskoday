package com.example.taskoday

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import com.example.taskoday.update.UpdateClient
import com.example.taskoday.update.UpdateManifest
import com.example.taskoday.update.LocalTaskodayUpdateCheck
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.taskoday.core.ui.theme.TaskodayTheme
import com.example.taskoday.navigation.TaskodayApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var updateDialog by mutableStateOf(false)
    private var updateManifest by mutableStateOf<UpdateManifest?>(null)
    private var updateMessage by mutableStateOf<String?>(null)
    private var updateChecking by mutableStateOf(false)
    private var updateBusy by mutableStateOf(false)
    private var updateProgress by mutableStateOf(0f)
    private val updateClient by lazy { UpdateClient(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContent {
            TaskodayTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalTaskodayUpdateCheck provides { checkForUpdates() }) {
                    TaskodayApp()
                    val manifest = updateManifest
                    if (updateDialog) AlertDialog(
                        onDismissRequest = { if (manifest?.mandatory != true && !updateBusy) updateDialog = false },
                        title = { Text(updateMessage ?: if (manifest == null) "Mise à jour" else "Taskoday ${manifest.versionName} disponible") },
                        text = { Column { if (manifest != null) { Text("Nouvelle version ${manifest.versionName} (version ${manifest.versionCode})."); manifest.changelog.forEach { Text("• $it") } }; if (updateBusy) LinearProgressIndicator(progress = { updateProgress }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) } },
                        confirmButton = { TextButton(enabled = !updateBusy && !updateChecking, onClick = { if (manifest != null) downloadAndInstall(manifest) else checkForUpdates() }) { Text(if (updateChecking) "Recherche…" else if (updateBusy) "Téléchargement…" else if (updateMessage?.contains("Réessay") == true || manifest == null) "Réessayer" else "Mettre à jour") } },
                        dismissButton = { if (manifest?.mandatory != true) TextButton(onClick = { updateDialog = false }) { Text("Plus tard") } },
                    )
                }
            }
        }
        if (isUpdateBuild()) {
            val prefs = getSharedPreferences("official_updates", MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (com.example.taskoday.update.UpdateCheckSchedule.shouldCheckAutomatically(
                    nowMillis = now,
                    lastSuccessfulCheckMillis = prefs.getLong("last_successful_check", 0L),
                    lastAttemptMillis = prefs.getLong("last_attempt", 0L),
                    successIntervalMillis = com.example.taskoday.BuildConfig.UPDATE_CHECK_INTERVAL_MS,
                    failureRetryIntervalMillis = com.example.taskoday.BuildConfig.UPDATE_FAILURE_RETRY_INTERVAL_MS,
                )
            ) {
                prefs.edit().putLong("last_attempt", now).apply()
                startUpdateCheck(manual = false, attemptAlreadyRecorded = true)
            }
        }
    }

    fun checkForUpdates() {
        updateDialog = true
        if (!isUpdateBuild()) {
            updateMessage = "Les mises à jour HarkServ sont réservées aux versions Release signées."
            return
        }
        startUpdateCheck(manual = true)
    }

    private fun startUpdateCheck(manual: Boolean, attemptAlreadyRecorded: Boolean = false) {
        if (updateChecking) return
        updateChecking = true
        if (manual) {
            updateMessage = "Recherche des mises à jour…"
            updateManifest = null
            updateDialog = true
        }
        val prefs = getSharedPreferences("official_updates", MODE_PRIVATE)
        if (!attemptAlreadyRecorded) prefs.edit().putLong("last_attempt", System.currentTimeMillis()).apply()
        lifecycleScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { updateClient.check() } }
            result.onSuccess { manifest ->
                // Only a complete, parsed and trusted server response advances this timestamp.
                prefs.edit().putLong("last_successful_check", System.currentTimeMillis()).apply()
                updateManifest = manifest
                updateMessage = if (manual && manifest == null) "Taskoday est à jour, ou aucune mise à jour n’est publiée." else null
                if (manifest != null) updateDialog = true
            }.onFailure { error ->
                if (manual) {
                    updateMessage = when (error) {
                        is com.example.taskoday.update.UpdateCheckFailure.Network -> "Connexion impossible. Vérifiez votre réseau puis réessayez."
                        is com.example.taskoday.update.UpdateCheckFailure.Server -> "Le serveur de mises à jour est indisponible. Réessayez plus tard."
                        is com.example.taskoday.update.UpdateCheckFailure.InvalidManifest -> "La réponse du serveur n’a pas pu être vérifiée. Réessayez plus tard."
                        else -> "Recherche impossible. Vérifiez votre connexion puis réessayez."
                    }
                }
            }
            updateChecking = false
        }
    }

    private fun isUpdateBuild(): Boolean =
        com.example.taskoday.BuildConfig.BUILD_TYPE.equals("release", ignoreCase = true) ||
            com.example.taskoday.BuildConfig.BUILD_TYPE.equals("qa", ignoreCase = true)

    private fun downloadAndInstall(manifest: UpdateManifest) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            updateMessage = "Autorisez l’installation d’applications pour Taskoday dans l’écran Android, puis appuyez sur Mettre à jour."
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                .onFailure { updateMessage = "Android n’a pas pu ouvrir le réglage d’autorisation. Activez l’installation de sources inconnues dans les paramètres système." }
            return
        }
        updateBusy = true; updateMessage = null; updateProgress = 0f
        lifecycleScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { updateClient.download(manifest) { count -> runOnUiThread { updateProgress = count.toFloat() / (manifest.sizeBytes ?: 1L) } } } }
            result.onSuccess { file ->
                updateBusy = false
                runCatching {
                    val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.updates", file)
                    startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
                    .onFailure { updateMessage = "Android n’a pas pu ouvrir l’installateur. Réessayez depuis cette fenêtre." }
            }.onFailure { error ->
                android.util.Log.e("TaskodayUpdater", "Update download or verification failed", error)
                updateBusy = false
                updateMessage = "Téléchargement ou vérification interrompu. Appuyez sur Réessayer."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
