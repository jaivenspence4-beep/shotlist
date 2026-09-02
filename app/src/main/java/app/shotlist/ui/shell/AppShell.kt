package app.shotlist.ui.shell

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.WorkManager
import app.shotlist.MainActivity
import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.actions.ShotlistActions
import app.shotlist.data.Finding
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb
import app.shotlist.diag.Diag
import app.shotlist.engine.EngineApi
import app.shotlist.entitlement.Entitlement
import app.shotlist.onboarding.OnboardingFlow
import app.shotlist.onboarding.OnboardingPreferences
import app.shotlist.ui.collections.CollectionTarget
import app.shotlist.ui.collections.CollectionsScreen
import app.shotlist.ui.collections.PinToCollectionSheet
import app.shotlist.ui.detail.FindingDetailSheet
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.glass.glassBackgroundBrush
import app.shotlist.ui.liquidbg.LiquidBackground
import app.shotlist.ui.paywall.ProPreviewReason
import app.shotlist.ui.paywall.ProPreviewSheet
import app.shotlist.ui.quests.DailyQuestsCard
import app.shotlist.ui.quests.LevelProgress
import app.shotlist.ui.quests.LevelUpBurst
import app.shotlist.ui.quests.QuestDashboard
import app.shotlist.ui.quests.QuestLevelPill
import app.shotlist.ui.quests.rememberQuestDashboard
import app.shotlist.ui.recall.RecallScreen
import app.shotlist.ui.purge.ShatterScreen
import app.shotlist.ui.scan.ScanScreen
import app.shotlist.ui.share.ShareCardGenerator
import app.shotlist.ui.share.ShareTemplate
import app.shotlist.ui.share.ShareTemplatePickerSheet
import app.shotlist.ui.theme.LivingScene
import app.shotlist.ui.theme.ShotlistPalette
import app.shotlist.ui.theme.ShotlistTheme
import app.shotlist.ui.track.TrackScreen
import app.shotlist.ui.you.YouScreen
import app.shotlist.widget.ShotlistWidgets
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

private enum class Tab(val label: String, val icon: ImageVector) {
    Inbox("Inbox", Icons.Outlined.Inbox),
    Scan("Scan", Icons.Outlined.CameraAlt),
    Track("Track", Icons.Outlined.CalendarMonth),
    You("You", Icons.Outlined.Person),
}

private sealed interface PendingShare {
    data class FindingCard(val action: ShotlistAction) : PendingShare

    data class WeeklyCard(
        val found: Int,
        val acted: Int,
        val streak: Int,
        val topType: String,
    ) : PendingShare
}

@Composable
fun AppShell() {
    val context = LocalContext.current
    val appearancePrefs = remember(context) {
        context.getSharedPreferences("shotlist_onboarding", Context.MODE_PRIVATE)
    }
    var palette by remember {
        mutableStateOf(
            appearancePrefs.getString("palette", null)
                ?.let { runCatching { ShotlistPalette.valueOf(it) }.getOrNull() }
                ?: ShotlistPalette.COSMIC,
        )
    }
    var livingScene by remember {
        mutableStateOf(
            appearancePrefs.getString("living_scene", null)
                ?.let { runCatching { LivingScene.valueOf(it) }.getOrNull() }
                ?: when (appearancePrefs.getString("orb_style", null)) {
                    "HALO" -> LivingScene.NOISE_FIELD
                    "AURORA" -> LivingScene.FIREFLIES
                    else -> LivingScene.PHASE_BEAM
                },
        )
    }
    ShotlistTheme(palette = palette) {
        AppShellContent(
            palette = palette,
            livingScene = livingScene,
            onPaletteChanged = {
                palette = it
                appearancePrefs.edit().putString("palette", it.name).apply()
            },
            onLivingSceneChanged = {
                livingScene = it
                appearancePrefs.edit().putString("living_scene", it.name).apply()
            },
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppShellContent(
    palette: ShotlistPalette,
    livingScene: LivingScene,
    onPaletteChanged: (ShotlistPalette) -> Unit,
    onLivingSceneChanged: (LivingScene) -> Unit,
) {
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    val deepLinkFindingId = mainActivity?.deepLinkFindingId
    val deepLinkSerial = mainActivity?.deepLinkSerial ?: 0
    val requestedTab = mainActivity?.targetTab
    val openVaultRequested = mainActivity?.openVaultRequested == true
    val haptics = LocalHapticFeedback.current
    val prefs = remember(context) {
        context.getSharedPreferences("shotlist_onboarding", android.content.Context.MODE_PRIVATE)
    }
    val isDebugBuild = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    var onboardingComplete by rememberSaveable {
        mutableStateOf(prefs.getBoolean("complete", false))
    }

    if (!onboardingComplete) {
        OnboardingFlow(
            sceneKey = livingScene.sceneKey,
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
    val findingHistory by db.findings().byTypes(wrappedFindingTypes).collectAsState(initial = emptyList())
    val vaultedFindings by db.findings().vaulted().collectAsState(initial = emptyList())
    var entitlement by remember(prefs) {
        mutableStateOf(
            if (isDebugBuild) {
                Entitlement.fromStored(prefs.getString(Entitlement.DEBUG_PREF_KEY, null))
            } else {
                Entitlement.releaseDefault()
            },
        )
    }
    var proPreviewReason by remember { mutableStateOf<ProPreviewReason?>(null) }
    var pendingShare by remember { mutableStateOf<PendingShare?>(null) }
    val visibleVaultedFindings = remember(vaultedFindings, entitlement) {
        entitlement.vaultItemLimit?.let { vaultedFindings.take(it) } ?: vaultedFindings
    }
    val shotCount by db.shots().count().collectAsState(initial = 0)
    // Recall temporarily replaces the tab subtree; keep each tab's saveable UI state
    // so returning does not feel like relaunching that tab.
    val tabStateHolder = rememberSaveableStateHolder()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var recallOpen by rememberSaveable { mutableStateOf(false) }
    var shatterOpen by rememberSaveable { mutableStateOf(false) }
    var collectionsOpen by rememberSaveable { mutableStateOf(false) }

    // Time Machine (t68): one memory card max; feed opens full-bleed.
    var memoriesOpen by rememberSaveable { mutableStateOf(false) }
    var todayMemory by remember {
        mutableStateOf<app.shotlist.engine.memories.MemoryEngine.Memory?>(null)
    }
    LaunchedEffect(Unit) {
        todayMemory = runCatching {
            app.shotlist.engine.memories.MemoryEngine.todayMemory(context)
        }.getOrNull()
    }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var detailFinding by remember { mutableStateOf<Finding?>(null) }
    var detailShot by remember { mutableStateOf<Shot?>(null) }
    var pendingCollectionTarget by remember { mutableStateOf<CollectionTarget?>(null) }
    var imageAccessGranted by remember { mutableStateOf(hasScreenshotAccess(context)) }
    var autoScanEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean("auto_scan", true))
    }
    var vaultUnlocked by remember { mutableStateOf(false) }
    var pendingVaultAction by remember { mutableStateOf<ShotlistAction?>(null) }
    var pendingNotificationAction by remember { mutableStateOf<ShotlistAction?>(null) }
    var notificationPermissionResult by remember { mutableIntStateOf(0) }
    val dailyStreak = remember(prefs) { updateDailyStreak(prefs) }
    val weeklyStats = remember(findingHistory) { buildWeeklyStats(findingHistory) }
    val questDashboard = rememberQuestDashboard()
    LaunchedEffect(findings, vaultedFindings) {
        ShotlistWidgets.updateAll(context)
    }
    val activity = context as? FragmentActivity
    val biometricPrompt = remember(activity) {
        activity?.let {
            BiometricPrompt(
                it,
                ContextCompat.getMainExecutor(it),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        vaultUnlocked = true
                        successMessage = "Vault unlocked"
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            successMessage = errString.toString()
                        }
                    }
                },
            )
        }
    }
    val vaultPromptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Shotlist Vault")
            .setSubtitle("Sensitive finds stay behind your screen lock")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
    }
    LaunchedEffect(deepLinkSerial, biometricPrompt) {
        if (requestedTab == "you" && openVaultRequested) {
            biometricPrompt?.authenticate(vaultPromptInfo)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vaultUnlocked = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val focusAreas = remember(onboardingComplete) {
        OnboardingPreferences.read(context)
    }
    val actions = remember(findings, focusAreas) {
        findings.withIndex()
            .sortedWith(
                compareBy<IndexedValue<Finding>> {
                    OnboardingPreferences.findingPriority(it.value.type, focusAreas)
                }.thenBy { it.index },
            )
            .map { it.value.toShotlistAction() }
    }
    LaunchedEffect(detailFinding?.shotId) {
        detailShot = detailFinding?.let { db.shots().byId(it.shotId) }
    }
    LaunchedEffect(deepLinkSerial) {
        if (deepLinkSerial > 0) {
            recallOpen = false
            shatterOpen = false
        }
        selected = when (requestedTab) {
            "inbox" -> Tab.Inbox.ordinal
            "scan" -> Tab.Scan.ordinal
            "track" -> Tab.Track.ordinal
            "you" -> Tab.You.ordinal
            else -> if (deepLinkFindingId != null) Tab.Inbox.ordinal else selected
        }
        if (deepLinkFindingId != null) {
            detailShot = null
            detailFinding = db.findings().byId(deepLinkFindingId)
        }
    }
    val vaultedFindingIds = remember(vaultedFindings) {
        vaultedFindings.mapTo(mutableSetOf()) { it.id }
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
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        prefs.edit().putBoolean("notifications_asked", true).apply()
        notificationPermissionResult += 1
    }

    fun setState(action: ShotlistAction, state: String) {
        action.findingId ?: return
        scope.launch {
            db.findings().setState(action.findingId, state)
        }
    }

    fun showFindingDetail(finding: Finding) {
        detailShot = null
        detailFinding = finding
    }

    fun loadFindingDetail(action: ShotlistAction) {
        val findingId = action.findingId ?: return
        scope.launch {
            db.findings().byId(findingId)?.let(::showFindingDetail)
        }
    }

    fun performAction(action: ShotlistAction) {
        successMessage = "Done — nice catch"
        when (action.kind) {
            ActionKind.Event, ActionKind.Deadline -> {
                ShotlistActions.scheduleEventReminders(context, action)
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
            ActionKind.Link -> {
                ShotlistActions.openUrlIntent(action)?.let(context::startActivity)
                setState(action, "ACCEPTED")
            }
            ActionKind.Contact -> {
                context.startActivity(ShotlistActions.contactInsertIntent(action))
                setState(action, "ACCEPTED")
            }
            ActionKind.Product, ActionKind.Recipe, ActionKind.Noise -> setState(action, "ACCEPTED")
        }
    }

    fun requestPrimaryAction(action: ShotlistAction) {
        val shouldAskNotifications = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("notifications_asked", false)
        if (shouldAskNotifications) {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (action.findingId?.let(vaultedFindingIds::contains) == true && !vaultUnlocked) {
            pendingVaultAction = action
            biometricPrompt?.authenticate(vaultPromptInfo)
                ?: run {
                    pendingVaultAction = null
                    successMessage = "Vault unlock is unavailable"
                }
        } else {
            performAction(action)
        }
    }

    LaunchedEffect(vaultUnlocked) {
        if (vaultUnlocked) {
            pendingVaultAction?.let(::performAction)
            pendingVaultAction = null
        }
    }

    LaunchedEffect(notificationPermissionResult) {
        if (notificationPermissionResult > 0) {
            pendingNotificationAction?.let { action ->
                if (action.findingId?.let(vaultedFindingIds::contains) == true && !vaultUnlocked) {
                    pendingVaultAction = action
                    biometricPrompt?.authenticate(vaultPromptInfo)
                        ?: run {
                            pendingVaultAction = null
                            successMessage = "Vault unlock is unavailable"
                        }
                } else {
                    performAction(action)
                }
            }
            pendingNotificationAction = null
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(1_800)
            successMessage = null
        }
    }

    BackHandler(enabled = detailFinding == null && (recallOpen || shatterOpen)) {
        recallOpen = false
        shatterOpen = false
    }
    BackHandler(enabled = memoriesOpen) {
        memoriesOpen = false
    }
    BackHandler(enabled = collectionsOpen) {
        collectionsOpen = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glassBackgroundBrush())
            .hazeSource(state = hazeState),
    ) {
        LiquidBackground(
            sceneKey = livingScene.sceneKey,
            streak = dailyStreak,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            TopGlassBar(
                hazeState = hazeState,
                dailyStreak = dailyStreak,
                questLevel = questDashboard?.level,
                recallOpen = recallOpen || shatterOpen,
                onRecall = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    shatterOpen = false
                    recallOpen = true
                },
            )
            Spacer(Modifier.height(16.dp))
            val memory = todayMemory
            if (memory != null && !shatterOpen && !recallOpen && !memoriesOpen &&
                Tab.entries[selected] == Tab.Inbox
            ) {
                app.shotlist.ui.memories.MemoryCard(
                    memory = memory,
                    hazeState = hazeState,
                    onOpen = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        memoriesOpen = true
                    },
                    onDismiss = {
                        app.shotlist.engine.memories.MemoryEngine
                            .dismiss(context, memory.shot.id)
                        todayMemory = null
                    },
                )
                Spacer(Modifier.height(14.dp))
            }
            if (shatterOpen) {
                ShatterScreen(
                    hazeState = hazeState,
                    onClose = { shatterOpen = false },
                    modifier = Modifier.weight(1f),
                )
            } else if (recallOpen) {
                RecallScreen(
                    hazeState = hazeState,
                    vaultUnlocked = vaultUnlocked,
                    entitlement = entitlement,
                    onClose = { recallOpen = false },
                    onFindingAction = { finding ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showFindingDetail(finding)
                    },
                    onShowProPreview = {
                        proPreviewReason = ProPreviewReason.RECALL_HISTORY
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                AnimatedContent(
                    targetState = Tab.entries[selected],
                    transitionSpec = { fadeIn() + scaleIn(initialScale = 0.98f) togetherWith fadeOut() + scaleOut(targetScale = 0.98f) },
                    label = "tab-content",
                    modifier = Modifier.weight(1f),
                ) { tab ->
                    tabStateHolder.SaveableStateProvider(tab.name) {
                        when (tab) {
                    Tab.Inbox -> InboxScreen(
                        actions = actions,
                        focusFindingId = deepLinkFindingId,
                        focusRequestSerial = deepLinkSerial,
                        vaultedFindingIds = vaultedFindingIds,
                        vaultUnlocked = vaultUnlocked,
                        scannedCount = shotCount,
                        dailyStreak = dailyStreak,
                        weeklyStats = weeklyStats,
                        questDashboard = questDashboard,
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
                        onShareWrapped = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            pendingShare = PendingShare.WeeklyCard(
                                found = weeklyStats.found,
                                acted = weeklyStats.acted,
                                streak = dailyStreak,
                                topType = weeklyStats.topType,
                            )
                        },
                        onOpenDetail = { action ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            loadFindingDetail(action)
                        },
                        onVault = { action ->
                            action.findingId?.let { id ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (entitlement.canAddVaultItem(vaultedFindings.size)) {
                                    scope.launch { db.findings().setVaulted(id, true) }
                                    vaultUnlocked = false
                                    successMessage = "Locked in your vault"
                                } else {
                                    proPreviewReason = ProPreviewReason.VAULT_CAPACITY
                                }
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
                    Tab.Scan -> ScanScreen(hazeState = hazeState)
                    Tab.Track -> TrackScreen(hazeState = hazeState)
                    Tab.You -> YouScreen(
                        hazeState = hazeState,
                        screenshotsChecked = shotCount,
                        thingsReady = actions.size,
                        vaultedFindings = visibleVaultedFindings,
                        vaultTotalCount = vaultedFindings.size,
                        vaultUnlocked = vaultUnlocked,
                        imageAccessGranted = imageAccessGranted,
                        autoScanEnabled = autoScanEnabled,
                        palette = palette,
                        livingScene = livingScene,
                        entitlement = entitlement,
                        showEntitlementPreview = isDebugBuild,
                        onPaletteChanged = onPaletteChanged,
                        onLivingSceneChanged = onLivingSceneChanged,
                        onEntitlementChanged = { newEntitlement ->
                            if (isDebugBuild) {
                                entitlement = newEntitlement
                                prefs.edit()
                                    .putString(Entitlement.DEBUG_PREF_KEY, newEntitlement.name)
                                    .apply()
                                proPreviewReason = null
                                successMessage = if (newEntitlement.isPro) {
                                    "Previewing Pro"
                                } else {
                                    "Previewing free limits"
                                }
                            }
                        },
                        onShowProPreview = {
                            proPreviewReason = ProPreviewReason.VAULT_CAPACITY
                        },
                        onOpenCollections = {
                            recallOpen = false
                            shatterOpen = false
                            collectionsOpen = true
                        },
                        onOpenShatter = {
                            recallOpen = false
                            shatterOpen = true
                        },
                        onAutoScanChanged = { enabled ->
                            autoScanEnabled = enabled
                            prefs.edit().putBoolean("auto_scan", enabled).apply()
                            if (enabled) EngineApi.startObserving(context) else EngineApi.stopObserving()
                            successMessage = if (enabled) "Watching new screenshots" else "Auto-scan paused"
                        },
                        onOpenVault = {
                            biometricPrompt?.authenticate(vaultPromptInfo)
                                ?: run { successMessage = "Vault unlock is unavailable" }
                        },
                        onCopyVaulted = { finding ->
                            ShotlistActions.copyCode(context, finding.toShotlistAction())
                            successMessage = "Copied privately"
                        },
                        onUnvault = { finding ->
                            scope.launch { db.findings().setVaulted(finding.id, false) }
                            successMessage = "Moved back to your inbox"
                        },
                        onReplayOnboarding = {
                            prefs.edit().putBoolean("complete", false).apply()
                            onboardingComplete = false
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
                                entitlement = if (isDebugBuild) {
                                    Entitlement.FREE
                                } else {
                                    Entitlement.releaseDefault()
                                }
                                proPreviewReason = null
                                onboardingComplete = false
                            }
                        },
                    )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GlassNavBar(
                selected = selected,
                onSelected = {
                    recallOpen = false
                    shatterOpen = false
                    collectionsOpen = false
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
        questDashboard?.let { dashboard ->
            LevelUpBurst(
                level = dashboard.level.level,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (memoriesOpen) {
            app.shotlist.ui.memories.MemoriesFeed(
                initialMemory = todayMemory,
                onPinShot = { shot ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pendingCollectionTarget = CollectionTarget.shot(shot)
                },
                onClose = { memoriesOpen = false },
            )
        }
        if (collectionsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                CollectionsScreen(
                    hazeState = hazeState,
                    onClose = { collectionsOpen = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    detailFinding?.let { finding ->
        val action = finding.toShotlistAction()
        FindingDetailSheet(
            finding = finding,
            shot = detailShot,
            action = action,
            vaultUnlocked = vaultUnlocked,
            hazeState = hazeState,
            onDismissRequest = {
                detailFinding = null
                detailShot = null
            },
            onUnlock = {
                biometricPrompt?.authenticate(vaultPromptInfo)
                    ?: run { successMessage = "Vault unlock is unavailable" }
            },
            onPrimaryAction = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                requestPrimaryAction(action)
            },
            onShare = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                pendingShare = PendingShare.FindingCard(action)
                // Avoid stacking two modal sheets; the share picker replaces detail.
                detailFinding = null
                detailShot = null
            },
            onPin = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pendingCollectionTarget = CollectionTarget.finding(finding)
                // Avoid stacking modal sheets; the collection picker replaces detail.
                detailFinding = null
                detailShot = null
            },
            onVaultChanged = { vaulted ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (vaulted && !finding.vaulted && !entitlement.canAddVaultItem(vaultedFindings.size)) {
                    proPreviewReason = ProPreviewReason.VAULT_CAPACITY
                } else {
                    scope.launch { db.findings().setVaulted(finding.id, vaulted) }
                    detailFinding = finding.copy(vaulted = vaulted)
                    if (vaulted) vaultUnlocked = false
                    successMessage = if (vaulted) "Locked in your vault" else "Moved back to your inbox"
                }
            },
            onDismissFinding = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                setState(action, "DISMISSED")
                detailFinding = null
                detailShot = null
                successMessage = "Cleared out"
            },
        )
    }

    pendingCollectionTarget?.let { target ->
        PinToCollectionSheet(
            target = target,
            hazeState = hazeState,
            onDismissRequest = { pendingCollectionTarget = null },
        )
    }

    pendingShare?.let { request ->
        ShareTemplatePickerSheet(
            initialTemplate = ShareCardGenerator.selectedTemplate(context),
            onDismissRequest = { pendingShare = null },
            onShare = { template: ShareTemplate ->
                val intent = when (request) {
                    is PendingShare.FindingCard -> ShareCardGenerator.findingIntent(
                        context = context,
                        action = request.action,
                        template = template,
                    )
                    is PendingShare.WeeklyCard -> ShareCardGenerator.weeklyIntent(
                        context = context,
                        found = request.found,
                        acted = request.acted,
                        streak = request.streak,
                        topType = request.topType,
                        template = template,
                    )
                }
                pendingShare = null
                context.startActivity(intent)
            },
        )
    }

    proPreviewReason?.let { reason ->
        ProPreviewSheet(
            reason = reason,
            hazeState = hazeState,
            showDebugHint = isDebugBuild,
            onDismiss = { proPreviewReason = null },
        )
    }
}

@Composable
private fun TopGlassBar(
    hazeState: dev.chrisbanes.haze.HazeState,
    dailyStreak: Int,
    questLevel: LevelProgress?,
    recallOpen: Boolean,
    onRecall: () -> Unit,
) {
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
            questLevel?.let { level ->
                QuestLevelPill(level = level)
                Spacer(Modifier.width(9.dp))
            }
            Text(
                "🔥 $dailyStreak",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFBE63),
                modifier = Modifier
                    .background(Color(0xFFFFBE63).copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        if (!recallOpen) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onRecall)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search every screenshot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "RECALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InboxScreen(
    actions: List<ShotlistAction>,
    focusFindingId: Long?,
    focusRequestSerial: Int,
    vaultedFindingIds: Set<Long>,
    vaultUnlocked: Boolean,
    scannedCount: Int,
    dailyStreak: Int,
    weeklyStats: WeeklyStats,
    questDashboard: QuestDashboard?,
    hasScreenshotAccess: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onRequestAccess: () -> Unit,
    onShareWrapped: () -> Unit,
    onOpenDetail: (ShotlistAction) -> Unit,
    onVault: (ShotlistAction) -> Unit,
    onSnooze: (ShotlistAction) -> Unit,
    onDismiss: (ShotlistAction) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(focusRequestSerial, actions.size, questDashboard != null) {
        val actionIndex = actions.indexOfFirst { it.findingId == focusFindingId }
        if (actionIndex >= 0) {
            val headerCount = 3 + if (questDashboard != null) 1 else 0
            listState.animateScrollToItem(actionIndex + headerCount)
        }
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        questDashboard?.let { dashboard ->
            item(key = "daily-quests") {
                DailyQuestsCard(
                    dashboard = dashboard,
                    hazeState = hazeState,
                )
            }
        }
        if (actions.isEmpty()) {
            item {
                EmptyInboxCard(
                    scannedCount = scannedCount,
                    hasScreenshotAccess = hasScreenshotAccess,
                    onRequestAccess = onRequestAccess,
                    hazeState = hazeState,
                )
            }
            item {
                WeeklyWrappedCard(
                    dailyStreak = dailyStreak,
                    stats = weeklyStats,
                    hazeState = hazeState,
                    onShare = onShareWrapped,
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
                WeeklyWrappedCard(
                    dailyStreak = dailyStreak,
                    stats = weeklyStats,
                    hazeState = hazeState,
                    onShare = onShareWrapped,
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
                    locked = action.findingId?.let(vaultedFindingIds::contains) == true && !vaultUnlocked,
                    hazeState = hazeState,
                    onOpenDetail = { onOpenDetail(action) },
                    onVault = { onVault(action) },
                    onSnooze = { onSnooze(action) },
                    onDismiss = { onDismiss(action) },
                )
            }
        }
    }
}

private data class WeeklyStats(
    val found: Int,
    val acted: Int,
    val topType: String,
)

@Composable
private fun WeeklyWrappedCard(
    dailyStreak: Int,
    stats: WeeklyStats,
    hazeState: dev.chrisbanes.haze.HazeState,
    onShare: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(15.dp),
        accent = Color(0xFFFF79C9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "YOUR WEEK ✦",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF79C9),
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${stats.found} useful finds",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${stats.acted} handled · ${dailyStreak}-day rhythm · ${stats.topType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                )
            }
            FilledTonalButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildWeeklyStats(findings: List<Finding>): WeeklyStats {
    val start = LocalDate.now()
        .with(DayOfWeek.MONDAY)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val thisWeek = findings.filter { it.createdAt >= start }
    val top = thisWeek.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "All clear"
    return WeeklyStats(
        found = thisWeek.size,
        acted = thisWeek.count { it.state == "ACCEPTED" },
        topType = top,
    )
}

private fun updateDailyStreak(prefs: android.content.SharedPreferences): Int {
    val today = LocalDate.now().toEpochDay()
    val lastDay = prefs.getLong("daily_streak_last_day", Long.MIN_VALUE)
    val oldStreak = prefs.getInt("daily_streak_current", 0)
    val current = when {
        lastDay == today -> oldStreak.coerceAtLeast(1)
        lastDay == today - 1 -> oldStreak + 1
        lastDay > today -> oldStreak.coerceAtLeast(1)
        else -> 1
    }
    if (lastDay != today) {
        prefs.edit()
            .putLong("daily_streak_last_day", today)
            .putInt("daily_streak_current", current)
            .putInt("daily_streak_best", maxOf(current, prefs.getInt("daily_streak_best", 0)))
            .apply()
    }
    return current
}

private val wrappedFindingTypes = listOf(
    "EVENT", "DEADLINE", "PRODUCT", "PLACE", "CODE", "WIFI", "URL", "PHONE", "TRACKING", "RECIPE",
)

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActionCard(
    action: ShotlistAction,
    locked: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onOpenDetail: () -> Unit,
    onVault: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpenDetail,
                    onLongClick = onVault,
                ),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                KindIcon(action.kind, accent)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (locked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Private find",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (locked) "Locked until you verify it’s you." else action.detail,
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
                    FilledTonalButton(
                        onClick = onOpenDetail,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.copy(alpha = 0.20f),
                            contentColor = accent,
                        ),
                    ) {
                        Text(if (locked) "View private details" else "View details", fontSize = 14.sp)
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
        ActionKind.Link -> Icons.Outlined.Link
        ActionKind.Contact -> Icons.Outlined.Person
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
    ActionKind.Link -> Color(0xFFAAB8FF)
    ActionKind.Contact -> Color(0xFF7EF5D8)
    ActionKind.Recipe -> Color(0xFFFF9D72)
    ActionKind.Noise -> Color(0xFFB5BAD0)
}

private fun kindLabel(kind: ActionKind): String = when (kind) {
    ActionKind.Event -> "Event"
    ActionKind.Deadline -> "Deadline"
    ActionKind.Product -> "Product"
    ActionKind.Place -> "Place"
    ActionKind.Code -> "Code"
    ActionKind.Link -> "Link"
    ActionKind.Contact -> "Contact"
    ActionKind.Recipe -> "Recipe"
    ActionKind.Noise -> "Saved"
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
