package app.shotlist.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * App-side share sheet shown before Android's destination chooser. Rendering
 * happens only after a style is chosen, so backing out never writes a card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTemplatePickerSheet(
    initialTemplate: ShareTemplate,
    onDismissRequest: () -> Unit,
    onShare: (ShareTemplate) -> Unit,
    title: String = "Make it yours",
) {
    var selectedKey by rememberSaveable(initialTemplate.key) { mutableStateOf(initialTemplate.key) }
    val selected = ShareTemplate.fromKey(selectedKey)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(title, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(
                    "Pick a look, then choose where it goes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ShareTemplate.entries, key = { it.key }) { template ->
                    TemplateChoice(
                        template = template,
                        selected = template == selected,
                        onClick = { selectedKey = template.key },
                    )
                }
            }

            Button(
                onClick = { onShare(selected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Share with ${selected.label}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplateChoice(
    template: ShareTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f)
    Column(
        modifier = Modifier
            .width(166.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(3.dp),
    ) {
        Box {
            TemplatePreview(
                template = template,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.80f)
                    .border(if (selected) 3.dp else 1.dp, outline, RoundedCornerShape(18.dp)),
            )
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Text(
            template.label,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 3.dp, top = 9.dp),
        )
        Text(
            template.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 3.dp, top = 1.dp),
        )
    }
}

@Composable
private fun TemplatePreview(template: ShareTemplate, modifier: Modifier = Modifier) {
    when (template) {
        ShareTemplate.AURORA -> PreviewFrame(
            modifier = modifier,
            background = Brush.linearGradient(listOf(Color(0xFF171A3D), Color(0xFF7C2F68), Color(0xFF102A35))),
            foreground = Color.White,
            accent = Color(0xFFFF79C9),
            panel = Color.White.copy(alpha = 0.13f),
            rounded = true,
        )
        ShareTemplate.POSTER -> PreviewFrame(
            modifier = modifier,
            background = Brush.linearGradient(listOf(Color(0xFFFF77B7), Color(0xFFFFB76A))),
            foreground = Color(0xFF171119),
            accent = Color(0xFF171119),
            panel = Color(0xFFFFE15B),
            rounded = false,
        )
        ShareTemplate.PAPER -> PreviewFrame(
            modifier = modifier,
            background = Brush.linearGradient(listOf(Color(0xFFF1E5C9), Color(0xFFFFFCF1))),
            foreground = Color(0xFF191E26),
            accent = Color(0xFFC33B68),
            panel = Color(0xFFFFFCF1),
            rounded = false,
        )
        ShareTemplate.SIGNAL -> PreviewFrame(
            modifier = modifier,
            background = Brush.linearGradient(listOf(Color(0xFF050709), Color(0xFF121820))),
            foreground = Color.White,
            accent = Color(0xFF70F0D0),
            panel = Color.Black.copy(alpha = 0.48f),
            rounded = false,
        )
    }
}

@Composable
private fun PreviewFrame(
    background: Brush,
    foreground: Color,
    accent: Color,
    panel: Color,
    rounded: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(18.dp)
    val panelShape = if (rounded) RoundedCornerShape(12.dp) else RectangleShape
    Box(modifier = modifier.clip(cardShape).background(background).padding(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .background(panel, panelShape)
                .padding(horizontal = 12.dp, vertical = 15.dp),
        ) {
            Text("✦ SHOTLIST", color = foreground, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.size(width = 45.dp, height = 5.dp).background(accent))
            Spacer(Modifier.height(10.dp))
            Text("Your useful find", color = foreground, fontSize = 18.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            Text("Ready when you need it.", color = foreground.copy(alpha = 0.72f), fontSize = 9.sp)
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { Box(Modifier.size(width = 18.dp, height = 3.dp).background(accent.copy(alpha = 0.78f))) }
            }
        }
    }
}
