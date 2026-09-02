package app.shotlist.ui.purge

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.data.PurgeCandidate
import app.shotlist.data.ShotlistDb
import app.shotlist.ui.glass.GlassPanel
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class SizedCandidate(
    val candidate: PurgeCandidate,
    val bytes: Long,
)

@Composable
fun ShatterScreen(
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val db = remember(context) { ShotlistDb.get(context) }
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<SizedCandidate>?>(null) }
    var cursor by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<List<SizedCandidate>>(emptyList()) }
    var pending by remember { mutableStateOf<List<SizedCandidate>>(emptyList()) }
    var completedCount by remember { mutableIntStateOf(0) }
    var completedBytes by remember { mutableStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }
    var burstKey by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            candidates = null
            candidates = withContext(Dispatchers.IO) {
                db.shots().purgeCandidates()
                    .mapNotNull { candidate -> candidate.withSizeIfReadable(context) }
            }
            cursor = 0
            selected = emptyList()
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val confirmed = pending
            scope.launch {
                withContext(Dispatchers.IO) {
                    db.shots().forgetTrashedShots(confirmed.map { it.candidate.shotId })
                }
                completedCount = confirmed.size
                completedBytes = confirmed.sumOf { it.bytes }
                pending = emptyList()
                selected = emptyList()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(70)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            message = "Nothing moved. Your screenshots are untouched."
            pending = emptyList()
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Shatter",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Dead screenshots. One safe sweep.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
            if (selected.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    if (combo >= 2) {
                        Text(
                            "$combo× combo",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFD166),
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        "${selected.size} queued",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF79C9),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> UnsupportedShatter(hazeState)
            completedCount > 0 -> CompletedShatter(
                hazeState = hazeState,
                count = completedCount,
                bytes = completedBytes,
                onShare = { context.startActivity(shatterShareIntent(context, completedCount, completedBytes)) },
                bestCombo = bestCombo,
                onDone = onClose,
            )
            candidates == null -> LoadingShatter(hazeState)
            candidates!!.isEmpty() -> EmptyShatter(hazeState, onDone = onClose)
            cursor < candidates!!.size -> {
                val item = candidates!![cursor]
                val reviewed by animateIntAsState(cursor, label = "reviewed-count")
                Text(
                    "${reviewed + 1} of ${candidates!!.size} · ${formatBytes(selected.sumOf { it.bytes })} queued",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CandidateCard(
                        item = item,
                        hazeState = hazeState,
                        onShatter = {
                            val nextCombo = combo + 1
                            scope.launch {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (nextCombo == 3 || nextCombo % 5 == 0) {
                                    delay(55)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            combo = nextCombo
                            bestCombo = maxOf(bestCombo, nextCombo)
                            selected = selected + item
                            cursor += 1
                            burstKey += 1
                        },
                        onKeep = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            combo = 0
                            cursor += 1
                        },
                    )
                    ShatterBurst(
                        trigger = burstKey,
                        combo = combo,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            else -> ReviewSelection(
                hazeState = hazeState,
                selected = selected,
                message = message,
                onTrash = {
                    if (selected.isEmpty()) {
                        onClose()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching {
                            val request = MediaStore.createTrashRequest(
                                context.contentResolver,
                                selected.map { Uri.parse(it.candidate.uri) },
                                true,
                            )
                            pending = selected
                            trashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                        }.onFailure {
                            pending = emptyList()
                            message = "Android couldn’t open the trash confirmation. Nothing moved."
                        }
                    }
                },
                onReviewAgain = {
                    cursor = 0
                    selected = emptyList()
                    combo = 0
                    bestCombo = 0
                    message = null
                },
            )
        }
    }
}

@Composable
private fun CandidateCard(
    item: SizedCandidate,
    hazeState: HazeState,
    onShatter: () -> Unit,
    onKeep: () -> Unit,
) {
    var dragY by remember(item.candidate.shotId) { mutableFloatStateOf(0f) }
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 38.dp,
        contentPadding = PaddingValues(15.dp),
        accent = Color(0xFFFF79C9),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragY
                rotationZ = dragY / 42f
                alpha = (1f - abs(dragY) / 900f).coerceIn(0.55f, 1f)
            }
            .pointerInput(item.candidate.shotId) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragY = (dragY + amount).coerceIn(-420f, 420f)
                    },
                    onDragEnd = {
                        when {
                            dragY < -130f -> onShatter()
                            dragY > 130f -> onKeep()
                        }
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            AsyncImage(
                model = Uri.parse(item.candidate.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                formatBytes(item.bytes),
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color(0xB20B1020), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(13.dp))
        Text(item.candidate.reason, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            formatTakenAt(item.candidate.takenAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FilledTonalButton(onClick = onKeep, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Keep")
            }
            FilledTonalButton(onClick = onShatter, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Shatter")
            }
        }
        Text(
            "Flick up to queue · down to keep",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        )
    }
}

@Composable
private fun ReviewSelection(
    hazeState: HazeState,
    selected: List<SizedCandidate>,
    message: String?,
    onTrash: () -> Unit,
    onReviewAgain: () -> Unit,
) {
    val targetMb = selected.sumOf { it.bytes }.toFloat() / (1024f * 1024f)
    val animatedMb by animateFloatAsState(targetMb, tween(650), label = "eligible-mb")
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(20.dp),
        accent = Color(0xFFFF79C9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Outlined.DeleteSweep,
            contentDescription = null,
            tint = Color(0xFFFF79C9),
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (selected.isEmpty()) "Nothing queued" else String.format("%.0f MB eligible", animatedMb),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            if (selected.isEmpty()) "You kept every card." else "${selected.size} screenshots · Android asks once before moving them to trash.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
        )
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFFBE63), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(18.dp))
        FilledTonalButton(onClick = onTrash, modifier = Modifier.fillMaxWidth()) {
            Icon(
                if (selected.isEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.AutoDelete,
                contentDescription = null,
            )
            Spacer(Modifier.width(7.dp))
            Text(if (selected.isEmpty()) "Done" else "Review in Android’s trash dialog", fontWeight = FontWeight.Bold)
        }
        if (selected.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onReviewAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Start over")
            }
        }
    }
}

@Composable
private fun CompletedShatter(
    hazeState: HazeState,
    count: Int,
    bytes: Long,
    bestCombo: Int,
    onShare: () -> Unit,
    onDone: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(20.dp),
        accent = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text("${formatBytes(bytes)} freed", fontSize = 36.sp, fontWeight = FontWeight.Black)
        Text(
            buildString {
                append("$count screenshots moved to trash — recoverable from Photos.")
                if (bestCombo >= 2) append(" Best combo: $bestCombo×.")
            },
        )
        Spacer(Modifier.height(18.dp))
        FilledTonalButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Share, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Share the clean sweep", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

@Composable
private fun LoadingShatter(hazeState: HazeState) {
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Text("Finding dead screenshots…", fontWeight = FontWeight.Bold)
        Text("Accepted and vaulted finds are excluded first.")
    }
}

@Composable
private fun EmptyShatter(hazeState: HazeState, onDone: () -> Unit) {
    GlassPanel(hazeState = hazeState, accent = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(8.dp))
        Text("Nothing safe to shatter", fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text("Accepted, vaulted, active, and app-private images were left alone.")
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun UnsupportedShatter(hazeState: HazeState) {
    GlassPanel(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
        Text("Android 11 or newer required", fontWeight = FontWeight.Black)
        Text("Older Android versions cannot offer the one-dialog, recoverable trash flow safely.")
    }
}

@Composable
private fun ShatterBurst(trigger: Int, combo: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(520))
        }
    }
    if (progress.value < 1f) {
        Box(modifier) {
            Canvas(Modifier.fillMaxSize()) {
                val origin = center
                val shardCount = 18 + combo.coerceAtMost(6) * 3
                repeat(shardCount) { index ->
                    val angle = index * (Math.PI * 2 / shardCount)
                    val inner = 30f + progress.value * 90f
                    val outer = 70f + progress.value * size.minDimension * 0.52f
                    drawLine(
                        color = if (index % 2 == 0) Color(0xFFFF79C9) else Color(0xFF7EF5D8),
                        start = androidx.compose.ui.geometry.Offset(
                            origin.x + cos(angle).toFloat() * inner,
                            origin.y + sin(angle).toFloat() * inner,
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            origin.x + cos(angle).toFloat() * outer,
                            origin.y + sin(angle).toFloat() * outer,
                        ),
                        strokeWidth = 3f * (1f - progress.value),
                    )
                }
            }
            if (combo >= 2) {
                Text(
                    "$combo×",
                    color = Color.White.copy(alpha = (1f - progress.value).coerceIn(0f, 1f)),
                    fontSize = (28 + combo.coerceAtMost(8) * 2).sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            scaleX = 0.78f + progress.value * 0.44f
                            scaleY = scaleX
                        },
                )
            }
        }
    }
}

private fun PurgeCandidate.withSizeIfReadable(context: Context): SizedCandidate? {
    val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
    if (parsed.scheme != "content" || parsed.authority?.startsWith("media") != true) return null
    return runCatching {
        val queriedSize = context.contentResolver.query(
            parsed,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
        } ?: -1L
        val descriptorSize = if (queriedSize >= 0) queriedSize else {
            context.contentResolver.openFileDescriptor(parsed, "r")?.use { it.statSize } ?: -1L
        }
        if (descriptorSize < 0) null else SizedCandidate(this, descriptorSize)
    }.getOrNull()
}

private fun formatTakenAt(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
