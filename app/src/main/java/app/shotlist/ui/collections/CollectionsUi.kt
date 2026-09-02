package app.shotlist.ui.collections

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.shotlist.data.BoardItem
import app.shotlist.data.BoardPin
import app.shotlist.data.BoardSummary
import app.shotlist.data.CollectionTargetType
import app.shotlist.data.Finding
import app.shotlist.data.SavedBoard
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.ui.share.ShareCardGenerator
import app.shotlist.ui.share.ShareTemplatePickerSheet
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

data class CollectionTarget private constructor(
    val type: String,
    val id: Long,
    val label: String,
) {
    companion object {
        fun finding(finding: Finding): CollectionTarget = CollectionTarget(
            type = CollectionTargetType.FINDING,
            id = finding.id,
            label = if (finding.vaulted) "Private find" else finding.title,
        )

        fun shot(shot: Shot): CollectionTarget = CollectionTarget(
            type = CollectionTargetType.SHOT,
            id = shot.id,
            label = "Screenshot",
        )
    }
}

@Composable
fun CollectionsScreen(
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dao = remember(context) { ShotlistDb.get(context).collections() }
    val boards by dao.boards().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var newBoardName by remember { mutableStateOf("") }

    selectedBoardId?.let { boardId ->
        BoardScreen(
            boardId = boardId,
            hazeState = hazeState,
            onBack = { selectedBoardId = null },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "collections-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Collections",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Keep the finds that belong together.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
        }
        item(key = "new-board") {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(14.dp),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newBoardName,
                        onValueChange = { if (it.length <= 40) newBoardName = it },
                        label = { Text("New collection") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        enabled = newBoardName.isNotBlank(),
                        onClick = {
                            val name = newBoardName
                            scope.launch {
                                selectedBoardId = dao.getOrCreateBoard(name)
                                newBoardName = ""
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Create")
                    }
                }
            }
        }
        if (boards.isEmpty()) {
            item(key = "empty") {
                GlassPanel(
                    hazeState = hazeState,
                    cornerRadius = 30.dp,
                    contentPadding = PaddingValues(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.CollectionsBookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("A home for the good stuff", fontWeight = FontWeight.Black)
                    Text(
                        "Try Trips, Recipes, Gift ideas, or anything you keep coming back to.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(boards, key = { it.id }) { board ->
                BoardRow(
                    board = board,
                    hazeState = hazeState,
                    onClick = { selectedBoardId = board.id },
                )
            }
        }
    }
}

@Composable
private fun BoardRow(
    board: BoardSummary,
    hazeState: HazeState,
    onClick: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp),
        accent = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
            ) {
                Icon(
                    Icons.Outlined.CollectionsBookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(board.name, fontWeight = FontWeight.Black)
                Text(
                    if (board.itemCount == 1) "1 saved item" else "${board.itemCount} saved items",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BoardScreen(
    boardId: Long,
    hazeState: HazeState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dao = remember(context) { ShotlistDb.get(context).collections() }
    val board by remember(boardId) { dao.board(boardId) }.collectAsState(initial = null)
    val boardItems by remember(boardId) { dao.items(boardId) }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    val currentBoard = board

    LazyColumn(
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "board-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        currentBoard?.name ?: "Collection",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (boardItems.size == 1) "1 saved item" else "${boardItems.size} saved items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
                IconButton(
                    enabled = currentBoard != null && boardItems.isNotEmpty(),
                    onClick = { shareOpen = true },
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share collection")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete collection")
                }
            }
        }
        if (boardItems.isEmpty()) {
            item(key = "empty-board") {
                GlassPanel(
                    hazeState = hazeState,
                    cornerRadius = 28.dp,
                    contentPadding = PaddingValues(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Pin a find to start", fontWeight = FontWeight.Black)
                    Text(
                        "Use Pin from a find or memory. This board stays on your phone.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
        } else {
            items(boardItems, key = { it.pinId }) { item ->
                BoardItemRow(
                    item = item,
                    hazeState = hazeState,
                    onRemove = { scope.launch { dao.unpin(item.pinId) } },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this collection?") },
            text = { Text("The original screenshots and finds stay in Shotlist.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            dao.deleteBoard(boardId)
                            onBack()
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            },
        )
    }

    if (shareOpen && currentBoard != null) {
        ShareTemplatePickerSheet(
            initialTemplate = ShareCardGenerator.selectedTemplate(context),
            title = "Share ${currentBoard.name}",
            onDismissRequest = { shareOpen = false },
            onShare = { template ->
                val highlight = boardItems.firstOrNull { it.available && !it.privatePixels }?.title
                    ?: "A private collection on my phone"
                shareOpen = false
                context.startActivity(
                    ShareCardGenerator.collectionIntent(
                        context = context,
                        boardName = currentBoard.name,
                        itemCount = boardItems.size,
                        highlight = highlight,
                        template = template,
                    )
                )
            },
        )
    }
}

@Composable
private fun BoardItemRow(
    item: BoardItem,
    hazeState: HazeState,
    onRemove: () -> Unit,
) {
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(12.dp),
        accent = if (item.privatePixels) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            ) {
                when {
                    item.privatePixels -> Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "Private",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    item.uri != null -> AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Icon(
                        Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.detail.isNotBlank()) {
                    Text(
                        item.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!item.available) {
                    Text(
                        "Original removed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove from collection")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinToCollectionSheet(
    target: CollectionTarget,
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember(context) { ShotlistDb.get(context).collections() }
    val boards by dao.boards().collectAsState(initial = emptyList())
    val pinnedBoardIds by remember(target) {
        dao.boardIdsForTarget(target.type, target.id)
    }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newBoardName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 36.dp,
            contentPadding = PaddingValues(20.dp),
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pin to a collection", fontWeight = FontWeight.Black)
                    Text(
                        target.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                items(boards, key = { it.id }) { board ->
                    val pinned = board.id in pinnedBoardIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .clickable {
                                scope.launch {
                                    if (pinned) {
                                        dao.unpin(board.id, target.type, target.id)
                                    } else {
                                        dao.pin(
                                            BoardPin(
                                                boardId = board.id,
                                                targetType = target.type,
                                                targetId = target.id,
                                            )
                                        )
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(board.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (pinned) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { if (it.length <= 40) newBoardName = it },
                    label = { Text("New collection") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = newBoardName.isNotBlank(),
                    onClick = {
                        val name = newBoardName
                        scope.launch {
                            val boardId = dao.getOrCreateBoard(name)
                            dao.pin(
                                BoardPin(
                                    boardId = boardId,
                                    targetType = target.type,
                                    targetId = target.id,
                                )
                            )
                            onDismissRequest()
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create and pin")
                }
            }
        }
    }
}
