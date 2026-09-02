package app.shotlist.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.ui.glass.GlassPanel
import dev.chrisbanes.haze.HazeState

enum class ProPreviewReason(val headline: String, val detail: String) {
    RECALL_HISTORY(
        headline = "Search beyond 30 days",
        detail = "Free Recall searches your most recent 30 days. Pro is planned to search your full on-device screenshot history.",
    ),
    VAULT_CAPACITY(
        headline = "Keep more than 3 private finds",
        detail = "The free private vault holds 3 finds. Pro is planned to remove that limit while keeping the vault on-device.",
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPreviewSheet(
    reason: ProPreviewReason,
    hazeState: HazeState,
    showDebugHint: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color(0x99060A16),
        dragHandle = null,
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 38.dp,
            contentPadding = PaddingValues(20.dp),
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            CircleShape,
                        )
                        .padding(11.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Shotlist Pro preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(reason.headline, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(
                reason.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProFeature("Full-history Recall")
                ProFeature("Unlimited private vault")
                ProFeature("The same local-first processing")
            }
            Text(
                "Purchases aren’t connected in this build. Nothing here can charge you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp),
            )
            if (showDebugHint) {
                Text(
                    "To preview both tiers, use Developer preview in the You tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Text("Keep using free", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProFeature(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}
