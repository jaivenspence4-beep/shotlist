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
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
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
import app.shotlist.data.Finding
import app.shotlist.data.Shot
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
fun MemoriesFeed(
    initialMemory: MemoryEngine.Memory? = null,
    onPinShot: (Shot) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember(context) { ShotlistDb.get(context) }
    val shots by db.shots().processedTimeline().collectAsState(initial = emptyList())
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMM d yyyy") }
    // The card the person tapped is page one even when a busy library pushes a
    // year-old shot outside processedTimeline's bounded window.
    val feedShots = remember(shots, initialMemory) {
        buildList {
            initialMemory?.shot?.let(::add)
            addAll(shots.filterNot { it.id == initialMemory?.shot?.id })
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (feedShots.isNotEmpty()) {
            val pager = rememberPagerState(pageCount = { feedShots.size })
            VerticalPager(
                state = pager,
                key = { feedShots[it].id },
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val shot = feedShots[page]
                val allFinds by androidx.compose.runtime.produceState<List<Finding>?>(
                    initialValue = null, shot.id,
                ) {
                    value = runCatching { db.findings().forShot(shot.id) }
                        .getOrNull()
                }
                val publicFinds = allFinds.orEmpty()
                    .filter { it.state != "DISMISSED" && !it.vaulted }
                val containsVaulted = allFinds?.any { it.vaulted } == true
                Box(Modifier.fillMaxSize()) {
                    when {
                        allFinds == null -> CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        containsVaulted -> MemoryTextFallback(
                            findings = publicFinds,
                            privatePixels = true,
                        )
                        else -> coil.compose.SubcomposeAsyncImage(
                            model = shot.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                }
                            },
                            error = {
                                // Share-copies are deleted after OCR: their persisted
                                // finds become the useful memory when pixels are gone.
                                MemoryTextFallback(
                                    findings = publicFinds,
                                    privatePixels = false,
                                )
                            },
                        )
                    }
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
                        publicFinds.take(3).forEach { f ->
                            Text(
                                f.title, color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                            )
                        }
                    }
                    IconButton(
                        onClick = { onPinShot(shot) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 104.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp)),
                    ) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = "Pin screenshot to a collection",
                            tint = Color.White,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${pager.currentPage + 1} / ${feedShots.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (feedShots.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Outlined.SwapVert,
                        contentDescription = "Swipe for another memory",
                        tint = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier.size(19.dp),
                    )
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
    }
}

@Composable
private fun MemoryTextFallback(
    findings: List<Finding>,
    privatePixels: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF11152B), Color(0xFF341C3E), Color(0xFF0C252A))
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (privatePixels) "Private screenshot" else "The useful part stayed",
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            findings.take(4).forEach { finding ->
                Text(
                    finding.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
