package com.m2h.m2haichatbot

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.m2h.m2haichatbot.domain.model.AppSettings
import com.m2h.m2haichatbot.presentation.navigation.NavGraph
import com.m2h.m2haichatbot.presentation.navigation.Screen
import com.m2h.m2haichatbot.presentation.update.AppUpdateViewModel
import com.m2h.m2haichatbot.presentation.theme.M2HAITheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingDownloadId: Long? = null
    private var pendingApkFile: File? = null
    private var onDownloadComplete: ((Boolean, String?) -> Unit)? = null
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == pendingDownloadId) {
                val result = queryDownloadResult(id)
                onDownloadComplete?.invoke(result.success, result.message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        setContent {
            val themeViewModel: com.m2h.m2haichatbot.presentation.theme.ThemeViewModel = hiltViewModel()
            val updateViewModel: AppUpdateViewModel = hiltViewModel()
            val updateSettings by updateViewModel.settings.collectAsState()
            val useDarkTheme by themeViewModel.darkTheme.collectAsState()
            var showSplash by remember { mutableStateOf(true) }
            var dismissedOptionalUpdate by remember { mutableStateOf(false) }
            var updateUiState by remember { mutableStateOf(UpdateUiState()) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                updateViewModel.refresh()
            }
            
            M2HAITheme(darkTheme = useDarkTheme ?: isSystemInDarkTheme()) {
                val authViewModel: com.m2h.m2haichatbot.presentation.auth.AuthViewModel = hiltViewModel()
                val authState by authViewModel.state.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        com.m2h.m2haichatbot.presentation.splash.SplashScreen(
                            onAnimationFinished = { showSplash = false }
                        )
                    } else {
                        if (authState.isLoading && authState.user == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val navController = rememberNavController()
                            val startDestination = if (authState.user != null) {
                                Screen.Home.route
                            } else {
                                Screen.Login.route
                            }
                            
                            NavGraph(
                                navController = navController,
                                startDestination = startDestination
                            )
                        }
                    }

                    val settings = updateSettings
                    val hasUpdateUrl = settings?.updateApkUrl?.isNotBlank() == true
                    val updateAvailable = settings != null &&
                        BuildConfig.VERSION_CODE < settings.latestVersionCode &&
                        !dismissedOptionalUpdate
                    val forcedByVersion = settings != null &&
                        hasUpdateUrl &&
                        BuildConfig.VERSION_CODE < settings.minSupportedVersionCode
                    if (settings != null && (updateAvailable || forcedByVersion)) {
                        AppUpdateDialog(
                            settings = settings,
                            forced = hasUpdateUrl && (settings.updateRequired || forcedByVersion),
                            state = updateUiState,
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            onLater = { dismissedOptionalUpdate = true },
                            onPermission = { openInstallPermissionSettings() },
                            onRetry = {
                                updateUiState = UpdateUiState()
                            },
                            onInstall = {
                                updateUiState.downloadedApkFile?.let { installApk(it) }
                            },
                            onUpdate = {
                                val started = startApkDownload(
                                    settings = settings,
                                    onComplete = { success, message ->
                                        updateUiState = if (success) {
                                            pendingDownloadId = null
                                            updateUiState.copy(
                                                isDownloading = false,
                                                downloadedApkFile = pendingApkFile,
                                                progress = 1f,
                                                statusText = "Download complete. Install when ready.",
                                                errorText = null
                                            )
                                        } else {
                                            pendingDownloadId = null
                                            updateUiState.copy(
                                                isDownloading = false,
                                                statusText = "Download failed",
                                                errorText = message ?: "Unable to download this update."
                                            )
                                        }
                                    }
                                )
                                if (started) {
                                    updateUiState = UpdateUiState(
                                        isDownloading = true,
                                        progress = 0f,
                                        statusText = "Preparing secure download..."
                                    )
                                    scope.launch {
                                        while (updateUiState.isDownloading && pendingDownloadId != null) {
                                            val progress = queryDownloadProgress(pendingDownloadId!!)
                                            if (progress.complete) {
                                                pendingDownloadId = null
                                                updateUiState = updateUiState.copy(
                                                    isDownloading = false,
                                                    downloadedApkFile = pendingApkFile,
                                                    progress = 1f,
                                                    statusText = "Download complete. Install when ready.",
                                                    errorText = null
                                                )
                                                break
                                            }
                                            if (progress.failed) {
                                                pendingDownloadId = null
                                                updateUiState = updateUiState.copy(
                                                    isDownloading = false,
                                                    progress = progress.fraction,
                                                    statusText = "Download failed",
                                                    errorText = progress.label
                                                )
                                                break
                                            }
                                            updateUiState = updateUiState.copy(
                                                progress = progress.fraction,
                                                statusText = progress.label
                                            )
                                            delay(700)
                                        }
                                    }
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                                    updateUiState = UpdateUiState(
                                        needsInstallPermission = true,
                                        statusText = "Allow M2HAI to install updates, then return here."
                                    )
                                } else {
                                    updateUiState = UpdateUiState(
                                        errorText = "Update package URL is missing or invalid."
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(downloadReceiver) }
        super.onDestroy()
    }

    private fun startApkDownload(
        settings: AppSettings,
        onComplete: (success: Boolean, message: String?) -> Unit
    ): Boolean {
        val apkUrl = settings.updateApkUrl.trim()
        if (apkUrl.isBlank()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return false
        }

        val fileName = "m2hai-${settings.latestVersionName}-${settings.latestVersionCode}.apk"
        val targetFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (targetFile.exists()) targetFile.delete()
        pendingApkFile = targetFile

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle(settings.updateTitle.ifBlank { "M2HAI update" })
            .setDescription(settings.updateMessage.ifBlank { "Downloading app update" })
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        onDownloadComplete = onComplete
        pendingDownloadId = manager.enqueue(request)
        return true
    }

    private fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
    }

    private fun queryDownloadResult(downloadId: Long): DownloadResult {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return DownloadResult(false, "Download was not found.")
            val status = cursor.intValue(DownloadManager.COLUMN_STATUS)
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> DownloadResult(true, null)
                DownloadManager.STATUS_FAILED -> DownloadResult(false, "Download failed. Check the APK URL and network connection.")
                else -> DownloadResult(false, "Download did not complete.")
            }
        }
        return DownloadResult(false, "Download status unavailable.")
    }

    private suspend fun queryDownloadProgress(downloadId: Long): DownloadProgress = withContext(Dispatchers.IO) {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@withContext DownloadProgress(0f, "Starting download...")
            }
            val downloaded = cursor.longValue(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = cursor.longValue(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val status = cursor.intValue(DownloadManager.COLUMN_STATUS)
            val fraction = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            val percent = (fraction * 100).toInt()
            val label = when (status) {
                DownloadManager.STATUS_PENDING -> "Waiting for download..."
                DownloadManager.STATUS_RUNNING -> if (total > 0L) "Downloading update $percent%" else "Downloading update..."
                DownloadManager.STATUS_PAUSED -> "Download paused by Android"
                DownloadManager.STATUS_SUCCESSFUL -> "Download complete"
                DownloadManager.STATUS_FAILED -> "Download failed. Check the APK URL and network connection."
                else -> "Preparing update..."
            }
            DownloadProgress(
                fraction = if (status == DownloadManager.STATUS_SUCCESSFUL) 1f else fraction,
                label = label,
                complete = status == DownloadManager.STATUS_SUCCESSFUL,
                failed = status == DownloadManager.STATUS_FAILED
            )
        } ?: DownloadProgress(0f, "Preparing update...")
    }
}

private data class UpdateUiState(
    val isDownloading: Boolean = false,
    val downloadedApkFile: File? = null,
    val progress: Float = 0f,
    val statusText: String = "",
    val errorText: String? = null,
    val needsInstallPermission: Boolean = false
)

private data class DownloadProgress(
    val fraction: Float,
    val label: String,
    val complete: Boolean = false,
    val failed: Boolean = false
)

private data class DownloadResult(val success: Boolean, val message: String?)

@Composable
private fun AppUpdateDialog(
    settings: AppSettings,
    forced: Boolean,
    state: UpdateUiState,
    currentVersionName: String,
    currentVersionCode: Int,
    onLater: () -> Unit,
    onPermission: () -> Unit,
    onRetry: () -> Unit,
    onInstall: () -> Unit,
    onUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!forced) onLater() },
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (forced) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (forced) Icons.Default.Warning else Icons.Default.SystemUpdateAlt,
                    contentDescription = null,
                    tint = if (forced) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(settings.updateTitle.ifBlank { "Update available" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(settings.updateChannel.uppercase()) },
                        leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    if (forced) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Required") },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ElevatedCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UpdateInfoRow("Installed", "$currentVersionName ($currentVersionCode)")
                        UpdateInfoRow("Available", "${settings.latestVersionName} (${settings.latestVersionCode})")
                        if (settings.updateApkSizeMb > 0.0) {
                            UpdateInfoRow("Package", "${settings.updateApkSizeMb} MB")
                        }
                        if (settings.updatePublishedAt.isNotBlank()) {
                            UpdateInfoRow("Published", settings.updatePublishedAt)
                        }
                    }
                }

                Text(
                    text = settings.updateMessage.ifBlank { "A newer M2HAI build is ready to install." },
                    style = MaterialTheme.typography.bodyMedium
                )

                val releaseNotes = settings.updateReleaseNotes
                    .lines()
                    .map { it.trim().trimStart('-', '*', '•').trim() }
                    .filter { it.isNotBlank() }
                    .take(5)
                if (releaseNotes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Release notes", fontWeight = FontWeight.SemiBold)
                        releaseNotes.forEach { note ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.NewReleases,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (settings.updateSha256.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        text = "SHA-256 ${settings.updateSha256}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (settings.updateApkUrl.isBlank()) {
                    Text("Admin has not configured an APK URL yet.", color = MaterialTheme.colorScheme.error)
                }

                if (state.needsInstallPermission) {
                    Text(
                        "Android needs permission to install updates from M2HAI.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (state.isDownloading || state.statusText.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp))
                        )
                        Text(
                            state.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.needsInstallPermission) {
                    TextButton(onClick = onPermission) {
                        Text("Allow install")
                    }
                }
                if (state.errorText != null) {
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                if (state.downloadedApkFile != null) {
                    Button(onClick = onInstall) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Install now", modifier = Modifier.padding(start = 8.dp))
                    }
                    return@Row
                }
                Button(
                    onClick = onUpdate,
                    enabled = settings.updateApkUrl.isNotBlank() && !state.isDownloading,
                    colors = ButtonDefaults.buttonColors()
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(if (state.isDownloading) "Downloading" else "Update now", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        dismissButton = if (forced) null else {
            { TextButton(onClick = onLater, enabled = !state.isDownloading) { Text("Later") } }
        }
    )
}

@Composable
private fun UpdateInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun Cursor.intValue(columnName: String): Int {
    return getInt(getColumnIndexOrThrow(columnName))
}

private fun Cursor.longValue(columnName: String): Long {
    return getLong(getColumnIndexOrThrow(columnName))
}
