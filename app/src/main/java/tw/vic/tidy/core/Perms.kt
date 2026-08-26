package tw.vic.tidy.core

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings

/**
 * Every privileged capability this app has is a permission the user grants
 * in system Settings. Nothing here is silent, and nothing here is required
 * for the app to open.
 */
object Perms {

    private const val ACTION_ALL_FILES_FOR_APP =
        "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION"
    private const val ACTION_ALL_FILES =
        "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"
    private const val ACTION_CLEAR_APP_CACHE =
        "android.os.storage.action.CLEAR_APP_CACHE"
    private const val ACTION_MANAGE_STORAGE =
        "android.os.storage.action.MANAGE_STORAGE"

    // ---- Whole-volume file access -------------------------------------

    fun hasAllFiles(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun requestAllFiles(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val direct = Intent(ACTION_ALL_FILES_FOR_APP).apply {
                data = Uri.parse("package:" + context.packageName)
            }
            if (!start(context, direct)) start(context, Intent(ACTION_ALL_FILES))
        } else {
            openAppDetails(context, context.packageName)
        }
    }

    // ---- Usage access (battery insight) -------------------------------

    fun hasUsageAccess(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (t: Throwable) {
        false
    }

    fun openUsageAccess(context: Context) {
        start(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    // ---- System-provided cleanup entry points -------------------------

    /** Android 12+ shows a system dialog that clears cached data for every app. */
    fun canUseSystemCacheDialog(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun openSystemCacheDialog(context: Context): Boolean =
        start(context, Intent(ACTION_CLEAR_APP_CACHE))

    fun openStorageManager(context: Context) {
        if (!start(context, Intent(ACTION_MANAGE_STORAGE))) {
            start(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
    }

    fun openBatteryOptimization(context: Context) {
        if (!start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            start(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }
    }

    fun openAppDetails(context: Context, pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        start(context, i)
    }

    /**
     * ColorOS keeps auto-start control in its own security centre. The
     * component name has moved between versions, so try the known ones and
     * fall back to the generic battery screen.
     */
    fun openOppoAutoStart(context: Context) {
        val candidates = listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.coloros.oppoguardelf" to "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
        )
        for ((pkg, cls) in candidates) {
            val i = Intent().apply { component = ComponentName(pkg, cls) }
            if (start(context, i)) return
        }
        openBatteryOptimization(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        false
    }
}
