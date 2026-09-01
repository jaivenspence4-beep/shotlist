package app.shotlist.ui.shell

import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.data.Finding
import java.time.Instant

fun Finding.toShotlistAction(): ShotlistAction {
    val kind = when (type) {
        "EVENT" -> ActionKind.Event
        "DEADLINE" -> ActionKind.Deadline
        "PRODUCT" -> ActionKind.Product
        "PLACE" -> ActionKind.Place
        "CODE", "WIFI", "TRACKING" -> ActionKind.Code
        "RECIPE" -> ActionKind.Recipe
        else -> ActionKind.Noise
    }
    return ShotlistAction(
        id = "finding-$id",
        findingId = id,
        kind = kind,
        title = title,
        detail = snippet.ifBlank { payload.ifBlank { type.lowercase().replaceFirstChar { it.uppercase() } } },
        source = "screenshot #$shotId",
        confidence = confidence,
        startsAt = whenAt?.let(Instant::ofEpochMilli),
        location = payload.takeIf { kind == ActionKind.Event || kind == ActionKind.Place },
        url = payload.takeIf { type == "URL" },
        code = payload.takeIf { kind == ActionKind.Code },
    )
}
