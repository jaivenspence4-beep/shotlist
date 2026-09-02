package app.shotlist.ui.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.celebration.CelebrationParticles
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

@Composable
fun rememberQuestDashboard(): QuestDashboard? {
    val context = LocalContext.current
    val dashboardFlow = remember(context) { DailyQuestEngine.observe(context) }
    val dashboard by dashboardFlow.collectAsState(initial = null)
    return dashboard
}

/** Compact level/XP treatment intended for the existing top header. */
@Composable
fun QuestLevelPill(
    level: LevelProgress,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.semantics {
            contentDescription =
                "Level ${level.level}, ${level.xpInLevel} of ${level.xpForNextLevel} XP"
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LV ${level.level}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${level.totalXp} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .width(92.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(level.fraction)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        ),
                        CircleShape,
                    ),
            )
        }
    }
}

/** Two or three small, finite goals—never a nagging infinite checklist. */
@Composable
fun DailyQuestsCard(
    dashboard: QuestDashboard,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(18.dp),
        accent = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Today’s small wins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${dashboard.completedCount}/${dashboard.quests.size} done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            Text(
                "${dashboard.quests.filterNot { it.complete }.sumOf { it.quest.xp }} XP left",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dashboard.quests.forEach { progress -> QuestRow(progress) }
        }
    }
}

@Composable
private fun QuestRow(progress: QuestProgress) {
    val color = if (progress.complete) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.14f), CircleShape),
        ) {
            Icon(
                imageVector = if (progress.complete) Icons.Outlined.Check else questIcon(progress.quest),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    progress.quest.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (progress.complete) "+${progress.quest.xp} XP" else
                        "${progress.progress}/${progress.quest.target}",
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                progress.quest.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(
                            (progress.progress.toFloat() / progress.quest.target).coerceIn(0f, 1f),
                        )
                        .height(4.dp)
                        .background(color, CircleShape),
                )
            }
        }
    }
}

/** Self-timed overlay; first composition never pretends a historical level was new. */
@Composable
fun LevelUpBurst(
    level: Int,
    modifier: Modifier = Modifier,
) {
    var lastLevel by rememberSaveable { mutableIntStateOf(level) }
    var celebratedLevel by rememberSaveable { mutableIntStateOf(0) }
    // A transient overlay must never restore as visible after activity/process recreation.
    var celebrationVisible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(level) {
        if (level > lastLevel) {
            celebratedLevel = level
            celebrationVisible = true
            lastLevel = level
            progress.snapTo(0f)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (level % 5 == 0) {
                delay(65L)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            progress.animateTo(1f, tween(1_650))
            celebrationVisible = false
        } else if (level < lastLevel) {
            // Handles a user clearing app data while this composition survives.
            lastLevel = level
        }
    }

    AnimatedVisibility(
        visible = celebrationVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.55f),
        exit = fadeOut() + scaleOut(targetScale = 1.2f),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB30A0D19)),
        ) {
            CelebrationParticles(
                progress = progress.value,
                intense = celebratedLevel % 5 == 0,
                modifier = Modifier.fillMaxSize(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (celebratedLevel % 5 == 0) Icons.Outlined.EmojiEvents else Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(76.dp),
                )
                Text(
                    if (celebratedLevel % 5 == 0) {
                        "Level $celebratedLevel milestone"
                    } else {
                        "Level $celebratedLevel"
                    },
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    if (celebratedLevel % 5 == 0) {
                        "Five more levels of useful momentum."
                    } else {
                        "A little sharper every day."
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun questIcon(quest: DailyQuest): ImageVector = when (quest) {
    DailyQuest.HANDLE_THREE -> Icons.Outlined.TaskAlt
    DailyQuest.SCAN_ONE -> Icons.Outlined.QrCodeScanner
    DailyQuest.CLEAR_INBOX -> Icons.Outlined.Inbox
}
