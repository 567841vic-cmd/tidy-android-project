package tw.vic.tidy.core

import android.os.Environment
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.ArrayDeque

enum class JunkCategory(
    val title: String,
    val hint: String,
    val checkedByDefault: Boolean
) {
    TEMP(
        "暫存與日誌",
        "以 .tmp、.log、.bak 等結尾的殘留檔，App 需要時會重建",
        true
    ),
    EMPTY_DIR(
        "空資料夾",
        "檔案移走後留下的空目錄。裡面沒有任何資料，直接移除",
        true
    ),
    ZERO_BYTE(
        "零位元組檔",
        "大小為 0、寫入中斷留下的空殼檔",
        true
    ),
    THUMBS(
        "失效縮圖與索引",
        ".thumbnails、Thumbs.db、.DS_Store 等會自動重建的索引資料",
        true
    ),
    APK_RESIDUE(
        "殘留安裝檔",
        "下載後沒清掉的 .apk / .xapk。確認不再需要重裝再移除",
        false
    ),
    BACKUP_RESIDUE(
        "備份殘留",
        "備份資料夾與 LOST.DIR 裡的孤兒檔。移除前請先確認備份已還原",
        false
    ),
    DUPLICATE(
        "重複檔案",
        "內容相同的副本，最早的一份會保留",
        false
    ),
    BIG_FILE(
        "大型檔案",
        "超過 200 MB 的檔案。這裡只列出，預設不勾選",
        false
    );
}

data class JunkItem(
    val path: String,
    val size: Long,
    val isDir: Boolean
)

data class CategoryResult(
    val category: JunkCategory,
    val items: List<JunkItem>
) {
    val bytes: Long get() = items.sumOf { it.size }
    val count: Int get() = items.size
}

data class ScanReport(
    val results: List<CategoryResult>,
    val filesScanned: Int,
    val elapsedMs: Long,
    val unreachableAppData: Boolean
) {
    fun bytesFor(selected: Set<JunkCategory>): Long =
        results.filter { it.category in selected }.sumOf { it.bytes }

    val totalBytes: Long get() = results.sumOf { it.bytes }
}

object JunkScanner {

    private val TEMP_SUFFIXES = listOf(
        ".tmp", ".temp", ".log", ".bak", ".old", ".crdownload",
        ".part", ".partial", ".download", ".!ut", ".dmp"
    )
    private val THUMB_NAMES = listOf("thumbs.db", ".ds_store", ".thumbdata", "desktop.ini")
    private val BACKUP_HINTS = listOf(
        "/lost.dir", "/backup", "/backups", "/.estrongs", "/.bak",
        "/oppo/backup", "/coloros/backup", "/.trashed"
    )
    private const val BIG_FILE_THRESHOLD = 200L * 1024 * 1024
    private const val DUPLICATE_MIN_SIZE = 1L * 1024 * 1024
    private const val MAX_DEPTH = 24

    /**
     * Walks shared storage once and sorts what it finds. /Android/data and
     * /Android/obb are skipped: Android 11+ blocks them for every third-party
     * app, permission or not.
     */
    fun scan(
        trashDir: File?,
        onProgress: (scanned: Int, path: String) -> Unit,
        shouldContinue: () -> Boolean
    ): ScanReport {
        val started = System.currentTimeMillis()
        val root = Environment.getExternalStorageDirectory()

        val temp = mutableListOf<JunkItem>()
        val emptyDirs = mutableListOf<JunkItem>()
        val zero = mutableListOf<JunkItem>()
        val thumbs = mutableListOf<JunkItem>()
        val apks = mutableListOf<JunkItem>()
        val backups = mutableListOf<JunkItem>()
        val big = mutableListOf<JunkItem>()
        val duplicateCandidates = mutableListOf<File>()

        var scanned = 0
        val trashPath = trashDir?.absolutePath
        val stack = ArrayDeque<Pair<File, Int>>()
        stack.push(root to 0)

        while (stack.isNotEmpty() && shouldContinue()) {
            val (dir, depth) = stack.pop()
            if (depth > MAX_DEPTH) continue

            val children = try {
                dir.listFiles()
            } catch (t: Throwable) {
                null
            } ?: continue

            if (children.isEmpty() && depth > 0 && !isProtectedDir(dir, root)) {
                emptyDirs.add(JunkItem(dir.absolutePath, 0L, true))
                continue
            }

            for (child in children) {
                if (!shouldContinue()) break
                val path = child.absolutePath
                if (trashPath != null && path.startsWith(trashPath)) continue

                if (child.isDirectory) {
                    if (isBlockedDir(path, root)) continue
                    stack.push(child to depth + 1)
                    continue
                }

                scanned++
                if (scanned % 400 == 0) onProgress(scanned, shortPath(path))

                val size = child.length()
                val lower = child.name.lowercase()
                val lowerPath = path.lowercase()

                when {
                    size == 0L ->
                        zero.add(JunkItem(path, 0L, false))

                    THUMB_NAMES.any { lower == it || lower.startsWith(it) } ||
                        lowerPath.contains("/.thumbnails/") ->
                        thumbs.add(JunkItem(path, size, false))

                    TEMP_SUFFIXES.any { lower.endsWith(it) } ->
                        temp.add(JunkItem(path, size, false))

                    lower.endsWith(".apk") || lower.endsWith(".apks") ||
                        lower.endsWith(".xapk") ->
                        apks.add(JunkItem(path, size, false))

                    BACKUP_HINTS.any { lowerPath.contains(it) } ->
                        backups.add(JunkItem(path, size, false))

                    else -> {
                        if (size >= BIG_FILE_THRESHOLD) {
                            big.add(JunkItem(path, size, false))
                        }
                        if (size >= DUPLICATE_MIN_SIZE) {
                            duplicateCandidates.add(child)
                        }
                    }
                }
            }
        }

        onProgress(scanned, "比對重複檔案")
        val duplicates = findDuplicates(duplicateCandidates, shouldContinue)

        val results = listOf(
            CategoryResult(JunkCategory.TEMP, temp.sortedByDescending { it.size }),
            CategoryResult(JunkCategory.EMPTY_DIR, emptyDirs),
            CategoryResult(JunkCategory.ZERO_BYTE, zero),
            CategoryResult(JunkCategory.THUMBS, thumbs.sortedByDescending { it.size }),
            CategoryResult(JunkCategory.APK_RESIDUE, apks.sortedByDescending { it.size }),
            CategoryResult(JunkCategory.BACKUP_RESIDUE, backups.sortedByDescending { it.size }),
            CategoryResult(JunkCategory.DUPLICATE, duplicates),
            CategoryResult(JunkCategory.BIG_FILE, big.sortedByDescending { it.size })
        ).filter { it.count > 0 }

        return ScanReport(
            results = results,
            filesScanned = scanned,
            elapsedMs = System.currentTimeMillis() - started,
            unreachableAppData = true
        )
    }

    /** Groups by size first, then fingerprints only the groups that collide. */
    private fun findDuplicates(
        candidates: List<File>,
        shouldContinue: () -> Boolean
    ): List<JunkItem> {
        val out = mutableListOf<JunkItem>()
        val bySize = candidates.groupBy { it.length() }
        for ((_, group) in bySize) {
            if (!shouldContinue()) break
            if (group.size < 2) continue
            val byHash = mutableMapOf<String, MutableList<File>>()
            for (f in group) {
                val fp = fingerprint(f) ?: continue
                byHash.getOrPut(fp) { mutableListOf() }.add(f)
            }
            for ((_, same) in byHash) {
                if (same.size < 2) continue
                // Keep the oldest copy; everything newer is the duplicate.
                val sorted = same.sortedBy { it.lastModified() }
                for (dup in sorted.drop(1)) {
                    out.add(JunkItem(dup.absolutePath, dup.length(), false))
                }
            }
        }
        return out.sortedByDescending { it.size }
    }

    private fun fingerprint(f: File): String? = try {
        val md = MessageDigest.getInstance("MD5")
        val chunk = 128 * 1024
        RandomAccessFile(f, "r").use { raf ->
            val len = raf.length()
            val buf = ByteArray(chunk)
            val head = raf.read(buf, 0, minOf(len, chunk.toLong()).toInt())
            if (head > 0) md.update(buf, 0, head)
            if (len > chunk * 2L) {
                raf.seek(len - chunk)
                val tail = raf.read(buf, 0, chunk)
                if (tail > 0) md.update(buf, 0, tail)
            }
        }
        md.update(f.length().toString().toByteArray())
        md.digest().joinToString("") { String.format("%02x", it) }
    } catch (t: Throwable) {
        null
    }

    /** Sandboxed app storage is off limits on Android 11+, so don't descend. */
    private fun isBlockedDir(path: String, root: File): Boolean {
        val rel = path.removePrefix(root.absolutePath).lowercase()
        return rel.startsWith("/android/data") || rel.startsWith("/android/obb")
    }

    /** Well-known folders stay even when empty; apps expect them to exist. */
    private fun isProtectedDir(dir: File, root: File): Boolean {
        val rel = dir.absolutePath.removePrefix(root.absolutePath).trim('/')
        if (!rel.contains('/')) {
            val keep = setOf(
                "dcim", "download", "downloads", "pictures", "movies", "music",
                "documents", "android", "alarms", "notifications", "ringtones",
                "podcasts", "recordings", "audiobooks", "screenshots"
            )
            if (rel.lowercase() in keep) return true
        }
        return rel.isEmpty()
    }
}
