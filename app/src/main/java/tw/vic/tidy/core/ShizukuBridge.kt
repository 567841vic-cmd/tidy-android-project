package tw.vic.tidy.core

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Expert mode. When Shizuku is running and has granted this app permission,
 * commands run with ADB-level rights — which is the only way, without root,
 * to actually trim other apps' caches or stop their background processes.
 *
 * Everything here is wrapped: if Shizuku is not installed the class simply
 * reports "unavailable" and the rest of the app carries on.
 */
object ShizukuBridge {

    const val REQUEST_CODE = 4919

    fun installed(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun granted(): Boolean = try {
        installed() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun shouldShowRationale(): Boolean = try {
        installed() && Shizuku.shouldShowRequestPermissionRationale()
    } catch (t: Throwable) {
        false
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (t: Throwable) {
            // Nothing to do — the caller already checked installed().
        }
    }

    data class ShellResult(val exitCode: Int, val output: String)

    /**
     * Shizuku.newProcess is not part of the public surface, so it is reached
     * by reflection. Commands used here produce a few lines at most.
     */
    fun exec(command: String): ShellResult {
        if (!granted()) return ShellResult(-1, "尚未取得 Shizuku 授權")
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null, arrayOf("sh", "-c", command), null, null
            ) as Process

            val out = process.inputStream.bufferedReader().use { it.readText() }
            val err = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            ShellResult(code, (out + err).trim())
        } catch (t: Throwable) {
            ShellResult(-1, t.message ?: "指令執行失敗")
        }
    }

    /** Clears cached data for every installed app. */
    fun trimAllCaches(): ShellResult = exec("pm trim-caches 999999999999")

    /** Ends background processes. Foreground apps are left alone by the system. */
    fun killBackgroundApps(): ShellResult = exec("am kill-all")
}
