package app.shotlist.ui.metabolic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.shotlist.data.GlucoseMoment
import app.shotlist.data.GlucoseSample
import app.shotlist.health.api.GlucoseStory
import app.shotlist.health.api.GlucoseUnit
import app.shotlist.health.api.GlucoseUnits
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun MetabolicChart(
    samples: List<GlucoseSample>,
    moments: List<GlucoseMoment>,
    from: Long,
    until: Long,
    unit: GlucoseUnit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val axis = GlucoseStory.axisBounds(samples)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                GlucoseUnits.format(axis.second, unit, locale),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
            Text(
                GlucoseUnits.label(unit),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(vertical = 8.dp),
        ) {
            repeat(5) { index ->
                val y = size.height * index / 4f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            fun xFor(time: Long): Float =
                ((time - from).toDouble() / (until - from).coerceAtLeast(1L))
                    .toFloat()
                    .coerceIn(0f, 1f) * size.width

            fun yFor(value: Double): Float =
                size.height - (((value - axis.first) / (axis.second - axis.first))
                    .toFloat()
                    .coerceIn(0f, 1f) * size.height)

            moments.filter { it.occurredAt in from..until }.forEach { moment ->
                val x = xFor(moment.occurredAt)
                drawLine(
                    color = secondary.copy(alpha = 0.34f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawCircle(secondary, radius = 4.dp.toPx(), center = Offset(x, 8.dp.toPx()))
            }

            val sorted = samples.sortedBy { it.observedAt }
            var path = Path()
            var hasSegment = false
            sorted.forEachIndexed { index, sample ->
                val point = Offset(xFor(sample.observedAt), yFor(sample.mmolPerLiter))
                val previous = sorted.getOrNull(index - 1)
                val startsSegment = previous == null ||
                    sample.observedAt - previous.observedAt > GlucoseStory.GAP_THRESHOLD_MS
                if (startsSegment) {
                    if (hasSegment) {
                        drawPath(path, primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }
                    path = Path().apply { moveTo(point.x, point.y) }
                    hasSegment = true
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            if (hasSegment) {
                drawPath(path, primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            sorted.lastOrNull()?.let { sample ->
                drawCircle(
                    color = primary,
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(sample.observedAt), yFor(sample.mmolPerLiter)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatAxisTime(from), style = MaterialTheme.typography.labelSmall, color = textColor)
            Text(formatAxisTime(until), style = MaterialTheme.typography.labelSmall, color = textColor)
        }
        Text(
            GlucoseUnits.format(axis.first, unit, locale),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

private fun formatAxisTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h a"))

internal fun momentLabel(kind: String): String = when (kind) {
    "MEAL" -> "Meal"
    "EXERCISE" -> "Movement"
    "SLEEP" -> "Sleep"
    else -> "Note"
}

internal fun momentColor(kind: String): Color = when (kind) {
    "MEAL" -> Color(0xFFFFB86B)
    "EXERCISE" -> Color(0xFF68E7D2)
    "SLEEP" -> Color(0xFFAAA4FF)
    else -> Color(0xFFFF86C8)
}
