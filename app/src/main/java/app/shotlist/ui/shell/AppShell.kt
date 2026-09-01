package app.shotlist.ui.shell

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WifiPassword
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.actions.ShotlistActions
import app.shotlist.data.ShotlistDb
import app.shotlist.diag.Diag
import app.shotlist.engine.EngineApi
import app.shotlist.onboarding.OnboardingFlow
import app.shotlist.ui.glass.GlassBackdrop
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.glass.glassBackgroundBrush
import app.shotlist.ui.you.YouScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

private enum class Tab(val label: String, val icon: ImageVector) {
    Inbox("Inbox", Icons.Outlined.Inbox),
    Scan("Scan", Icons.Outlined.CameraAlt),
    Track("Track", Icons.Outlined.CalendarMonth),
    You("You", Icons.Outlined.Person),
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppShell() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember(context) {
        context.getSharedPreferences("shotlist_onboarding", android.content.Context.MODE_PRIVATE)
    }
    var onboardingComplete by rememberSaveable {
        mutableStateOf(prefs.getBoolean("complete", false))
    }

    if (!onboardingComplete) {
        OnboardingFlow(
            onFinished = {
                prefs.edit().putBoolean("complete", true).apply()
                onboardingComplete = true
            },
        )
        return
    }

    val hazeState = remember { HazeState() }
    val db = remember(context) { ShotlistDb.get(context) }
    val scope = rememberCoroutineScope()
    val findings by db.findings().inbox().collectAsState(initial = emptyList())
    val shotCount by db.shots().count().collectAsState(initial = 0)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var imageAccessGranted by remember { mutableStateOf(hasScreenshotAccess(context)) }
    var autoScanEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("auto_scan", true))
    }
    val actions = remember(findings) {
        findings.map { it.toShotlistAction() }
    }
    val accessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        imageAccessGranted = hasScreenshotAccess(context)
        if (imageAccessGranted) {
            EngineApi.backfill(context)
            EngineApi.startObserving(context)
            successMessage = "Looking for the useful stuff"
        }
    }

    fun setState(action: ShotlistAction, state: String) {
        action.findingId ?: return
        scope.launch {
            db.findings().setState(action.findingId, state)
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(1_800)
            successMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glassBackgroundBrush())
            .hazeSource(state = hazeState),
    ) {
        GlassBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            TopGlassBar(hazeState = hazeState)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = Tab.entries[selected],
                transitionSpec = { fadeIn() + scaleIn(initialScale = 0.98f) togetherWith fadeOut() + scaleOut(targetScale = 0.98f) },
                label = "tab-content",
                modifier = Modifier.weight(1f),
            ) { tab ->
                when (tab) {
                    Tab.Inbox -> InboxScreen(
                        actions = actions,
                        scannedCount = shotCount,
                        hasScreenshotAccess = imageAccessGranted,
                        hazeState = hazeState,
                        onRequestAccess = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (hasScreenshotAccess(context)) {
                                imageAccessGranted = true
                                EngineApi.backfill(context)
                                EngineApi.startObserving(context)
                                successMessage = "Fresh scan started"
                            } else {
                                accessLauncher.launch(screenshotPermissions())
                            }
                        },
                        onAccept = { action ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            successMessage = "Done — nice catch"
                            when (action.kind) {
                                ActionKind.Event, ActionKind.Deadline -> {
                                    context.startActivity(ShotlistActions.calendarInsertIntent(action))
                                    setState(action, "ACCEPTED")
                                }
                                ActionKind.Code -> {
                                    ShotlistActions.copyCode(context, action)
                                    setState(action, "ACCEPTED")
                                }
                                ActionKind.Place -> {
                                    ShotlistActions.mapSearchIntent(action)?.let(context::startActivity)
                                    setState(action, "ACCEPTED")
                                }
                                ActionKind.Product, ActionKind.Recipe, ActionKind.Noise -> setState(action, "ACCEPTED")
                            }
                        },
                        onSnooze = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            successMessage = "Tucked away for later"
                            setState(it, "SNOOZED")
                        },
                        onDismiss = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            successMessage = "Cleared out"
                            setState(it, "DISMISSED")
                        },
                    )
                    Tab.Scan -> ModulePlaceholder(
                        hazeState = hazeState,
                        icon = Icons.Outlined.CameraAlt,
                        title = "Scan",
                        subtitle = "Food calories, plants, labels, anything — module slot ready.",
                        badge = "Next module",
                    )
                    Tab.Track -> ModulePlaceholder(
                        hazeState = hazeState,
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Track",
                        subtitle = "Cycle, habits, streaks. Private and local-first.",
                        badge = "Platform",
                    )
                    Tab.You -> YouScreen(
                        hazeState = hazeState,
                        screenshotsChecked = shotCount,
                        thingsReady = actions.size,
                        imageAccessGranted = imageAccessGranted,
                        autoScanEnabled = autoScanEnabled,
                        onAutoScanChanged = { enabled ->
                            autoScanEnabled = enabled
                            prefs.edit().putBoolean("auto_scan", enabled).apply()
                            if (enabled) EngineApi.startObserving(context) else EngineApi.stopObserving()
                            successMessage = if (enabled) "Watching new screenshots" else "Auto-scan paused"
                        },
                        onOpenVault = {
                            val keyguard = context.getSystemService(KeyguardManager::class.java)
                            val intent = keyguard?.createConfirmDeviceCredentialIntent(
                                "Unlock Shotlist Vault",
                                "Sensitive finds stay behind your screen lock",
                            )
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                successMessage = "Set up a screen lock to use Vault"
                            }
                        },
                        onShareBugReport = {
                            context.startActivity(Diag.shareIntent(context))
                        },
                        onDeleteAllData = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            EngineApi.stopObserving()
                            WorkManager.getInstance(context).cancelAllWork()
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    db.clearAllTables()
                                    context.filesDir.resolve("shared").deleteRecursively()
                                    context.filesDir.resolve("diag.log").delete()
                                }
                                context.getSharedPreferences("shotlist_engine", Context.MODE_PRIVATE)
                                    .edit().clear().apply()
                                prefs.edit().clear().apply()
                                onboardingComplete = false
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassNavBar(
                selected = selected,
                onSelected = {
                    if (selected != it) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = it
                    }
                },
                hazeState = hazeState,
            )
        }
        AnimatedVisibility(
            visible = successMessage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.78f, animationSpec = spring()),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 94.dp),
        ) {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(successMessage.orEmpty(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TopGlassBar(hazeState: dev.chrisbanes.haze.HazeState) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Shotlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "Screenshots in. Life out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            Text(
                "LOCAL",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun InboxScreen(
    actions: List<ShotlistAction>,
    scannedCount: Int,
    hasScreenshotAccess: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onRequestAccess: () -> Unit,
    onAccept: (ShotlistAction) -> Unit,
    onSnooze: (ShotlistAction) -> Unit,
    onDismiss: (ShotlistAction) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (actions.isEmpty()) {
            item {
                EmptyInboxCard(
                    scannedCount = scannedCount,
                    hasScreenshotAccess = hasScreenshotAccess,
                    onRequestAccess = onRequestAccess,
                    hazeState = hazeState,
                )
            }
        } else {
            item {
                HeroCard(
                    scannedCount = scannedCount,
                    actionCount = actions.size,
                    hazeState = hazeState,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val eventCount = actions.count { it.kind == ActionKind.Event }
                    val codeCount = actions.count { it.kind == ActionKind.Code }
                    val deadlineCount = actions.count { it.kind == ActionKind.Deadline }
                    if (eventCount > 0) StatPill("$eventCount events", Color(0xFF8FB5FF))
                    if (codeCount > 0) StatPill("$codeCount codes", Color(0xFFA6F4E6))
                    if (deadlineCount > 0) StatPill("$deadlineCount deadlines", Color(0xFFFFC978))
                }
            }
            items(actions, key = { it.id }) { action ->
                ActionCard(
                    action = action,
                    hazeState = hazeState,
                    onAccept = { onAccept(action) },
                    onSnooze = { onSnooze(action) },
                    onDismiss = { onDismiss(action) },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    scannedCount: Int,
    actionCount: Int,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    val displayActionCount by animateIntAsState(
        targetValue = actionCount,
        animationSpec = tween(durationMillis = 650),
        label = "action-count",
    )
    val displayScannedCount by animateIntAsState(
        targetValue = scannedCount,
        animationSpec = tween(durationMillis = 700),
        label = "hero-count",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(15.dp),
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .weight(1.08f)
                .fillMaxHeight(),
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                displayActionCount.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, lineHeight = 46.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                "ready for you",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .weight(0.92f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniMetric(
                value = displayScannedCount.toString(),
                label = "screenshots checked",
                accent = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                modifier = Modifier.weight(1f),
            )
            MiniMetric(
                value = "100%",
                label = "on your phone",
                accent = MaterialTheme.colorScheme.secondary,
                hazeState = hazeState,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MiniMetric(
    value: String,
    label: String,
    accent: Color,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        accent = accent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = accent,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyInboxCard(
    scannedCount: Int,
    hasScreenshotAccess: Boolean,
    onRequestAccess: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    val displayCount by animateIntAsState(
        targetValue = scannedCount,
        animationSpec = tween(durationMillis = 700),
        label = "empty-count",
    )
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 34.dp,
        contentPadding = PaddingValues(16.dp),
        accent = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Screenshot. Forget. Done.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenshotCount(displayCount)
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasScreenshotAccess) {
                "All clear. Take a screenshot and Shotlist will catch the useful part for you."
            } else {
                "Give access once. Dates, codes, and plans become taps instead of scavenger hunts."
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        )
        EmptyOrbitIllustration()
        FilledTonalButton(
            onClick = onRequestAccess,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (hasScreenshotAccess) "Scan again" else "Grant screenshot access",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Private by design · screenshot contents stay here",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.86f),
        )
    }
}

@Composable
private fun ScreenshotCount(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, lineHeight = 42.sp),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "screenshots\nchecked",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, lineHeight = 17.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun EmptyOrbitIllustration() {
    val transition = rememberInfiniteTransition(label = "empty-orbit")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit-angle",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val orbitRadius = 43.dp.toPx()
        drawCircle(primary.copy(alpha = 0.08f), radius = 56.dp.toPx(), center = center)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = orbitRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        repeat(3) { index ->
            val radians = Math.toRadians((rotation + index * 120f).toDouble())
            val dotCenter = Offset(
                x = center.x + cos(radians).toFloat() * orbitRadius,
                y = center.y + sin(radians).toFloat() * orbitRadius,
            )
            drawCircle(
                color = if (index == 1) secondary else primary,
                radius = if (index == 1) 7.dp.toPx() else 5.dp.toPx(),
                center = dotCenter,
            )
        }
        drawCircle(secondary.copy(alpha = 0.20f), radius = 24.dp.toPx(), center = center)
        drawCircle(primary, radius = 8.dp.toPx(), center = center)
    }
}

@Composable
private fun StatPill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    action: ShotlistAction,
    hazeState: dev.chrisbanes.haze.HazeState,
    onAccept: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = actionColor(action.kind)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onDismiss()
                SwipeToDismissBoxValue.EndToStart -> onSnooze()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            value != SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Text(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) "Done" else "Later",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(16.dp),
            accent = accent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                KindIcon(action.kind, accent)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        action.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${kindLabel(action.kind)} · From screenshot",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.copy(alpha = 0.92f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onAccept,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = accent.copy(alpha = 0.20f),
                                contentColor = accent,
                            ),
                        ) {
                            Text(primaryCta(action.kind), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KindIcon(kind: ActionKind, accent: Color) {
    val icon = when (kind) {
        ActionKind.Event, ActionKind.Deadline -> Icons.Outlined.CalendarMonth
        ActionKind.Product -> Icons.Outlined.ShoppingBag
        ActionKind.Place -> Icons.Outlined.Map
        ActionKind.Code -> Icons.Outlined.WifiPassword
        ActionKind.Recipe -> Icons.Outlined.AutoAwesome
        ActionKind.Noise -> Icons.Outlined.Inbox
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(accent.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent)
    }
}

private fun actionColor(kind: ActionKind): Color = when (kind) {
    ActionKind.Event -> Color(0xFF8EAAFF)
    ActionKind.Deadline -> Color(0xFFFFBE63)
    ActionKind.Product -> Color(0xFFFF79C9)
    ActionKind.Place -> Color(0xFF70F0D0)
    ActionKind.Code -> Color(0xFF58D8FF)
    ActionKind.Recipe -> Color(0xFFFF9D72)
    ActionKind.Noise -> Color(0xFFB5BAD0)
}

private fun kindLabel(kind: ActionKind): String = when (kind) {
    ActionKind.Event -> "Event"
    ActionKind.Deadline -> "Deadline"
    ActionKind.Product -> "Product"
    ActionKind.Place -> "Place"
    ActionKind.Code -> "Code"
    ActionKind.Recipe -> "Recipe"
    ActionKind.Noise -> "Saved"
}

private fun primaryCta(kind: ActionKind): String = when (kind) {
    ActionKind.Event -> "Add"
    ActionKind.Deadline -> "Remind"
    ActionKind.Product -> "Track"
    ActionKind.Place -> "Open"
    ActionKind.Code -> "Copy"
    ActionKind.Recipe -> "List"
    ActionKind.Noise -> "Archive"
}

@Composable
private fun ModulePlaceholder(
    hazeState: dev.chrisbanes.haze.HazeState,
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 38.dp,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 36.sp),
            fontWeight = FontWeight.Black,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            badge,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun GlassNavBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Tab.entries.forEachIndexed { index, tab ->
                val isSelected = selected == index
                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(),
                    label = "tab-pill",
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    animationSpec = spring(),
                    label = "tab-scale",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .background(pillColor, RoundedCornerShape(24.dp))
                        .clickable { onSelected(index) }
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private fun screenshotPermissions(): Array<String> =
    buildList {
        add(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }.toTypedArray()

private fun hasScreenshotAccess(context: Context): Boolean {
    val fullPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasFullAccess = ContextCompat.checkSelfPermission(
        context,
        fullPermission,
    ) == PackageManager.PERMISSION_GRANTED
    val hasPartialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) == PackageManager.PERMISSION_GRANTED
    return hasFullAccess || hasPartialAccess
}
