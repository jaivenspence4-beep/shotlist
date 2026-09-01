package app.shotlist.engine.barcode

import android.content.Context
import android.net.Uri
import app.shotlist.data.Finding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

data class BarcodeResult(
    val type: String,
    val title: String,
    val detail: String,
    val payload: String,
    val rawValue: String,
    val whenAt: Long? = null,
    val vaulted: Boolean = false,
    val phone: String? = null,
    val email: String? = null,
    val cta: String? = null,
) {
    fun toFinding(shotId: Long): Finding = Finding(
        shotId = shotId,
        type = type,
        title = title,
        snippet = detail,
        whenAt = whenAt,
        payload = payload,
        confidence = 0.99f,
        vaulted = vaulted,
    )
}

object BarcodeDecoder {
    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    fun decode(
        context: Context,
        file: File,
        onSuccess: (BarcodeResult) -> Unit,
        onEmpty: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val image = runCatching { InputImage.fromFilePath(context, Uri.fromFile(file)) }
            .getOrElse {
                file.delete()
                onFailure()
                return
            }
        val scanner = BarcodeScanning.getClient(scannerOptions)
        scanner.process(image)
            .addOnSuccessListener { codes ->
                codes.asSequence()
                    .mapNotNull(::map)
                    .firstOrNull()
                    ?.let(onSuccess)
                    ?: onEmpty()
            }
            .addOnFailureListener { onFailure() }
            .addOnCompleteListener {
                scanner.close()
                file.delete()
            }
    }

    private fun map(code: Barcode): BarcodeResult? {
        val raw = code.rawValue?.trim().orEmpty()
        if (raw.isBlank()) return null
        return when (code.valueType) {
            Barcode.TYPE_WIFI -> {
                val wifi = code.wifi ?: return plainCode(raw)
                val ssid = wifi.ssid?.trim().orEmpty().ifBlank { "Wi-Fi network" }
                val password = wifi.password.orEmpty()
                BarcodeResult(
                    type = "WIFI",
                    title = ssid,
                    detail = if (password.isBlank()) "Open network · saved in your private Vault" else "Password saved in your private Vault",
                    payload = password,
                    rawValue = raw,
                    vaulted = true,
                )
            }

            Barcode.TYPE_URL -> {
                val target = code.url?.url?.trim().orEmpty()
                val uri = runCatching { Uri.parse(target) }.getOrNull()
                if (uri?.scheme !in setOf("http", "https")) {
                    plainCode(raw)
                } else {
                    BarcodeResult(
                        type = "URL",
                        title = uri.host?.removePrefix("www.") ?: "Web link",
                        detail = target,
                        payload = target,
                        rawValue = raw,
                        cta = "Open link",
                    )
                }
            }

            Barcode.TYPE_CONTACT_INFO -> {
                val contact = code.contactInfo ?: return plainCode(raw)
                val name = contact.name?.formattedName?.trim().orEmpty().ifBlank {
                    contact.organization?.trim().orEmpty().ifBlank { "New contact" }
                }
                val phone = contact.phones.firstOrNull()?.number?.trim()
                val email = contact.emails.firstOrNull()?.address?.trim()
                BarcodeResult(
                    type = "PHONE",
                    title = name,
                    detail = listOfNotNull(phone, email).joinToString(" · ").ifBlank { "Contact card" },
                    payload = phone ?: email.orEmpty(),
                    rawValue = raw,
                    phone = phone,
                    email = email,
                    cta = "Add contact",
                )
            }

            Barcode.TYPE_PHONE -> {
                val phone = code.phone?.number?.trim().orEmpty().ifBlank { raw }
                BarcodeResult(
                    type = "PHONE",
                    title = phone,
                    detail = "Phone number",
                    payload = phone,
                    rawValue = raw,
                    phone = phone,
                    cta = "Add contact",
                )
            }

            Barcode.TYPE_EMAIL -> {
                val email = code.email?.address?.trim().orEmpty().ifBlank { raw }
                BarcodeResult(
                    type = "PHONE",
                    title = email,
                    detail = "Email address",
                    payload = email,
                    rawValue = raw,
                    email = email,
                    cta = "Add contact",
                )
            }

            Barcode.TYPE_CALENDAR_EVENT -> {
                val event = code.calendarEvent ?: return plainCode(raw)
                val title = event.summary?.trim().orEmpty().ifBlank { "Calendar event" }
                val startsAt = event.start?.toEpochMillis()
                BarcodeResult(
                    type = "EVENT",
                    title = title,
                    detail = event.description?.trim().orEmpty().ifBlank {
                        event.location?.trim().orEmpty().ifBlank { "Ready to add to Calendar" }
                    },
                    payload = event.location.orEmpty(),
                    rawValue = raw,
                    whenAt = startsAt,
                    cta = "Add event",
                )
            }

            Barcode.TYPE_GEO -> {
                val point = code.geoPoint ?: return plainCode(raw)
                val coordinates = "${point.lat},${point.lng}"
                BarcodeResult(
                    type = "PLACE",
                    title = "Pinned place",
                    detail = coordinates,
                    payload = coordinates,
                    rawValue = raw,
                    cta = "Open map",
                )
            }

            Barcode.TYPE_PRODUCT, Barcode.TYPE_ISBN -> BarcodeResult(
                type = "PRODUCT",
                title = "Barcode ${code.displayValue ?: raw}",
                detail = "Ready to look up",
                payload = raw,
                rawValue = raw,
                cta = "Look up",
            )

            else -> plainCode(raw)
        }
    }

    private fun plainCode(raw: String) = BarcodeResult(
        type = "CODE",
        title = "Scanned code",
        detail = raw,
        payload = raw,
        rawValue = raw,
        cta = "Copy",
    )

    private fun Barcode.CalendarDateTime.toEpochMillis(): Long? = try {
        val zone = if (isUtc) ZoneOffset.UTC else ZoneId.systemDefault()
        LocalDateTime.of(year, month, day, hours, minutes, seconds)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeException) {
        null
    }
}
