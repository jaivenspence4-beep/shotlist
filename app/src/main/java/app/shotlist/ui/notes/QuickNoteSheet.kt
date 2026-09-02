package app.shotlist.ui.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import app.shotlist.data.Finding
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb
import app.shotlist.ui.glass.GlassPanel
import dev.chrisbanes.haze.HazeState
import java.util.UUID
import kotlinx.coroutines.launch

private const val MAX_NOTE_LENGTH = 2_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteSheet(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val database = remember(context) { ShotlistDb.get(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismissRequest() },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 36.dp,
            contentPadding = PaddingValues(20.dp),
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            androidx.compose.foundation.layout.Row {
                Column(Modifier.weight(1f)) {
                    Text("Quick note", fontWeight = FontWeight.Black)
                    Text(
                        "Saved locally as a find. It never nags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
                IconButton(onClick = onDismissRequest, enabled = !saving) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { value ->
                    note = value.take(MAX_NOTE_LENGTH)
                    error = null
                },
                label = { Text("What do you want to keep?") },
                supportingText = { Text("${note.length}/$MAX_NOTE_LENGTH") },
                minLines = 5,
                maxLines = 10,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                enabled = note.isNotBlank() && !saving,
                onClick = {
                    val clean = note.trim()
                    saving = true
                    scope.launch {
                        runCatching {
                            database.withTransaction {
                                val now = System.currentTimeMillis()
                                val shotId = database.shots().insert(
                                    Shot(
                                        mediaId = manualMediaId(),
                                        uri = "",
                                        takenAt = now,
                                        ocrText = clean,
                                        status = "PROCESSED",
                                        createdAt = now,
                                    ),
                                )
                                check(shotId > 0) { "Could not reserve local note storage" }
                                val title = clean.lineSequence()
                                    .firstOrNull { it.isNotBlank() }
                                    ?.trim()
                                    ?.take(72)
                                    .orEmpty()
                                    .ifBlank { "Quick note" }
                                database.findings().insertAll(
                                    listOf(
                                        Finding(
                                            shotId = shotId,
                                            type = "NOTE",
                                            title = title,
                                            snippet = clean,
                                            confidence = 1f,
                                            createdAt = now,
                                        ),
                                    ),
                                )
                            }
                        }.onSuccess {
                            onSaved()
                        }.onFailure { failure ->
                            error = failure.message ?: "Could not save note"
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null)
                Text(if (saving) "Saving…" else "Save note", modifier = Modifier.padding(start = 7.dp))
            }
        }
    }
}

private fun manualMediaId(): Long {
    val randomPositive = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    return -randomPositive.coerceAtLeast(1L)
}
