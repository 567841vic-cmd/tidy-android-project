package tw.vic.tidy.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = total - free
}

data class RamInfo(val total: Long, val avail: Long) {
    val used: Long get() = total - avail
}

data class BatteryInfo(
    val level: Int,
    val tempC: Float,
    val voltageMv: Int,
    val status: String,
    val health: String,
    val plugged: String,
    val currentMa: Int
)

object SystemStats {

    fun storage(): StorageInfo = try {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        StorageInfo(stat.totalBytes, stat.availableBytes)
    } catch (t: Throwable) {
        StorageInfo(0L, 0L)
    }

    fun ram(context: Context): RamInfo = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        RamInfo(mi.totalMem, mi.availMem)
    } catch (t: Throwable) {
        RamInfo(0L, 0L)
    }

    fun battery(context: Context): BatteryInfo {
        val intent: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val volt = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val statusText = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充電中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放電中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充飽"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充電"
            else -> "未知"
        }
        val healthText = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "過熱"
            BatteryManager.BATTERY_HEALTH_DEAD -> "已失效"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "過壓"
            BatteryManager.BATTERY_HEALTH_COLD -> "過冷"
            else -> "未知"
        }
        val pluggedText = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "電源"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "無線"
            else -> "未接電源"
        }

        var currentMa = 0
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val micro = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            currentMa = micro / 1000
        } catch (t: Throwable) {
            // Some vendors report nothing here; the field is simply hidden.
        }

        return BatteryInfo(pct, temp, volt, statusText, healthText, pluggedText, currentMa)
    }

    fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    fun androidVersion(): String = "Android ${Build.VERSION.RELEASE}"
}
