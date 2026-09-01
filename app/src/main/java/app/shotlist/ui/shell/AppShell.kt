package app.shotlist.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WifiPassword
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.actions.ShotlistActions
import app.shotlist.data.ShotlistDb
import app.shotlist.onboarding.OnboardingFlow
import app.shotlist.ui.glass.GlassBackdrop
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.glass.glassBackgroundBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    Inbox("Inbox", Icons.Outlined.Inbox),
    Scan("Scan", Icons.Outlined.CameraAlt),
    Track("Track", Icons.Outlined.Favorite),
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
    val actions = remember(findings) {
        findings.map { it.toShotlistAction() }
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
                        hazeState = hazeState,
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
                        icon = Icons.Outlined.Favorite,
                        title = "Track",
                        subtitle = "Cycle, habits, streaks. Private and local-first.",
                        badge = "Platform",
                    )
                    Tab.You -> ModulePlaceholder(
                        hazeState = hazeState,
                        icon = Icons.Outlined.Lock,
                        title = "You",
                        subtitle = "Vault, privacy dashboard, settings, and local-only controls.",
                        badge = "Screenshot contents stay on this device",
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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Shotlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "Your screenshots become things that happen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f))
        }
    }
}

@Composable
private fun InboxScreen(
    actions: List<ShotlistAction>,
    scannedCount: Int,
    hazeState: dev.chrisbanes.haze.HazeState,
    onAccept: (ShotlistAction) -> Unit,
    onSnooze: (ShotlistAction) -> Unit,
    onDismiss: (ShotlistAction) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            HeroCard(
                scannedCount = scannedCount,
                actionCount = actions.size,
                hazeState = hazeState,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("${actions.count { it.kind == ActionKind.Event }} events")
                StatPill("${actions.count { it.kind == ActionKind.Code }} codes")
                StatPill("${actions.count { it.kind == ActionKind.Deadline }} deadlines")
            }
        }
        if (actions.isEmpty()) {
            item {
                EmptyInboxCard(hazeState = hazeState)
            }
        } else {
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
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 36.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(36.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
    ) {
        Text(
            "$scannedCount screenshots read",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (actionCount == 0) {
                "When onboarding scans your screenshot graveyard, useful events and deadlines appear here."
            } else {
                "Shotlist found $actionCount useful action${if (actionCount == 1) "" else "s"} hiding in the graveyard."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (actionCount == 0) "Waiting for your next screenshot" else "Ready when you are",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun EmptyInboxCard(hazeState: dev.chrisbanes.haze.HazeState) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 30.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Nothing actionable yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Grant screenshot access or share a screenshot to start. " +
                "Screenshot contents stay on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun StatPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(999.dp))
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDismiss()
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
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 30.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                KindIcon(action.kind)
                Spacer(Modifier.width(14.dp))
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
                        "${(action.confidence * 100).toInt()}% · ${action.source}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onAccept) {
                            Text(primaryCta(action.kind))
                        }
                        OutlinedButton(onClick = onSnooze) {
                            Text("Snooze")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KindIcon(kind: ActionKind) {
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
            .size(46.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        Spacer(Modifier.height(18.dp))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .background(pillColor, RoundedCornerShape(24.dp))
                        .clickable { onSelected(index) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                    AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
                        Row {
                            Spacer(Modifier.width(6.dp))
                            Text(tab.label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
