package app.shotlist.ui.shell

import app.shotlist.actions.ActionKind
import app.shotlist.actions.ShotlistAction
import app.shotlist.data.Finding
import java.time.Instant

fun Finding.toShotlistAction(): ShotlistAction {
    val isLockedNote = type == "NOTE" && vaulted
    val kind = when (type) {
        "EVENT" -> ActionKind.Event
        "DEADLINE" -> ActionKind.Deadline
        "PRODUCT" -> ActionKind.Product
        "PLACE" -> ActionKind.Place
        "CODE", "WIFI", "TRACKING" -> ActionKind.Code
        "URL" -> ActionKind.Link
        "PHONE" -> ActionKind.Contact
        "RECIPE" -> ActionKind.Recipe
        else -> ActionKind.Noise
    }
    return ShotlistAction(
        id = "finding-$id",
        findingId = id,
        kind = kind,
        title = if (isLockedNote) "Vaulted note" else title,
        detail = when {
            isLockedNote -> "Unlock to read."
            type == "CODE" || type == "WIFI" -> "Sensitive value hidden. Tap Copy when you need it."
            else -> snippet.ifBlank { payload.ifBlank { type.lowercase().replaceFirstChar { it.uppercase() } } }
        },
        source = if (type == "NOTE") "quick note" else "screenshot #$shotId",
        confidence = confidence,
        startsAt = whenAt?.let(Instant::ofEpochMilli),
        location = payload.takeIf { kind == ActionKind.Event || kind == ActionKind.Place },
        url = payload.takeIf { type == "URL" },
        code = payload.takeIf { kind == ActionKind.Code },
        phone = payload.takeIf { type == "PHONE" && !it.contains("@") },
        email = payload.takeIf { type == "PHONE" && it.contains("@") }
            ?: snippet.split(" · ").firstOrNull { type == "PHONE" && it.contains("@") },
    )
}
