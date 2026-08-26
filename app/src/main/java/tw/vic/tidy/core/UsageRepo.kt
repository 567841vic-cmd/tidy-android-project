package tw.vic.tidy.core

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppUsage(
    val pkg: String,
    val label: String,
    val foregroundMs: Long,
    val lastUsed: Long,
    val isSystem: Boolean
)

/**
 * Android does not expose per-app battery draw to third-party apps. Foreground
 * time is the honest proxy, so the UI says "耗電推估" rather than claiming to
 * report mAh it cannot see.
 */
object UsageRepo {

    fun topApps(context: Context, hours: Int = 24, limit: Int = 12): List<AppUsage> {
        if (!Perms.hasUsageAccess(context)) return emptyList()
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - hours * 60L * 60 * 1000
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
                ?: return emptyList()

            val pm = context.packageManager
            val merged = HashMap<String, AppUsage>()

            for (s in stats) {
                if (s.totalTimeInForeground <= 0) continue
                if (s.packageName == context.packageName) continue
                val existing = merged[s.packageName]
                val info: ApplicationInfo = try {
                    pm.getApplicationInfo(s.packageName, 0)
                } catch (t: Throwable) {
                    continue
                }
                val label = try {
                    pm.getApplicationLabel(info).toString()
                } catch (t: Throwable) {
                    s.packageName
                }
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                merged[s.packageName] = AppUsage(
                    pkg = s.packageName,
                    label = label,
                    foregroundMs = (existing?.foregroundMs ?: 0L) + s.totalTimeInForeground,
                    lastUsed = maxOf(existing?.lastUsed ?: 0L, s.lastTimeUsed),
                    isSystem = isSystem
                )
            }

            merged.values.sortedByDescending { it.foregroundMs }.take(limit)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    fun launchable(context: Context, pkg: String): Boolean = try {
        context.packageManager.getLaunchIntentForPackage(pkg) != null
    } catch (t: Throwable) {
        false
    }

    @Suppress("unused")
    fun installedCount(context: Context): Int = try {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).size
    } catch (t: Throwable) {
        0
    }
}
