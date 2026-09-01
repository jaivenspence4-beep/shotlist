package app.shotlist.ui.track

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shotlist.data.CycleEntry
import app.shotlist.data.Habit
import app.shotlist.data.HabitTick
import app.shotlist.data.ShotlistDb
import app.shotlist.ui.glass.GlassPanel
import app.shotlist.widget.ShotlistWidgets
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun TrackScreen(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val db = remember(context) { ShotlistDb.get(context) }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()
    val entries by db.cycle().lastYear().collectAsState(initial = emptyList())
    val habits by db.habits().active().collectAsState(initial = emptyList())
    val ticks by db.habits().ticksSince(todayEpoch - 90).collectAsState(initial = emptyList())
    var showAddHabit by remember { mutableStateOf(false) }
    LaunchedEffect(habits, ticks) {
        ShotlistWidgets.updateAll(context)
    }

    val periodStarts = remember(entries) { periodStarts(entries) }
    val typicalCycle = remember(periodStarts) { averageCycleLength(periodStarts) }
    val latestStart = periodStarts.maxOrNull()
    val cycleDay = latestStart?.let { (todayEpoch - it + 1).toInt().coerceAtLeast(1) }
    val nextEstimate = latestStart?.plus(typicalCycle.toLong())
    val todayFlow = entries.firstOrNull { it.day == todayEpoch }?.flow ?: "NONE"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 10.dp),
    ) {
        item {
            CycleHero(
                hazeState = hazeState,
                cycleDay = cycleDay,
                typicalCycle = typicalCycle,
                nextEstimate = nextEstimate,
            )
        }
        item {
            GlassPanel(
                hazeState = hazeState,
                cornerRadius = 30.dp,
                contentPadding = PaddingValues(16.dp),
                accent = Color(0xFFFF79C9),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("How is today?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "One tap. No forms, no cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(flowOptions, key = { it.value }) { option ->
                        FlowChip(
                            option = option,
                            selected = todayFlow == option.value,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    if (option.value == "NONE") {
                                        db.cycle().clearDay(todayEpoch)
                                    } else {
                                        db.cycle().upsert(
                                            CycleEntry(day = todayEpoch, flow = option.value),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
        item {
            WeekCalendar(
                hazeState = hazeState,
                today = today,
                entries = entries,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Habits",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Tiny wins count.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddHabit = true
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Add")
                }
            }
        }
        if (habits.isEmpty()) {
            item {
                GlassPanel(
                    hazeState = hazeState,
                    cornerRadius = 28.dp,
                    contentPadding = PaddingValues(16.dp),
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAddHabit = true
                        },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Start absurdly small", fontWeight = FontWeight.Bold)
                            Text(
                                "Water. Stretch. Read one page.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            )
                        }
                    }
                }
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                val completedToday = ticks.any { it.habitId == habit.id && it.day == todayEpoch }
                val streak = streakFor(habit.id, ticks, todayEpoch)
                HabitCard(
                    hazeState = hazeState,
                    habit = habit,
                    streak = streak,
                    completedToday = completedToday,
                    onToggle = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            if (completedToday) {
                                db.habits().untick(habit.id, todayEpoch)
                            } else {
                                db.habits().tick(HabitTick(habitId = habit.id, day = todayEpoch))
                            }
                        }
                    },
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Private on your phone · just your rhythm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                )
            }
        }
    }

    if (showAddHabit) {
        AddHabitDialog(
            onDismiss = { showAddHabit = false },
            onAdd = { name, emoji ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch { db.habits().insert(Habit(name = name, emoji = emoji)) }
                showAddHabit = false
            },
        )
    }
}

@Composable
private fun CycleHero(
    hazeState: HazeState,
    cycleDay: Int?,
    typicalCycle: Int,
    nextEstimate: Long?,
) {
    val shownDay by animateIntAsState(
        targetValue = cycleDay ?: 0,
        animationSpec = spring(),
        label = "cycle-day",
    )
    val progress by animateFloatAsState(
        targetValue = if (cycleDay == null) 0.08f else (cycleDay.toFloat() / typicalCycle).coerceIn(0.04f, 1f),
        animationSpec = spring(),
        label = "cycle-progress",
    )
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 34.dp,
        contentPadding = PaddingValues(16.dp),
        accent = Color(0xFFFF79C9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(158.dp), contentAlignment = Alignment.Center) {
                val pink = Color(0xFFFF79C9)
                val amber = Color(0xFFFFBE63)
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 11.dp.toPx()
                    val inset = stroke / 2f
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(pink, amber, pink)),
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (cycleDay == null) "—" else shownDay.toString(),
                        fontSize = 38.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF79C9),
                    )
                    Text(
                        if (cycleDay == null) "log a start" else "cycle day",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFFFFBE63),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (nextEstimate == null) "Your rhythm starts here" else "Next estimate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (nextEstimate == null) {
                        "Log a flow day and the ring learns locally."
                    } else {
                        LocalDate.ofEpochDay(nextEstimate).format(DateTimeFormatter.ofPattern("MMM d"))
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
                if (nextEstimate != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Based on a $typicalCycle-day rhythm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCalendar(
    hazeState: HazeState,
    today: LocalDate,
    entries: List<CycleEntry>,
) {
    val loggedDays = remember(entries) { entries.associateBy { it.day } }
    val days = remember(today) { (-3L..3L).map(today::plusDays) }
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(14.dp),
        accent = Color(0xFF8EAAFF),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                val isToday = day == today
                val flow = loggedDays[day.toEpochDay()]?.flow
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    )
                    Text(day.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(5.dp)
                            .background(
                                if (flow != null) flowColor(flow) else Color.White.copy(alpha = 0.10f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    hazeState: HazeState,
    habit: Habit,
    streak: Int,
    completedToday: Boolean,
    onToggle: () -> Unit,
) {
    val accent by animateColorAsState(
        targetValue = if (completedToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        animationSpec = spring(),
        label = "habit-color",
    )
    val scale by animateFloatAsState(
        targetValue = if (completedToday) 1f else 0.97f,
        animationSpec = spring(),
        label = "habit-scale",
    )
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 28.dp,
        contentPadding = PaddingValues(14.dp),
        accent = accent,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onToggle),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.emoji, fontSize = 23.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (streak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFFA64D),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "$streak day streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFBE63),
                        )
                    }
                } else {
                    Text(
                        "Tap when it’s done",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    )
                }
            }
            Text(
                if (completedToday) "Done" else "Today",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun FlowChip(option: FlowOption, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (selected) option.color else Color.White.copy(alpha = 0.62f),
        animationSpec = spring(),
        label = "flow-color",
    )
    Text(
        option.label,
        color = color,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .background(
                if (selected) option.color.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
        title = { Text("A tiny daily win") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    label = { Text("Habit") },
                    placeholder = { Text("Drink water") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(4) },
                    label = { Text("Emoji") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                enabled = name.isNotBlank(),
                onClick = { onAdd(name.trim(), emoji.ifBlank { "✨" }) },
            ) {
                Text("Start habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

private data class FlowOption(val label: String, val value: String, val color: Color)

private val flowOptions = listOf(
    FlowOption("None", "NONE", Color(0xFFA7ADC4)),
    FlowOption("Spotting", "SPOTTING", Color(0xFFFFB2CF)),
    FlowOption("Light", "LIGHT", Color(0xFFFF8DBD)),
    FlowOption("Medium", "MEDIUM", Color(0xFFFF5FA8)),
    FlowOption("Heavy", "HEAVY", Color(0xFFE63B86)),
)

private fun flowColor(flow: String): Color =
    flowOptions.firstOrNull { it.value == flow }?.color ?: Color(0xFFFF79C9)

private fun periodStarts(entries: List<CycleEntry>): List<Long> {
    val flowDays = entries.asSequence()
        .filter { it.flow != "NONE" }
        .map { it.day }
        .distinct()
        .sorted()
        .toList()
    return flowDays.filterIndexed { index, day ->
        index == 0 || day - flowDays[index - 1] > 2
    }
}

private fun averageCycleLength(starts: List<Long>): Int {
    if (starts.size < 2) return 28
    return starts.zipWithNext { first, second -> (second - first).toInt() }
        .takeLast(6)
        .average()
        .toInt()
        .coerceIn(21, 40)
}

private fun streakFor(habitId: Long, ticks: List<HabitTick>, today: Long): Int {
    val days = ticks.asSequence().filter { it.habitId == habitId }.map { it.day }.toSet()
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak += 1
        day -= 1
    }
    return streak
}
