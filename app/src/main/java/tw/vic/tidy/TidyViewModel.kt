package tw.vic.tidy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tw.vic.tidy.core.AppUsage
import tw.vic.tidy.core.BatteryInfo
import tw.vic.tidy.core.JunkCategory
import tw.vic.tidy.core.JunkItem
import tw.vic.tidy.core.JunkScanner
import tw.vic.tidy.core.Perms
import tw.vic.tidy.core.RamInfo
import tw.vic.tidy.core.RecycleBin
import tw.vic.tidy.core.ScanReport
import tw.vic.tidy.core.ShizukuBridge
import tw.vic.tidy.core.StorageInfo
import tw.vic.tidy.core.SystemStats
import tw.vic.tidy.core.UsageRepo
import tw.vic.tidy.core.formatBytes

data class TidyState(
    val storage: StorageInfo = StorageInfo(0L, 0L),
    val ram: RamInfo = RamInfo(0L, 0L),
    val battery: BatteryInfo? = null,
    val hasAllFiles: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val scanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanLabel: String = "",
    val report: ScanReport? = null,
    val selected: Set<JunkCategory> = emptySet(),
    val cleaning: Boolean = false,
    val lastFreed: Long = -1L,
    val trashBytes: Long = 0L,
    val trashCount: Int = 0,
    val usage: List<AppUsage> = emptyList(),
    val shizukuInstalled: Boolean = false,
    val shizukuGranted: Boolean = false,
    val shellBusy: Boolean = false,
    val shellOutput: String = "",
    val notice: String? = null
) {
    val selectedBytes: Long get() = report?.bytesFor(selected) ?: 0L
    val selectedItems: List<JunkItem>
        get() = report?.results.orEmpty()
            .filter { it.category in selected }
            .flatMap { it.items }
}

class TidyViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(TidyState())
    val state: StateFlow<TidyState> = _state.asStateFlow()

    private var scanJob: Job? = null

    @Volatile
    private var scanCancelled = false

    init {
        viewModelScope.launch(Dispatchers.IO) { RecycleBin.purgeExpired(getApplication()) }
        refresh()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val storage = withContext(Dispatchers.IO) { SystemStats.storage() }
            val ram = SystemStats.ram(ctx)
            val battery = SystemStats.battery(ctx)
            val trash = withContext(Dispatchers.IO) { RecycleBin.list(ctx) }
            _state.update {
                it.copy(
                    storage = storage,
                    ram = ram,
                    battery = battery,
                    hasAllFiles = Perms.hasAllFiles(ctx),
                    hasUsageAccess = Perms.hasUsageAccess(ctx),
                    trashBytes = trash.sumOf { e -> e.size },
                    trashCount = trash.size,
                    shizukuInstalled = ShizukuBridge.installed(),
                    shizukuGranted = ShizukuBridge.granted()
                )
            }
        }
    }

    fun refreshUsage() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { UsageRepo.topApps(ctx) }
            _state.update { it.copy(usage = list, hasUsageAccess = Perms.hasUsageAccess(ctx)) }
        }
    }

    // ---- Scanning -----------------------------------------------------

    fun startScan() {
        val ctx = getApplication<Application>()
        if (_state.value.scanning) return
        if (!Perms.hasAllFiles(ctx)) {
            _state.update { it.copy(notice = "需要「所有檔案存取權」才能掃描") }
            return
        }
        scanCancelled = false
        _state.update {
            it.copy(
                scanning = true, scanProgress = 0f, scanLabel = "準備掃描",
                report = null, lastFreed = -1L, selected = emptySet()
            )
        }

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val bin = RecycleBin.binDir(ctx)
            val report = JunkScanner.scan(
                trashDir = bin,
                onProgress = { scanned, path ->
                    _state.update { s ->
                        s.copy(
                            scanProgress = minOf(0.94f, scanned / 24000f),
                            scanLabel = path
                        )
                    }
                },
                shouldContinue = { !scanCancelled }
            )
            val defaults = report.results
                .map { it.category }
                .filter { it.checkedByDefault }
                .toSet()
            _state.update {
                it.copy(
                    scanning = false,
                    scanProgress = 1f,
                    scanLabel = "",
                    report = report,
                    selected = defaults
                )
            }
            refresh()
        }
    }

    fun cancelScan() {
        scanCancelled = true
        scanJob?.cancel()
        scanJob = null
        _state.update { it.copy(scanning = false, scanLabel = "", scanProgress = 0f) }
    }

    fun toggleCategory(category: JunkCategory) {
        _state.update {
            val next = it.selected.toMutableSet()
            if (!next.remove(category)) next.add(category)
            it.copy(selected = next)
        }
    }

    fun selectDefaults() {
        _state.update { s ->
            s.copy(
                selected = s.report?.results.orEmpty()
                    .map { it.category }.filter { it.checkedByDefault }.toSet()
            )
        }
    }

    fun selectNone() {
        _state.update { it.copy(selected = emptySet()) }
    }

    // ---- Cleaning -----------------------------------------------------

    fun cleanSelected() {
        val ctx = getApplication<Application>()
        val items = _state.value.selectedItems
        if (items.isEmpty() || _state.value.cleaning) return
        _state.update { it.copy(cleaning = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val outcome = RecycleBin.remove(ctx, items)
            val message = buildString {
                append("已移到回收桶 ${outcome.movedCount} 項")
                if (outcome.failedCount > 0) append("，${outcome.failedCount} 項無法移除")
            }
            _state.update {
                it.copy(
                    cleaning = false,
                    lastFreed = outcome.freedBytes,
                    report = null,
                    selected = emptySet(),
                    notice = message
                )
            }
            refresh()
        }
    }

    fun emptyTrash() {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            RecycleBin.emptyNow(ctx)
            _state.update { it.copy(notice = "回收桶已清空") }
            refresh()
        }
    }

    fun restoreTrash() {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val n = RecycleBin.restoreAll(ctx)
            _state.update { it.copy(notice = "已還原 $n 項") }
            refresh()
        }
    }

    // ---- Expert mode --------------------------------------------------

    fun refreshShizuku() {
        _state.update {
            it.copy(
                shizukuInstalled = ShizukuBridge.installed(),
                shizukuGranted = ShizukuBridge.granted()
            )
        }
    }

    fun trimAllCaches() = runShell("清除全機 App 快取") { ShizukuBridge.trimAllCaches() }

    fun killBackgroundApps() = runShell("結束背景 App") { ShizukuBridge.killBackgroundApps() }

    private fun runShell(label: String, block: () -> ShizukuBridge.ShellResult) {
        if (_state.value.shellBusy) return
        _state.update { it.copy(shellBusy = true, shellOutput = "$label…") }
        viewModelScope.launch(Dispatchers.IO) {
            val before = SystemStats.ram(getApplication())
            val result = block()
            val after = SystemStats.ram(getApplication())
            val gained = after.avail - before.avail
            val text = when {
                result.exitCode != 0 -> "$label 失敗：${result.output}"
                gained > 0 -> "$label 完成，記憶體多出 ${formatBytes(gained)}"
                else -> "$label 完成"
            }
            _state.update { it.copy(shellBusy = false, shellOutput = text) }
            refresh()
        }
    }

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }
}
