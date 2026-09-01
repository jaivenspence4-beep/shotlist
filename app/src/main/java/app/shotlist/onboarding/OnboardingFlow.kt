package app.shotlist.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.shotlist.data.ShotlistDb
import app.shotlist.engine.EngineApi
import app.shotlist.ui.glass.GlassBackdrop
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.glass.glassBackgroundBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val db = remember(context) { ShotlistDb.get(context) }
    val screenshotsRead by db.shots().count().collectAsState(initial = 0)
    val suggestedActions by db.findings().suggestedCount().collectAsState(initial = 0)
    val reveal = OnboardingReveal(screenshotsRead, suggestedActions)
    var step by rememberSaveable {
        mutableStateOf(
            if (hasUsableImageAccess(context)) PermissionStep.Scanning else PermissionStep.Intro,
        )
    }
    var backfillStarted by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = requiredImagePermissions().all { result[it] == true || hasPermission(context, it) }
        val partial = hasPartialImageAccess(context)
        step = if (granted) PermissionStep.Scanning else PermissionStep.Denied
        if (!granted && partial) step = PermissionStep.Scanning
    }

    LaunchedEffect(step) {
        if (step == PermissionStep.Scanning && !backfillStarted) {
            backfillStarted = true
            EngineApi.backfill(context, limit = 100)
            EngineApi.startObserving(context)
        }
    }

    LaunchedEffect(step, screenshotsRead, suggestedActions) {
        if (step == PermissionStep.Scanning && suggestedActions > 0) {
            delay(1_500)
            step = PermissionStep.Ready
        }
    }

    LaunchedEffect(step) {
        if (step == PermissionStep.Scanning) {
            delay(6_500)
            step = PermissionStep.Ready
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glassBackgroundBrush())
            .hazeSource(hazeState)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassBackdrop()
        AnimatedContent(targetState = step, label = "onboarding-step") { currentStep ->
            when (currentStep) {
                PermissionStep.Intro -> IntroStep(
                    onContinue = {
                        step = PermissionStep.Requesting
                        permissionLauncher.launch(requiredScreenshotPermissions().toTypedArray())
                    },
                    onShareOnly = onFinished,
                    hazeState = hazeState,
                )
                PermissionStep.Requesting -> ProgressStep(
                    title = "Waiting for permission",
                    detail = "Android is asking for screenshot access now.",
                    reveal = reveal,
                    hazeState = hazeState,
                )
                PermissionStep.Scanning -> ProgressStep(
                    title = "Reading your screenshot graveyard",
                    detail = if (hasPartialImageAccess(context) && !hasImagePermission(context)) {
                        "Limited-photo mode is on. Shotlist will scan the screenshots Android allowed and you can share more anytime."
                    } else {
                        "OCR is running locally. Useful events, deadlines, and codes will appear as they are found."
                    },
                    reveal = reveal,
                    hazeState = hazeState,
                )
                PermissionStep.Ready -> ReadyStep(
                    reveal = reveal,
                    onFinished = onFinished,
                    hazeState = hazeState,
                )
                PermissionStep.Denied -> DeniedStep(
                    onRetry = {
                        step = PermissionStep.Requesting
                        permissionLauncher.launch(requiredScreenshotPermissions().toTypedArray())
                    },
                    onShareOnly = onFinished,
                    hazeState = hazeState,
                )
            }
        }
    }
}

@Composable
private fun IntroStep(
    onContinue: () -> Unit,
    onShareOnly: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    val haptics = LocalHapticFeedback.current
    GlassPanel(hazeState = hazeState, cornerRadius = 38.dp, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.ImageSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text("Find the stuff you forgot", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            "Shotlist scans screenshots already on your phone, then turns flyers, deadlines, codes, and tickets into review cards.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(18.dp))
        PrivacyBullets()
        Spacer(Modifier.height(22.dp))
        FilledTonalButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scan my screenshots")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShareOnly()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Use share sheet only")
        }
    }
}

@Composable
private fun ProgressStep(
    title: String,
    detail: String,
    reveal: OnboardingReveal,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    GlassPanel(hazeState = hazeState, cornerRadius = 38.dp, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        Spacer(Modifier.height(20.dp))
        RevealRow(reveal)
        AnimatedVisibility(visible = reveal.hasWowMoment, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(14.dp))
                Text(
                    "The first useful finds are already flowing into Inbox.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ReadyStep(
    reveal: OnboardingReveal,
    onFinished: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    val haptics = LocalHapticFeedback.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val iconScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.62f,
        animationSpec = spring(),
        label = "ready-pop",
    )
    GlassPanel(hazeState = hazeState, cornerRadius = 38.dp, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Outlined.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
        )
        Spacer(Modifier.height(18.dp))
        Text("Your Inbox is alive", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            if (hasImagePermission(LocalContext.current)) {
                "Shotlist will keep watching new screenshots and only nudge when it sees something time-bound or useful."
            } else {
                "Shotlist is ready in limited mode. Share screenshots into the app whenever Android does not allow background access."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(18.dp))
        RevealRow(reveal)
        Spacer(Modifier.height(22.dp))
        FilledTonalButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFinished()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Show me the good stuff")
        }
    }
}

@Composable
private fun DeniedStep(
    onRetry: () -> Unit,
    onShareOnly: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    val haptics = LocalHapticFeedback.current
    GlassPanel(hazeState = hazeState, cornerRadius = 38.dp, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text("Screenshot access is off", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            "Automatic backfill needs image access. You can retry, or use the store-safe share-sheet mode.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        )
        Spacer(Modifier.height(22.dp))
        FilledTonalButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onRetry()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Try permission again")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShareOnly()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue with share sheet")
        }
    }
}

@Composable
private fun PrivacyBullets() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrivacyBullet(icon = Icons.Outlined.CloudOff, text = "Screenshot contents stay on this device.")
        PrivacyBullet(icon = Icons.Outlined.Security, text = "Calendar entries happen only after your tap.")
        PrivacyBullet(icon = Icons.Outlined.NotificationsActive, text = "New screenshots can become useful reminders.")
    }
}

@Composable
private fun PrivacyBullet(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
    }
}

@Composable
private fun RevealRow(reveal: OnboardingReveal) {
    val screenshotsRead by animateIntAsState(
        targetValue = reveal.screenshotsRead,
        animationSpec = tween(durationMillis = 700),
        label = "screenshots-read",
    )
    val suggestedActions by animateIntAsState(
        targetValue = reveal.suggestedActions,
        animationSpec = tween(durationMillis = 700),
        label = "suggested-actions",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RevealPill("$screenshotsRead read")
            RevealPill("$suggestedActions useful")
        }
        RevealPill("Screenshot contents stay on this device")
    }
}

@Composable
private fun RevealPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * Onboarding asks only for access needed to scan screenshots. Notification access is
 * deferred until a future reminder control can request it in context.
 */
private fun requiredScreenshotPermissions(): List<String> =
    buildList {
        addAll(requiredImagePermissions())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }

private fun requiredImagePermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasImagePermission(context: Context): Boolean =
    requiredImagePermissions().all { hasPermission(context, it) }

private fun hasUsableImageAccess(context: Context): Boolean =
    hasImagePermission(context) || hasPartialImageAccess(context)

private fun hasPartialImageAccess(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
