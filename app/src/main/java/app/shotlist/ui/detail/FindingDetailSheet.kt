package app.shotlist.ui.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.data.Finding
import app.shotlist.data.Shot
import app.shotlist.ui.glass.GlassPanel
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingDetailSheet(
    finding: Finding,
    shot: Shot?,
    action: ShotlistAction,
    vaultUnlocked: Boolean,
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    onUnlock: () -> Unit,
    onPrimaryAction: () -> Unit,
    onShare: () -> Unit,
    onVaultChanged: (Boolean) -> Unit,
    onDismissFinding: () -> Unit,
) {
    val locked = finding.vaulted && !vaultUnlocked
    val accent = detailAccent(action.kind)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color(0x99060A16),
        dragHandle = null,
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 38.dp,
            contentPadding = PaddingValues(0.dp),
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                SourcePreview(
                    shot = shot,
                    locked = locked,
                    accent = accent,
                    onUnlock = onUnlock,
                    onClose = onDismissRequest,
                )
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (locked) "Private find" else action.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                detailKindLabel(action.kind),
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            "${(finding.confidence * 100).toInt().coerceIn(0, 100)}% match",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "EXTRACTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (locked) {
                        LockedFields(onUnlock = onUnlock, accent = accent)
                    } else {
                        ExtractedFields(finding = finding, action = action, accent = accent)
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "WHY THIS SURFACED",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = if (locked) {
                            AnnotatedString(
                                "This ${detailKindLabel(action.kind).lowercase()} is vaulted. Unlock to reveal the matched text.",
                            )
                        } else {
                            whySurfaced(finding, action, accent)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    )

                    Spacer(Modifier.height(22.dp))
                    if (locked) {
                        DetailButton(
                            text = "Unlock details",
                            icon = Icons.Outlined.LockOpen,
                            accent = accent,
                            onClick = onUnlock,
                        )
                    } else {
                        DetailButton(
                            text = detailPrimaryCta(action.kind),
                            icon = detailPrimaryIcon(action.kind),
                            accent = accent,
                            onClick = onPrimaryAction,
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            FilledTonalButton(
                                onClick = onShare,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                            FilledTonalButton(
                                onClick = { onVaultChanged(!finding.vaulted) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    if (finding.vaulted) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (finding.vaulted) "Unvault" else "Vault")
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        FilledTonalButton(
                            onClick = onDismissFinding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFF6688).copy(alpha = 0.14f),
                                contentColor = Color(0xFFFF91A8),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Dismiss this find")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePreview(
    shot: Shot?,
    locked: Boolean,
    accent: Color,
    onUnlock: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
            .background(accent.copy(alpha = 0.12f)),
    ) {
        if (!shot?.uri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(shot!!.uri),
                contentDescription = "Source screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (locked) Modifier.blur(28.dp) else Modifier),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (locked) Color(0x99060A16) else Color.Transparent,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(width = 42.dp, height = 5.dp)
                .background(Color.White.copy(alpha = 0.58f), RoundedCornerShape(999.dp)),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color(0x99060A16), CircleShape),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close details", tint = Color.White)
        }
        if (locked) {
            FilledTonalButton(
                onClick = onUnlock,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xCC12182A),
                    contentColor = Color.White,
                ),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Unlock source")
            }
        } else if (shot == null) {
            Text(
                "Loading source screenshot…",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun LockedFields(onUnlock: () -> Unit, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Text("Sensitive values are hidden behind your screen lock.")
        Spacer(Modifier.height(10.dp))
        FilledTonalButton(onClick = onUnlock) {
            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = accent)
            Spacer(Modifier.width(6.dp))
            Text("Verify it’s you", color = accent)
        }
    }
}

@Composable
private fun ExtractedFields(finding: Finding, action: ShotlistAction, accent: Color) {
    val fields = buildList {
        action.startsAt?.atZone(ZoneId.systemDefault())?.let {
            add("When" to it.format(detailDateFormat))
        }
        action.location?.takeIf { it.isNotBlank() }?.let { add("Where" to it) }
        action.code?.takeIf { it.isNotBlank() }?.let { add("Code" to it) }
        finding.amountCents?.let { add("Price" to formatPrice(it)) }
        action.url?.takeIf { it.isNotBlank() }?.let { add("Link" to it) }
        action.phone?.takeIf { it.isNotBlank() }?.let { add("Phone" to it) }
        action.email?.takeIf { it.isNotBlank() }?.let { add("Email" to it) }
        if (isEmpty()) {
            val fallback = finding.payload.ifBlank { finding.snippet.ifBlank { finding.title } }
            add("Found" to fallback)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(22.dp))
            .padding(horizontal = 15.dp),
    ) {
        fields.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    modifier = Modifier.width(66.dp),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index != fields.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }
        }
    }
}

@Composable
private fun DetailButton(text: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = accent.copy(alpha = 0.22f),
            contentColor = accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(7.dp))
        Text(text, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

private fun whySurfaced(finding: Finding, action: ShotlistAction, accent: Color): AnnotatedString =
    buildAnnotatedString {
        val label = detailKindLabel(action.kind).lowercase()
        val article = if (label.firstOrNull() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        append("Shotlist recognized $article $label")
        val matched = finding.snippet.ifBlank { finding.title }
        if (matched.isNotBlank()) {
            append(": ")
            pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold))
            append(matched)
            pop()
        }
    }

private val detailDateFormat = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

private fun formatPrice(cents: Long): String = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance("USD")
}.format(cents / 100.0)

private fun detailKindLabel(kind: ActionKind): String = when (kind) {
    ActionKind.Event -> "Event"
    ActionKind.Deadline -> "Deadline"
    ActionKind.Product -> "Product"
    ActionKind.Place -> "Place"
    ActionKind.Code -> "Code"
    ActionKind.Link -> "Link"
    ActionKind.Contact -> "Contact"
    ActionKind.Recipe -> "Recipe"
    ActionKind.Noise -> "Saved find"
}

private fun detailPrimaryCta(kind: ActionKind): String = when (kind) {
    ActionKind.Event -> "Add to Calendar"
    ActionKind.Deadline -> "Set reminder"
    ActionKind.Product -> "Save product"
    ActionKind.Place -> "Open in Maps"
    ActionKind.Code -> "Copy code"
    ActionKind.Link -> "Open link"
    ActionKind.Contact -> "Add contact"
    ActionKind.Recipe -> "Save recipe"
    ActionKind.Noise -> "Mark handled"
}

private fun detailPrimaryIcon(kind: ActionKind): ImageVector = when (kind) {
    ActionKind.Event, ActionKind.Deadline -> Icons.Outlined.CalendarMonth
    ActionKind.Product, ActionKind.Recipe, ActionKind.Noise -> Icons.Outlined.ShoppingBag
    ActionKind.Place -> Icons.Outlined.Map
    ActionKind.Code -> Icons.Outlined.ContentCopy
    ActionKind.Link -> Icons.Outlined.Link
    ActionKind.Contact -> Icons.Outlined.PersonAdd
}

private fun detailAccent(kind: ActionKind): Color = when (kind) {
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
