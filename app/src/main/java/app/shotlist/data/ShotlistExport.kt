package app.shotlist.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.util.Base64
import android.util.JsonWriter
import androidx.core.content.FileProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Streams a complete, user-initiated snapshot without copying screenshot pixels. */
object ShotlistExport {
    private const val EXPORT_DIR = "shared_cards"
    private const val EXPORT_PREFIX = "shotlist-data-"

    suspend fun shareIntent(context: Context): Intent = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val file = createZip(appContext)
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.files",
            file,
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My Shotlist data")
            clipData = ClipData.newRawUri("Shotlist data export", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun clearCached(context: Context) = withContext(Dispatchers.IO) {
        val sharedDirectory = exportDirectory(context.applicationContext)
        sharedDirectory.listFiles().orEmpty().forEach { it.deleteRecursively() }
        sharedDirectory.delete()
    }

    private fun createZip(context: Context): File {
        val directory = exportDirectory(context)
        directory.mkdirs()
        directory.listFiles().orEmpty().filter(::isExportFile).forEach { it.delete() }

        val timestamp = System.currentTimeMillis()
        val output = directory.resolve("$EXPORT_PREFIX$timestamp.zip")
        val database = ShotlistDb.get(context).openHelper.readableDatabase
        database.beginTransaction()
        try {
            val tables = userTables(database)
            ZipOutputStream(FileOutputStream(output).buffered()).use { zip ->
                writeDataJson(zip, database, tables)
                writeImagesJson(zip, database)
                writeReadme(zip, tables)
            }
            database.setTransactionSuccessful()
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            database.endTransaction()
        }
        return output
    }

    private fun writeDataJson(
        zip: ZipOutputStream,
        database: SupportSQLiteDatabase,
        tables: List<String>,
    ) {
        zip.putNextEntry(ZipEntry("shotlist-data.json"))
        val writer = JsonWriter(OutputStreamWriter(zip, StandardCharsets.UTF_8)).apply {
            setIndent("  ")
        }
        writer.beginObject()
        writer.name("formatVersion").value(1L)
        writer.name("exportedAt").value(Instant.now().toString())
        writer.name("databaseVersion").value(database.version.toLong())
        writer.name("tables").beginObject()
        tables.forEach { table ->
            writer.name(table).beginArray()
            database.query("SELECT * FROM ${quoteIdentifier(table)}").use { cursor ->
                while (cursor.moveToNext()) {
                    writer.beginObject()
                    cursor.columnNames.forEachIndexed { index, column ->
                        writer.name(column)
                        writeCursorValue(writer, cursor, index)
                    }
                    writer.endObject()
                }
            }
            writer.endArray()
        }
        writer.endObject()
        writer.endObject()
        writer.flush()
        zip.closeEntry()
    }

    private fun writeImagesJson(zip: ZipOutputStream, database: SupportSQLiteDatabase) {
        zip.putNextEntry(ZipEntry("images.json"))
        val writer = JsonWriter(OutputStreamWriter(zip, StandardCharsets.UTF_8)).apply {
            setIndent("  ")
        }
        writer.beginObject()
        writer.name("note").value(
            "This manifest lists source image references. Screenshot pixels are not copied into the ZIP.",
        )
        writer.name("images").beginArray()
        database.query(
            "SELECT shots.id, shots.uri, shots.takenAt, EXISTS(" +
                "SELECT 1 FROM findings WHERE findings.shotId = shots.id " +
                "AND findings.vaulted = 1) AS containsVaultedData " +
                "FROM shots ORDER BY shots.takenAt DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                writer.beginObject()
                writer.name("shotId").value(cursor.getLong(0))
                writer.name("sourceUri").value(cursor.getString(1))
                writer.name("takenAt").value(cursor.getLong(2))
                writer.name("containsVaultedData").value(cursor.getLong(3) == 1L)
                writer.endObject()
            }
        }
        writer.endArray()
        writer.endObject()
        writer.flush()
        zip.closeEntry()
    }

    private fun writeReadme(zip: ZipOutputStream, tables: List<String>) {
        zip.putNextEntry(ZipEntry("README.txt"))
        val text = buildString {
            appendLine("Shotlist local data export")
            appendLine()
            appendLine("shotlist-data.json contains every local user-data table present at export time.")
            appendLine("images.json lists original image references; this ZIP contains no screenshot pixels.")
            appendLine("Vaulted values are included only after the in-app private-data confirmation.")
            appendLine()
            appendLine("Tables: ${tables.joinToString(", ")}")
        }
        zip.write(text.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeCursorValue(writer: JsonWriter, cursor: Cursor, index: Int) {
        when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> writer.nullValue()
            Cursor.FIELD_TYPE_INTEGER -> writer.value(cursor.getLong(index))
            Cursor.FIELD_TYPE_FLOAT -> writer.value(cursor.getDouble(index))
            Cursor.FIELD_TYPE_STRING -> writer.value(cursor.getString(index))
            Cursor.FIELD_TYPE_BLOB -> writer.value(
                Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP),
            )
            else -> writer.nullValue()
        }
    }

    private fun userTables(database: SupportSQLiteDatabase): List<String> =
        buildList {
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!isInternalTable(name)) add(name)
                }
            }
        }

    private fun isInternalTable(name: String): Boolean =
        name == "android_metadata" ||
            name == "room_master_table" ||
            name.startsWith("sqlite_") ||
            name == "shots_fts" ||
            name.startsWith("shots_fts_")

    private fun quoteIdentifier(value: String): String =
        "`" + value.replace("`", "``") + "`"

    private fun exportDirectory(context: Context): File =
        context.cacheDir.resolve(EXPORT_DIR)

    private fun isExportFile(file: File): Boolean =
        file.isFile && file.name.startsWith(EXPORT_PREFIX) && file.extension == "zip"
}
