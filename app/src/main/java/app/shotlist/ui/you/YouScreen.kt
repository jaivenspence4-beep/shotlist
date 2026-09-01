package app.shotlist.ui.you

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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.data.Finding
import app.shotlist.ui.glass.GlassPanel
import dev.chrisbanes.haze.HazeState

@Composable
fun YouScreen(
    hazeState: HazeState,
    screenshotsChecked: Int,
    thingsReady: Int,
    vaultedFindings: List<Finding>,
    vaultUnlocked: Boolean,
    imageAccessGranted: Boolean,
    autoScanEnabled: Boolean,
    onAutoScanChanged: (Boolean) -> Unit,
    onOpenVault: () -> Unit,
    onCopyVaulted: (Finding) -> Unit,
    onUnvault: (Finding) -> Unit,
    onReplayOnboarding: () -> Unit,
    onShareBugReport: () -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                            "Private vault · ${vaultedFindings.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Text(
                            if (vaultUnlocked) "Unlocked for this visit" else "Codes and Wi-Fi, behind your screen lock",
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
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                        onDeleteAllData()
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
                Text("Stays on this device", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "0 bytes sent. No account. No cloud.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivacyMetric("$screenshotsChecked", "checked", MaterialTheme.colorScheme.primary)
            PrivacyMetric("$thingsReady", "ready", MaterialTheme.colorScheme.tertiary)
            PrivacyMetric("0", "sent", MaterialTheme.colorScheme.secondary)
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
