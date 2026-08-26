package tw.vic.tidy.core

import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, if (mb < 10) "%.1f MB" else "%.0f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

/** Splits a size into the number and its unit so the UI can style them apart. */
fun splitBytes(bytes: Long): Pair<String, String> {
    val s = formatBytes(bytes)
    val i = s.indexOf(' ')
    return if (i < 0) s to "" else s.substring(0, i) to s.substring(i + 1)
}

fun formatDuration(millis: Long): String {
    val totalMin = millis / 60000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 -> "${h} 小時 ${m} 分"
        m > 0 -> "${m} 分"
        else -> "不到 1 分"
    }
}

fun shortPath(path: String): String {
    val root = "/storage/emulated/0"
    return if (path.startsWith(root)) "內部儲存" + path.substring(root.length) else path
}
