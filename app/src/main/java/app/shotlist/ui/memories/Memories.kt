package app.shotlist.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.shotlist.data.ShotlistDb
import app.shotlist.engine.memories.MemoryEngine
import app.shotlist.ui.glass.GlassPanel
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The one glass memory card allowed in the Inbox (Time Machine, t68). */
@Composable
fun MemoryCard(
    memory: MemoryEngine.Memory,
    hazeState: HazeState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 26.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = memory.shot.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AutoAwesome, contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        memory.agoLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    memory.findings.firstOrNull()?.title ?: "A moment you kept",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Outlined.Close, contentDescription = "Dismiss memory",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** Full-screen vertical swipe through everything that ever mattered (t71). */
@Composable
fun MemoriesFeed(onClose: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { ShotlistDb.get(context) }
    val shots by db.shots().processedTimeline().collectAsState(initial = emptyList())
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMM d yyyy") }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (shots.isNotEmpty()) {
            val pager = rememberPagerState(pageCount = { shots.size })
            VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val shot = shots[page]
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = shot.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f),
                                )
                            )
                            .padding(20.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            Instant.ofEpochMilli(shot.takenAt)
                                .atZone(ZoneId.systemDefault()).format(dateFmt),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        } else {
            Text(
                "Your memories build as Shotlist finds things.",
                Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
        }
        Spacer(Modifier.height(0.dp))
    }
}
