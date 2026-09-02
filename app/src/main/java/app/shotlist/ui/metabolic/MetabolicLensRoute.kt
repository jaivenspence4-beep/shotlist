package app.shotlist.ui.metabolic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.shotlist.data.GlucoseMoment
import app.shotlist.data.GlucoseSample
import app.shotlist.data.ShotlistDb
import app.shotlist.health.android.HealthConnectGateway
import app.shotlist.health.android.HealthPermissionController
import app.shotlist.health.android.HealthPermissionDecision
import app.shotlist.health.api.GlucoseStory
import app.shotlist.health.api.GlucoseSync
import app.shotlist.health.api.GlucoseUnit
import app.shotlist.health.api.GlucoseUnits
import app.shotlist.health.api.HealthAvailability
import app.shotlist.health.api.RoomGlucoseStore
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.theme.ShotlistTheme
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class StoryWindow(val label: String, val millis: Long) {
    DAY("24h", GlucoseStory.DAY_MS),
    WEEK("7d", 7 * GlucoseStory.DAY_MS),
    MONTH("30d", 30 * GlucoseStory.DAY_MS),
}

@Composable
fun MetabolicLensRoute(
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { ShotlistDb.get(context) }
    val dao = remember(database) { database.glucose() }
    val gateway = remember(context) { HealthConnectGateway(context) }
    val sync = remember(gateway, dao) { GlucoseSync(gateway, RoomGlucoseStore(dao)) }
    val permissions = remember(context, gateway) { HealthPermissionController(context, gateway) }
    val permissionContract = remember(permissions) { permissions.requestContract() }
    val refreshMutex = remember { Mutex() }

    var syncResult by remember { mutableStateOf<GlucoseSync.Result?>(null) }
    var checking by remember { mutableStateOf(true) }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var storyWindow by rememberSaveable { mutableStateOf(StoryWindow.DAY) }
    var permissionDecision by remember { mutableStateOf<HealthPermissionDecision?>(null) }
    var sourceChoices by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSourceChooser by remember { mutableStateOf(false) }
    var showMomentSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDisconnect by remember { mutableStateOf(false) }

    val syncState by dao.syncStateFlow().collectAsState(initial = null)
    val selectedOrigin = syncState?.selectedOrigin
    val from = clockNow - storyWindow.millis
    val samplesFlow = remember(dao, selectedOrigin, from, clockNow) {
        selectedOrigin?.let { dao.samplesBetween(it, from, clockNow) } ?: flowOf(emptyList())
    }
    val samples by samplesFlow.collectAsState(initial = emptyList())
    val latestFlow = remember(dao, selectedOrigin) {
        selectedOrigin?.let(dao::latest) ?: flowOf(null)
    }
    val latest by latestFlow.collectAsState(initial = null)
    val momentsFlow = remember(dao, from, clockNow) { dao.momentsBetween(from, clockNow) }
    val moments by momentsFlow.collectAsState(initial = emptyList())
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val displayUnit = GlucoseUnits.resolve(syncState?.displayUnit, locale)

    suspend fun refreshStory() {
        refreshMutex.withLock {
            checking = true
            clockNow = System.currentTimeMillis()
            val result = runCatching { sync.refresh() }
                .getOrElse { GlucoseSync.Result.Failed("read") }
            syncResult = result
            when (result) {
                is GlucoseSync.Result.NeedsSource -> {
                    sourceChoices = result.origins
                    showSourceChooser = true
                }
                GlucoseSync.Result.NoAccess -> Unit
                else -> permissionDecision = null
            }
            checking = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(permissionContract) { granted ->
        permissionDecision = permissions.recordPromptResult(granted)
        if (permissionDecision == HealthPermissionDecision.GRANTED) {
            scope.launch { refreshStory() }
        }
    }

    DisposableEffect(activity) {
        val window = activity?.window
        val wasSecure = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { refreshStory() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { refreshStory() }
    BackHandler(onBack = onClose)

    val hasHistory = latest != null
    val showDashboard = hasHistory ||
        (selectedOrigin != null && syncResult is GlucoseSync.Result.Synced)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            RouteHeader(
                checking = checking,
                onClose = onClose,
                onRefresh = { scope.launch { refreshStory() } },
                onSettings = { showSettings = true },
            )
        }

        if (showDashboard) {
            item {
                ConnectionBanner(
                    result = syncResult,
                    paused = syncState?.paused == true,
                    onConnect = { permissionLauncher.launch(permissions.requiredPermissions) },
                    onManageAccess = { context.startSafely(permissions.manageAccessIntent()) },
                    onResume = {
                        scope.launch {
                            sync.setPaused(false)
                            refreshStory()
                        }
                    },
                )
            }
            item {
                StoryDashboard(
                    hazeState = hazeState,
                    samples = samples,
                    latest = latest,
                    moments = moments,
                    storyWindow = storyWindow,
                    from = from,
                    until = clockNow,
                    unit = displayUnit,
                    locale = locale,
                    sourceLabel = selectedOrigin?.let { context.sourceLabel(it) }.orEmpty(),
                    onWindowChanged = { storyWindow = it },
                    onAddMoment = { showMomentSheet = true },
                    onDeleteMoment = { moment -> scope.launch { dao.deleteMoment(moment.id) } },
                )
            }
        } else {
            item {
                when (val result = syncResult) {
                    is GlucoseSync.Result.Unavailable -> AvailabilityPanel(
                        hazeState = hazeState,
                        availability = result.availability,
                        onInstallOrUpdate = {
                            context.startSafely(permissions.installOrUpdateIntent())
                        },
                    )
                    GlucoseSync.Result.NoSource -> EmptyStoryPanel(
                        hazeState = hazeState,
                        onManageAccess = { context.startSafely(permissions.manageAccessIntent()) },
                    )
                    GlucoseSync.Result.Paused -> PausedPanel(
                        hazeState = hazeState,
                        onResume = {
                            scope.launch {
                                sync.setPaused(false)
                                refreshStory()
                            }
                        },
                    )
                    is GlucoseSync.Result.Failed -> ErrorPanel(
                        hazeState = hazeState,
                        onTryAgain = { scope.launch { refreshStory() } },
                    )
                    else -> SetupExplainerPanel(
                        hazeState = hazeState,
                        checking = checking,
                        decision = permissionDecision,
                        onConnect = { permissionLauncher.launch(permissions.requiredPermissions) },
                        onManageAccess = { context.startSafely(permissions.manageAccessIntent()) },
                    )
                }
            }
        }
    }

    if (showSourceChooser) {
        SourceChooserSheet(
            hazeState = hazeState,
            sources = sourceChoices,
            sourceLabel = context::sourceLabel,
            onSelect = { origin ->
                showSourceChooser = false
                scope.launch {
                    sync.selectSource(origin)
                    refreshStory()
                }
            },
            onDismiss = { showSourceChooser = false },
        )
    }
    if (showMomentSheet) {
        AddMomentSheet(
            hazeState = hazeState,
            onSave = { kind, occurredAt, note ->
                scope.launch {
                    dao.insertMoment(
                        GlucoseMoment(
                            occurredAt = occurredAt,
                            kind = kind,
                            note = note.takeIf(String::isNotBlank),
                        ),
                    )
                    showMomentSheet = false
                }
            },
            onDismiss = { showMomentSheet = false },
        )
    }
    if (showSettings) {
        MetabolicSettingsSheet(
            hazeState = hazeState,
            paused = syncState?.paused == true,
            unit = displayUnit,
            source = selectedOrigin?.let { context.sourceLabel(it) },
            onPausedChanged = { paused ->
                scope.launch {
                    sync.setPaused(paused)
                    if (!paused) refreshStory()
                }
            },
            onUnitChanged = { unit -> scope.launch { sync.setDisplayUnit(unit) } },
            onManageAccess = { context.startSafely(permissions.manageAccessIntent()) },
            onDisconnect = { showDisconnect = true },
            onDismiss = { showSettings = false },
        )
    }
    if (showDisconnect) {
        DisconnectDialog(
            hazeState = hazeState,
            onKeep = {
                scope.launch {
                    sync.disconnect(keepHistory = true)
                    syncResult = GlucoseSync.Result.NoAccess
                    showDisconnect = false
                    showSettings = false
                }
            },
            onDelete = {
                scope.launch {
                    sync.disconnect(keepHistory = false)
                    syncResult = GlucoseSync.Result.NoAccess
                    showDisconnect = false
                    showSettings = false
                }
            },
            onDismiss = { showDisconnect = false },
        )
    }
}

@Composable
private fun RouteHeader(
    checking: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Close Metabolic Lens")
        }
        Column(Modifier.weight(1f)) {
            Text("METABOLIC LENS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            Text("Your private glucose story", fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        if (checking) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh glucose story")
            }
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "Metabolic Lens settings")
        }
    }
}

@Composable
private fun SetupExplainerPanel(
    hazeState: HazeState,
    checking: Boolean,
    decision: HealthPermissionDecision?,
    onConnect: () -> Unit,
    onManageAccess: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        accent = Color(0xFF75E6C8),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, tint = Color(0xFF75E6C8))
        Spacer(Modifier.height(12.dp))
        Text("See the shape of your day", fontWeight = FontWeight.Black, fontSize = 25.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lingo sends five-minute sensor-glucose readings through Health Connect about three hours later. Shotlist reads them only when you open this screen and stores its copy on this phone unless you choose to export it.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Not live. Not medical advice.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        )
        decision?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                if (it == HealthPermissionDecision.MANAGE_ACCESS_REQUIRED) {
                    "Open Health Connect to allow access when you are ready."
                } else {
                    "Access was not allowed. Nothing was read."
                },
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
        if (decision == HealthPermissionDecision.MANAGE_ACCESS_REQUIRED) {
            FilledTonalButton(onClick = onManageAccess, modifier = Modifier.fillMaxWidth()) {
                Text("Manage access")
            }
        } else {
            Button(onClick = onConnect, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Connect through Health Connect")
            }
        }
    }
}

@Composable
private fun AvailabilityPanel(
    hazeState: HazeState,
    availability: HealthAvailability,
    onInstallOrUpdate: () -> Unit,
) {
    val canInstall = availability == HealthAvailability.NOT_INSTALLED ||
        availability == HealthAvailability.NEEDS_UPDATE
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text(
            when (availability) {
                HealthAvailability.NOT_INSTALLED -> "Health Connect is needed"
                HealthAvailability.NEEDS_UPDATE -> "Health Connect needs an update"
                else -> "Health Connect is unavailable"
            },
            fontWeight = FontWeight.Black,
            fontSize = 23.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (canInstall) {
                "Install or update the on-device provider, then return here."
            } else {
                "This device, Android version, or work profile cannot use this private tracker. The rest of Shotlist still works normally."
            },
        )
        if (canInstall) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onInstallOrUpdate, modifier = Modifier.fillMaxWidth()) {
                Text("Open provider page")
            }
        }
    }
}

@Composable
private fun EmptyStoryPanel(hazeState: HazeState, onManageAccess: () -> Unit) {
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("No readings yet", fontWeight = FontWeight.Black, fontSize = 23.sp)
        Spacer(Modifier.height(8.dp))
        Text("In Lingo, turn on Sync with Health Connect, then come back after data has arrived.")
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onManageAccess, modifier = Modifier.fillMaxWidth()) {
            Text("Manage access")
        }
    }
}

@Composable
private fun PausedPanel(hazeState: HazeState, onResume: () -> Unit) {
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Text("Sync is paused", fontWeight = FontWeight.Black, fontSize = 23.sp)
        Spacer(Modifier.height(8.dp))
        Text("Shotlist is not reading Health Connect. Resume whenever you want this story to update.")
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
            Text("Resume sync")
        }
    }
}

@Composable
private fun ErrorPanel(hazeState: HazeState, onTryAgain: () -> Unit) {
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Text("The story could not update", fontWeight = FontWeight.Black, fontSize = 23.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your saved readings are unchanged.")
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Try again")
        }
    }
}

@Composable
private fun ConnectionBanner(
    result: GlucoseSync.Result?,
    paused: Boolean,
    onConnect: () -> Unit,
    onManageAccess: () -> Unit,
    onResume: () -> Unit,
) {
    when {
        paused || result == GlucoseSync.Result.Paused -> StatusBanner(
            text = "Sync paused — saved history is still here.",
            action = "Resume",
            onAction = onResume,
        )
        result == GlucoseSync.Result.NoAccess -> StatusBanner(
            text = "Health Connect access is off. Saved history is still here.",
            action = "Connect",
            onAction = onConnect,
            secondaryAction = onManageAccess,
        )
        result is GlucoseSync.Result.Failed -> StatusBanner(
            text = "The latest update did not finish. Saved history is unchanged.",
        )
    }
}

@Composable
private fun StatusBanner(
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
    secondaryAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        action?.let {
            TextButton(onClick = onAction) { Text(it) }
        }
        secondaryAction?.let {
            IconButton(onClick = it) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Manage access")
            }
        }
    }
}

@Composable
private fun StoryDashboard(
    hazeState: HazeState,
    samples: List<GlucoseSample>,
    latest: GlucoseSample?,
    moments: List<GlucoseMoment>,
    storyWindow: StoryWindow,
    from: Long,
    until: Long,
    unit: GlucoseUnit,
    locale: Locale,
    sourceLabel: String,
    onWindowChanged: (StoryWindow) -> Unit,
    onAddMoment: () -> Unit,
    onDeleteMoment: (GlucoseMoment) -> Unit,
) {
    val summary = remember(samples) { GlucoseStory.summarize(samples) }
    val freshness = GlucoseStory.freshness(latest?.observedAt, until)
    val latestDateTime = latest?.observedAt?.let(::dateAndTime)
    val freshnessCopy = GlucoseStory.freshnessCopy(
        freshness = freshness,
        latestTime = latestDateTime?.second.orEmpty(),
        latestDate = latestDateTime?.first.orEmpty(),
    )
    val glucoseWord = GlucoseStory.glucoseWord(samples)

    GlassPanel(
        hazeState = hazeState,
        accent = Color(0xFF75E6C8),
        contentPadding = PaddingValues(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(glucoseWord.replaceFirstChar { it.uppercase(locale) }, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    freshnessCopy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
                if (sourceLabel.isNotBlank()) {
                    Text(
                        sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF75E6C8))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StoryWindow.entries.forEach { window ->
                FilterChip(
                    selected = storyWindow == window,
                    onClick = { onWindowChanged(window) },
                    label = { Text(window.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (samples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No readings in this view.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        } else {
            MetabolicChart(
                samples = samples,
                moments = moments,
                from = from,
                until = until,
                unit = unit,
                locale = locale,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile("Observed low", summary.lowMmol, unit, locale, Modifier.weight(1f))
            MetricTile("Median", summary.medianMmol, unit, locale, Modifier.weight(1f))
            MetricTile("Observed high", summary.highMmol, unit, locale, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${summary.count} readings · ${summary.gaps.size} data gaps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
    }

    Spacer(Modifier.height(14.dp))
    GlassPanel(
        hazeState = hazeState,
        accent = MaterialTheme.colorScheme.tertiary,
        contentPadding = PaddingValues(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Moments", fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    "Labels you add appear on the curve. They do not explain changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
            }
            FilledTonalButton(onClick = onAddMoment) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add")
            }
        }
        moments.takeLast(8).reversed().forEach { moment ->
            Spacer(Modifier.height(10.dp))
            MomentRow(moment = moment, onDelete = { onDeleteMoment(moment) })
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: Double?,
    unit: GlucoseUnit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f))
            .padding(10.dp),
    ) {
        Text(
            value?.let { GlucoseUnits.format(it, unit, locale) } ?: "—",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MomentRow(moment: GlucoseMoment, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(momentColor(moment.kind).copy(alpha = 0.10f))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(momentColor(moment.kind)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(momentLabel(moment.kind), fontWeight = FontWeight.Bold)
            Text(
                moment.note?.takeIf(String::isNotBlank) ?: formatMomentTime(moment.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete moment")
        }
    }
}

@Composable
private fun SourceChooserSheet(
    hazeState: HazeState,
    sources: List<String>,
    sourceLabel: (String) -> String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SecureMetabolicSheet(onDismissRequest = onDismiss) {
        GlassPanel(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 34.dp,
        ) {
            Text("Choose one glucose source", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(
                "Shotlist never silently mixes readings from different apps.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
            Spacer(Modifier.height(14.dp))
            sources.forEach { source ->
                FilledTonalButton(
                    onClick = { onSelect(source) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(sourceLabel(source), fontWeight = FontWeight.Bold)
                        Text(source, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Not now")
            }
        }
    }
}

@Composable
private fun AddMomentSheet(
    hazeState: HazeState,
    onSave: (kind: String, occurredAt: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf("MEAL") }
    var offsetHours by rememberSaveable { mutableStateOf(0) }
    var note by rememberSaveable { mutableStateOf("") }
    SecureMetabolicSheet(onDismissRequest = onDismiss) {
        GlassPanel(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 34.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a moment", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Text(
                "A label on your timeline, not an explanation for a change.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("MEAL", "EXERCISE", "SLEEP", "NOTE").forEach { option ->
                    FilterChip(
                        selected = kind == option,
                        onClick = { kind = option },
                        label = { Text(momentLabel(option)) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 3).forEach { hours ->
                    FilterChip(
                        selected = offsetHours == hours,
                        onClick = { offsetHours = hours },
                        label = { Text(if (hours == 0) "Now" else "${hours}h ago") },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(160) },
                label = { Text("Optional note") },
                supportingText = { Text("${note.length}/160") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val occurredAt = System.currentTimeMillis() - offsetHours * 60L * 60 * 1000
                    onSave(kind, occurredAt, note.trim())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add to story")
            }
        }
    }
}

@Composable
private fun MetabolicSettingsSheet(
    hazeState: HazeState,
    paused: Boolean,
    unit: GlucoseUnit,
    source: String?,
    onPausedChanged: (Boolean) -> Unit,
    onUnitChanged: (GlucoseUnit) -> Unit,
    onManageAccess: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    SecureMetabolicSheet(onDismissRequest = onDismiss) {
        GlassPanel(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 34.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Metabolic Lens settings", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sync with Health Connect", fontWeight = FontWeight.Bold)
                    Text(
                        if (paused) "Paused" else "Only while this screen is in use",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = !paused, onCheckedChange = { onPausedChanged(!it) })
            }
            Spacer(Modifier.height(14.dp))
            Text("Display units", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlucoseUnit.entries.forEach { option ->
                    FilterChip(
                        selected = unit == option,
                        onClick = { onUnitChanged(option) },
                        label = { Text(GlucoseUnits.label(option)) },
                    )
                }
            }
            source?.let {
                Spacer(Modifier.height(14.dp))
                Text("Source", fontWeight = FontWeight.Bold)
                Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f))
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onManageAccess, modifier = Modifier.fillMaxWidth()) {
                Text("Manage Health Connect access")
            }
            TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun DisconnectDialog(
    hazeState: HazeState,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    SecureMetabolicDialog(onDismissRequest = onDismiss) {
        GlassPanel(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
        ) {
            Text("Disconnect Health Connect?", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("Both choices revoke Shotlist access. Choose whether the history already copied to this phone stays here.")
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) {
                Text("Keep local history")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete local history and moments")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel")
            }
        }
    }
}

private fun dateAndTime(epochMillis: Long): Pair<String, String> {
    val value = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return value.format(DateTimeFormatter.ofPattern("MMM d")) to
        value.format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun formatMomentTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d · h:mm a"))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.startSafely(intent: android.content.Intent) {
    runCatching { startActivity(intent) }
}

private fun Context.sourceLabel(packageName: String): String = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(packageName, 0)
    }
    packageManager.getApplicationLabel(info).toString()
}.getOrElse {
    packageName.substringAfterLast('.').replaceFirstChar { char -> char.uppercase() }
}

@Preview(name = "Metabolic setup", showBackground = true)
@Composable
private fun SetupPreview() {
    ShotlistTheme(darkTheme = true) {
        SetupExplainerPanel(
            hazeState = remember { HazeState() },
            checking = false,
            decision = null,
            onConnect = {},
            onManageAccess = {},
        )
    }
}

@Preview(name = "Metabolic empty", showBackground = true)
@Composable
private fun EmptyPreview() {
    ShotlistTheme(darkTheme = true) {
        EmptyStoryPanel(remember { HazeState() }, onManageAccess = {})
    }
}

@Preview(name = "Metabolic unavailable", showBackground = true)
@Composable
private fun UnavailablePreview() {
    ShotlistTheme(darkTheme = true) {
        AvailabilityPanel(
            hazeState = remember { HazeState() },
            availability = HealthAvailability.UNSUPPORTED,
            onInstallOrUpdate = {},
        )
    }
}

@Preview(name = "Metabolic paused", showBackground = true)
@Composable
private fun PausedPreview() {
    ShotlistTheme(darkTheme = true) {
        PausedPanel(remember { HazeState() }, onResume = {})
    }
}
