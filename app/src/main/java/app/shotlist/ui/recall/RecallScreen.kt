package app.shotlist.ui.recall

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.actions.ActionKind
import app.shotlist.data.Finding
import app.shotlist.data.RecallHit
import app.shotlist.data.ShotlistDb
import app.shotlist.engine.TitleQuality
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.shell.toShotlistAction
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@Composable
fun RecallScreen(
    hazeState: HazeState,
    vaultUnlocked: Boolean,
    onClose: () -> Unit,
    onFindingAction: (Finding) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val db = remember(context) { ShotlistDb.get(context) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var matchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(180)
        focusRequester.requestFocus()
    }
    LaunchedEffect(query.text) {
        delay(100)
        matchQuery = toFtsMatchQuery(query.text)
    }

    val resultsFlow = remember(db, matchQuery) {
        if (matchQuery.isBlank()) flowOf(emptyList()) else db.shots().recall(matchQuery)
    }
    val results by resultsFlow.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column {
                Text(
                    "Recall",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Ctrl+F for your camera roll",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.text.isNotEmpty()) {
                    IconButton(onClick = { query = TextFieldValue("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Wi-Fi, receipt, restaurant, tracking…") },
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        Spacer(Modifier.height(12.dp))
        when {
            query.text.isBlank() -> RecallPrompt(hazeState)
            matchQuery.isBlank() -> RecallMessage("Type at least two letters")
            results.isEmpty() -> RecallMessage("Nothing yet. Try a name, place, code, or phrase.")
            else -> {
                Text(
                    "${results.size} ${if (results.size == 1) "match" else "matches"} · local and instant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.shotId }) { hit ->
                        RecallResultCard(
                            hit = hit,
                            hazeState = hazeState,
                            locked = hit.findingVaulted == true && !vaultUnlocked,
                            onAction = {
                                val finding = hit.finding()
                                if (finding != null) {
                                    onFindingAction(finding)
                                } else {
                                    openScreenshot(context, hit.uri)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecallPrompt(hazeState: HazeState) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 32.dp,
        contentPadding = PaddingValues(18.dp),
        accent = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Outlined.ImageSearch,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text("Find the answer, not the screenshot", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(
            "Try “wifi”, a restaurant name, an order number, or the few words you remember. OCR search never leaves this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecallSuggestion("wifi")
            RecallSuggestion("receipt")
            RecallSuggestion("tracking")
        }
    }
}

@Composable
private fun RecallSuggestion(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

@Composable
private fun RecallMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun RecallResultCard(
    hit: RecallHit,
    hazeState: HazeState,
    locked: Boolean,
    onAction: () -> Unit,
) {
    val finding = hit.finding()
    val action = finding?.toShotlistAction()
    val accent = action?.kind?.let(::recallAccent) ?: MaterialTheme.colorScheme.primary
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(13.dp),
        accent = accent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 92.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (locked || hit.uri.isBlank()) {
                    Icon(
                        if (locked) Icons.Outlined.Lock else Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                    )
                } else {
                    AsyncImage(
                        model = Uri.parse(hit.uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (locked) "Private result" else recallHeaderTitle(hit.findingTitle, hit.excerpt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (locked) AnnotatedString("Unlock to reveal this match")
                    else highlightedExcerpt(hit.excerpt, accent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    formatDate(hit.takenAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FilledTonalButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    locked -> "View private details"
                    action == null -> "Open screenshot"
                    else -> "View details"
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun recallHeaderTitle(findingTitle: String?, excerpt: String): String {
    findingTitle
        ?.let(TitleQuality::firstUsableLine)
        ?.let { return it }

    val cleanExcerpt = excerpt
        .replace("[", "")
        .replace("]", "")
        .lineSequence()
        .joinToString("\n") { line -> line.trim().trim('…').trim() }
    return TitleQuality.firstUsableLine(cleanExcerpt) ?: "Screenshot match"
}

internal fun toFtsMatchQuery(input: String): String =
    input.lowercase()
        .split(Regex("[^\\p{L}\\p{N}_]+"))
        .asSequence()
        .filter { it.length >= 2 }
        .take(6)
        .joinToString(" AND ") { "$it*" }

private fun highlightedExcerpt(source: String, accent: Color): AnnotatedString = buildAnnotatedString {
    var highlighted = false
    val chunk = StringBuilder()
    fun flush() {
        if (chunk.isEmpty()) return
        val text = chunk.toString()
        if (highlighted) {
            pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold))
            append(text)
            pop()
        } else {
            append(text)
        }
        chunk.clear()
    }
    source.forEach { char ->
        when (char) {
            '[' -> {
                flush()
                highlighted = true
            }
            ']' -> {
                flush()
                highlighted = false
            }
            else -> chunk.append(char)
        }
    }
    flush()
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d · h:mm a"))

private fun recallAccent(kind: ActionKind): Color = when (kind) {
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

private fun openScreenshot(context: android.content.Context, rawUri: String) {
    if (rawUri.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(rawUri), "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
}
