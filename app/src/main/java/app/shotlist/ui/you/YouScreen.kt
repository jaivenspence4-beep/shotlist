package app.shotlist.ui.you

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.WifiPassword
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.data.Finding
import app.shotlist.data.ShotlistExport
import app.shotlist.engine.IngestWorker
import app.shotlist.entitlement.Entitlement
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.privacy.PrivacyPolicyScreen
import app.shotlist.ui.theme.LivingScene
import app.shotlist.ui.theme.ShotlistPalette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun YouScreen(
    hazeState: HazeState,
    screenshotsChecked: Int,
    thingsReady: Int,
    vaultedFindings: List<Finding>,
    vaultTotalCount: Int,
    vaultUnlocked: Boolean,
    imageAccessGranted: Boolean,
    autoScanEnabled: Boolean,
    palette: ShotlistPalette,
    livingScene: LivingScene,
    entitlement: Entitlement,
    showEntitlementPreview: Boolean,
    onPaletteChanged: (ShotlistPalette) -> Unit,
    onLivingSceneChanged: (LivingScene) -> Unit,
    onEntitlementChanged: (Entitlement) -> Unit,
    onShowProPreview: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenShatter: () -> Unit,
    onAutoScanChanged: (Boolean) -> Unit,
    onOpenVault: () -> Unit,
    onCopyVaulted: (Finding) -> Unit,
    onUnvault: (Finding) -> Unit,
    onReplayOnboarding: () -> Unit,
    onShareBugReport: () -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showPrivateExportDialog by remember { mutableStateOf(false) }
    var exportInProgress by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var lastImportCount by remember { mutableStateOf<Int?>(null) }
    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        if (uris.isNotEmpty()) {
            lastImportCount = uris.size
            IngestWorker.enqueueSharedAll(context, uris)
        }
    }
    val hiddenVaultCount = (vaultTotalCount - vaultedFindings.size).coerceAtLeast(0)

    fun startExport() {
        if (exportInProgress) return
        scope.launch {
            exportInProgress = true
            exportError = null
            try {
                runCatching {
                    val send = ShotlistExport.shareIntent(context)
                    context.startActivity(Intent.createChooser(send, "Share Shotlist data"))
                }
                    .onFailure { error ->
                        exportError = error.message ?: "Export failed"
                    }
            } finally {
                exportInProgress = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
    ) {
        item {
            PrivacySeal(
                hazeState = hazeState,
                screenshotsChecked = screenshotsChecked,
                thingsReady = thingsReady,
            )
        }
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!vaultUnlocked) onOpenVault()
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = {
                            Icon(
                                Icons.Outlined.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (entitlement.isPro) {
                                "Private vault · $vaultTotalCount"
                            } else {
                                "Private vault · ${vaultedFindings.size}/${entitlement.vaultItemLimit}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Text(
                            when {
                                !vaultUnlocked -> "Codes and Wi-Fi, behind your screen lock"
                                hiddenVaultCount > 0 -> "$hiddenVaultCount more private ${if (hiddenVaultCount == 1) "find" else "finds"} in Pro preview"
                                else -> "Unlocked for this visit"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                    Icon(
                        if (vaultUnlocked) Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = if (vaultUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (vaultUnlocked) {
            if (vaultedFindings.isEmpty()) {
                item {
                    GlassPanel(
                        hazeState = hazeState,
                        cornerRadius = 26.dp,
                        contentPadding = PaddingValues(15.dp),
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Nothing sensitive saved", fontWeight = FontWeight.Bold)
                        Text(
                            "Codes and Wi-Fi passwords you find will appear here automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                }
            } else {
                items(vaultedFindings, key = { "vault-${it.id}" }) { finding ->
                    VaultFindingCard(
                        hazeState = hazeState,
                        finding = finding,
                        onCopy = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCopyVaulted(finding)
                        },
                        onUnvault = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUnvault(finding)
                        },
                    )
                }
                if (hiddenVaultCount > 0) {
                    item(key = "vault-capacity-limit") {
                        VaultCapacityCard(
                            hiddenCount = hiddenVaultCount,
                            hazeState = hazeState,
                            onShowProPreview = onShowProPreview,
                        )
                    }
                }
            }
        }
        item(key = "collections") {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenCollections()
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(
                        color = MaterialTheme.colorScheme.secondary,
                        icon = {
                            Icon(
                                Icons.Outlined.CollectionsBookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Collections", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Boards for screenshots and useful finds",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Open collections",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        item {
            Text(
                "YOUR GLASS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Color mood", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShotlistPalette.entries.forEach { choice ->
                        val selected = choice == palette
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) palettePreview(choice).copy(alpha = 0.24f)
                                    else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPaletteChanged(choice)
                                }
                                .padding(vertical = 11.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(palettePreview(choice), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(choice.emoji, color = Color(0xFF0B1020), fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(choice.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                Text("Living background", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                LivingScene.entries.forEach { choice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLivingSceneChanged(choice)
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(choice.label, fontWeight = FontWeight.Bold)
                            Text(
                                choice.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                        if (choice == livingScene) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Import screenshots",
                    detail = lastImportCount?.let { count ->
                        val noun = if (count == 1) "image" else "images"
                        "$count selected $noun sent for on-device processing"
                    } ?: "Choose specific images · no library permission",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        screenshotPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                SettingRow(
                    icon = Icons.Outlined.ImageSearch,
                    title = "Watch new screenshots",
                    detail = if (imageAccessGranted) "Ready in the background" else "Photo access is off",
                    accent = MaterialTheme.colorScheme.primary,
                    trailing = {
                        Switch(
                            checked = autoScanEnabled && imageAccessGranted,
                            enabled = imageAccessGranted,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAutoScanChanged(it)
                            },
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                ActionRow(
                    icon = Icons.Outlined.AutoDelete,
                    title = "Shatter dead screenshots",
                    detail = "Flick through safe-to-trash clutter",
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenShatter()
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                SettingRow(
                    icon = Icons.Outlined.CloudOff,
                    title = "Network access",
                    detail = "Not requested by Shotlist",
                    accent = MaterialTheme.colorScheme.secondary,
                    trailing = {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Off",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                )
            }
        }
        item {
            Text(
                "HELP & DATA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
        if (showEntitlementPreview) {
            item {
                Text(
                    "DEVELOPER PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
            }
            item {
                GlassPanel(
                    hazeState = hazeState,
                    cornerRadius = 30.dp,
                    contentPadding = PaddingValues(16.dp),
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Preview Pro tier",
                        detail = "UI preview only · billing is disconnected",
                        accent = MaterialTheme.colorScheme.tertiary,
                        trailing = {
                            Switch(
                                checked = entitlement.isPro,
                                onCheckedChange = { enabled ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEntitlementChanged(
                                        if (enabled) Entitlement.PRO else Entitlement.FREE,
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionRow(
                    icon = Icons.Outlined.Policy,
                    title = "Privacy policy",
                    detail = "What Shotlist accesses, stores, and shares",
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showPrivacyPolicy = true
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                ActionRow(
                    icon = Icons.Outlined.BugReport,
                    title = "Share bug report",
                    detail = "You choose where the local log goes",
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShareBugReport()
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                ActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Export my data",
                    detail = when {
                        exportInProgress -> "Building ZIP…"
                        exportError != null -> exportError.orEmpty()
                        vaultTotalCount > 0 && !vaultUnlocked -> "Unlock vault first · private values are included"
                        else -> "JSON + image references · no screenshot pixels"
                    },
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when {
                            exportInProgress -> Unit
                            vaultTotalCount > 0 && !vaultUnlocked -> onOpenVault()
                            vaultTotalCount > 0 -> showPrivateExportDialog = true
                            else -> startExport()
                        }
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                ActionRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Replay the welcome",
                    detail = "See the three-tap setup again",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReplayOnboarding()
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                )
                ActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete all my data",
                    detail = "Screenshots, finds, tracking, habits — everything",
                    accent = Color(0xFFFF788F),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    },
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            title = { Text("Delete everything?") },
            text = {
                Text("This permanently clears Shotlist’s local database, settings, copied images, and diagnostic log. Your phone’s original screenshots stay untouched.")
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = false
                        scope.launch {
                            ShotlistExport.clearCached(context)
                            onDeleteAllData()
                        }
                    },
                ) {
                    Text("Delete Shotlist data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Keep it")
                }
            },
        )
    }

    if (showPrivateExportDialog) {
        AlertDialog(
            onDismissRequest = { showPrivateExportDialog = false },
            icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
            title = { Text("Include private vault values?") },
            text = {
                Text(
                    "This ZIP includes vaulted codes, Wi-Fi details, and any other private payloads. Only share it somewhere you trust. Screenshot pixels are not included.",
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPrivateExportDialog = false
                        startExport()
                    },
                ) {
                    Text("Include and export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivateExportDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onClose = { showPrivacyPolicy = false })
    }
}

@Composable
private fun VaultCapacityCard(
    hiddenCount: Int,
    hazeState: HazeState,
    onShowProPreview: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(15.dp),
        accent = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShowProPreview),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "$hiddenCount more private ${if (hiddenCount == 1) "find" else "finds"}",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Preview Pro to see your unlimited vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Preview Pro",
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

private fun palettePreview(palette: ShotlistPalette): Color = when (palette) {
    ShotlistPalette.COSMIC -> Color(0xFFA8B8FF)
    ShotlistPalette.TIDE -> Color(0xFF62DDF5)
    ShotlistPalette.SUNSET -> Color(0xFFFF9B78)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFindingCard(
    hazeState: HazeState,
    finding: Finding,
    onCopy: () -> Unit,
    onUnvault: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(15.dp),
        accent = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCopy,
                onLongClick = onUnvault,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (finding.type == "WIFI") Icons.Outlined.WifiPassword else Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(finding.title, fontWeight = FontWeight.Bold)
                Text(
                    finding.payload.ifBlank { finding.snippet },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 2,
                )
            }
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun PrivacySeal(
    hazeState: HazeState,
    screenshotsChecked: Int,
    thingsReady: Int,
) {
    val transition = rememberInfiniteTransition(label = "privacy-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_250),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "seal-pulse",
    )
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 34.dp,
        contentPadding = PaddingValues(18.dp),
        accent = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Screenshots stay on-device", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "No screenshot uploads. No account. No ads.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivacyMetric("$screenshotsChecked", "checked", MaterialTheme.colorScheme.primary)
            PrivacyMetric("$thingsReady", "ready", MaterialTheme.colorScheme.tertiary)
            PrivacyMetric("0", "uploaded", MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun PrivacyMetric(value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.11f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    accent: Color,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
        trailing()
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = accent)
    }
}

@Composable
private fun IconBubble(
    color: Color,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
