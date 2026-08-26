package tw.vic.tidy.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Nothing is deleted outright. Files are moved into app-private storage and
 * kept for 30 days, so a wrong tick is recoverable. Empty folders are the one
 * exception — there is no data in them to lose.
 */
object RecycleBin {

    const val RETENTION_DAYS = 30
    private const val RETENTION_MS = RETENTION_DAYS * 24L * 60 * 60 * 1000

    data class Entry(
        val id: String,
        val originalPath: String,
        val storedName: String,
        val size: Long,
        val deletedAt: Long
    )

    data class CleanOutcome(
        val movedCount: Int,
        val freedBytes: Long,
        val failedCount: Int
    )

    fun binDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "recyclebin").apply { if (!exists()) mkdirs() }
    }

    private fun indexFile(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "recyclebin_index.json")
    }

    fun list(context: Context): List<Entry> {
        val f = indexFile(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(
                    id = o.getString("id"),
                    originalPath = o.getString("original"),
                    storedName = o.getString("stored"),
                    size = o.getLong("size"),
                    deletedAt = o.getLong("deletedAt")
                )
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private fun save(context: Context, entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("original", e.originalPath)
                    put("stored", e.storedName)
                    put("size", e.size)
                    put("deletedAt", e.deletedAt)
                }
            )
        }
        try {
            indexFile(context).writeText(arr.toString())
        } catch (t: Throwable) {
            // The bin index is a convenience; a failure here must not crash a clean.
        }
    }

    fun totalBytes(context: Context): Long = list(context).sumOf { it.size }

    /** Moves the given paths into the bin. Returns what actually happened. */
    fun remove(context: Context, items: List<JunkItem>): CleanOutcome {
        val bin = binDir(context)
        val entries = list(context).toMutableList()
        var moved = 0
        var failed = 0
        var freed = 0L
        val stamp = System.currentTimeMillis()

        items.forEachIndexed { index, item ->
            val src = File(item.path)
            if (!src.exists()) return@forEachIndexed

            if (item.isDir) {
                // Empty folder: no payload, so remove it directly.
                if (src.delete()) moved++ else failed++
                return@forEachIndexed
            }

            val storedName = "${stamp}_${index}_${src.name}"
            val dest = File(bin, storedName)
            val ok = try {
                src.renameTo(dest) || copyThenDelete(src, dest)
            } catch (t: Throwable) {
                false
            }

            if (ok) {
                moved++
                freed += item.size
                entries.add(
                    Entry(
                        id = storedName,
                        originalPath = item.path,
                        storedName = storedName,
                        size = item.size,
                        deletedAt = stamp
                    )
                )
            } else {
                failed++
            }
        }

        save(context, entries)
        return CleanOutcome(moved, freed, failed)
    }

    fun restore(context: Context, entry: Entry): Boolean {
        val stored = File(binDir(context), entry.storedName)
        if (!stored.exists()) return false
        val target = File(entry.originalPath)
        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val ok = try {
            stored.renameTo(target) || copyThenDelete(stored, target)
        } catch (t: Throwable) {
            false
        }
        if (ok) save(context, list(context).filter { it.id != entry.id })
        return ok
    }

    fun restoreAll(context: Context): Int = list(context).count { restore(context, it) }

    fun purgeExpired(context: Context) {
        val now = System.currentTimeMillis()
        val keep = mutableListOf<Entry>()
        list(context).forEach { e ->
            if (now - e.deletedAt > RETENTION_MS) {
                File(binDir(context), e.storedName).delete()
            } else {
                keep.add(e)
            }
        }
        save(context, keep)
    }

    fun emptyNow(context: Context) {
        list(context).forEach { File(binDir(context), it.storedName).delete() }
        save(context, emptyList())
    }

    private fun copyThenDelete(src: File, dest: File): Boolean = try {
        src.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        src.delete()
    } catch (t: Throwable) {
        dest.delete()
        false
    }
}
