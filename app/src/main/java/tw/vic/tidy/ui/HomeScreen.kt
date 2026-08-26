package tw.vic.tidy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tw.vic.tidy.TidyState
import tw.vic.tidy.TidyViewModel
import tw.vic.tidy.core.Perms
import tw.vic.tidy.core.ShizukuBridge
import tw.vic.tidy.core.SystemStats
import tw.vic.tidy.core.formatBytes
import tw.vic.tidy.core.splitBytes
import tw.vic.tidy.ui.components.Divider1
import tw.vic.tidy.ui.components.Eyebrow
import tw.vic.tidy.ui.components.HandoffRow
import tw.vic.tidy.ui.components.Panel
import tw.vic.tidy.ui.components.PrimaryAction
import tw.vic.tidy.ui.components.QuietAction
import tw.vic.tidy.ui.components.StatLine
import tw.vic.tidy.ui.components.Stratum
import tw.vic.tidy.ui.components.StratumBar
import tw.vic.tidy.ui.components.StratumLegend
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.Ty

private val FreeBand = Color(0xFF2A4A44)

@Composable
fun HomeScreen(vm: TidyViewModel, state: TidyState, onGoClean: () -> Unit) {
    val context = LocalContext.current
    val sweep by animateFloatAsState(targetValue = state.scanProgress, label = "sweep")

    LaunchedEffect(Unit) { vm.refresh() }

    val reclaimable = state.report?.totalBytes ?: 0L
    val usedOther = (state.storage.used - reclaimable).coerceAtLeast(0L)
    val strata = listOf(
        Stratum("已使用", usedOther, T.Slate),
        Stratum("可回收", reclaimable, T.Brass, reclaimable = true),
        Stratum("可用空間", state.storage.free, FreeBand)
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("淨機", style = Ty.Title)
        Spacer(Modifier.height(4.dp))
        Text(
            "${SystemStats.deviceName()} · ${SystemStats.androidVersion()}",
            style = Ty.Caption
        )

        Spacer(Modifier.height(24.dp))
        Eyebrow("儲存空間剖面")
        Spacer(Modifier.height(12.dp))
        StratumBar(strata = strata, sweep = sweep, scanning = state.scanning)
        Spacer(Modifier.height(12.dp))
        StratumLegend(strata)

        Spacer(Modifier.height(22.dp))
        HeadlineNumber(state, reclaimable)

        Spacer(Modifier.height(20.dp))

        if (!state.hasAllFiles) {
            Panel {
                Text("先開啟檔案存取權", style = Ty.Title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "掃描共用儲存空間需要「所有檔案存取權」。App 只讀取檔案的名稱、大小與時間，不會上傳任何內容。",
                    style = Ty.Caption
                )
                Spacer(Modifier.height(14.dp))
                PrimaryAction("前往授權") { Perms.requestAllFiles(context) }
                Spacer(Modifier.height(6.dp))
                QuietAction("我已經開好了，重新檢查") { vm.refresh() }
            }
            Spacer(Modifier.height(16.dp))
        } else if (state.scanning) {
            Panel {
                Text("掃描中", style = Ty.Title)
                Spacer(Modifier.height(6.dp))
                Text(
                    state.scanLabel.ifBlank { "讀取目錄" },
                    style = Ty.Mono, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                QuietAction("停止掃描") { vm.cancelScan() }
            }
            Spacer(Modifier.height(16.dp))
        } else if (state.report == null) {
            PrimaryAction("開始掃描") { vm.startScan() }
            Spacer(Modifier.height(16.dp))
        } else {
            PrimaryAction("查看 ${state.report?.results?.size ?: 0} 類可回收項目") { onGoClean() }
            Spacer(Modifier.height(6.dp))
            QuietAction("重新掃描") { vm.startScan() }
            Spacer(Modifier.height(10.dp))
        }

        Eyebrow("此刻的機況")
        Spacer(Modifier.height(6.dp))
        Panel {
            val ram = state.ram
            StatLine(
                label = "記憶體",
                value = formatBytes(ram.avail) + " 可用",
                detail = "共 ${formatBytes(ram.total)}。Android 會自行回收背景程式，佔用高不等於變慢。",
                fraction = if (ram.total > 0) ram.used.toFloat() / ram.total else 0f,
                color = T.Steel
            )
            Divider1()
            state.battery?.let { b ->
                StatLine(
                    label = "電池",
                    value = "${b.level}%",
                    detail = "${b.status} · ${b.tempC} °C · 健康度 ${b.health}",
                    fraction = b.level / 100f,
                    color = if (b.level < 20) T.Clay else T.Jade
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("交給系統處理")
        Spacer(Modifier.height(6.dp))
        Panel {
            if (Perms.canUseSystemCacheDialog()) {
                HandoffRow(
                    title = "清除全機 App 快取",
                    detail = "由系統跳出確認視窗，一次清掉所有 App 的快取資料",
                    actionLabel = "開啟"
                ) { Perms.openSystemCacheDialog(context) }
                Divider1()
            }
            HandoffRow(
                title = "儲存空間管理",
                detail = "系統內建的空間釋出建議",
                actionLabel = "開啟"
            ) { Perms.openStorageManager(context) }
        }

        Spacer(Modifier.height(16.dp))
        ExpertPanel(vm, state)

        if (state.trashCount > 0) {
            Spacer(Modifier.height(16.dp))
            Eyebrow("回收桶")
            Spacer(Modifier.height(6.dp))
            Panel {
                Text(
                    "${state.trashCount} 項 · ${formatBytes(state.trashBytes)}",
                    style = Ty.Title
                )
                Spacer(Modifier.height(6.dp))
                Text("移除的檔案會保留 30 天，期間隨時可以放回原位。", style = Ty.Caption)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuietAction("全部還原") { vm.restoreTrash() }
                    Spacer(Modifier.width(8.dp))
                    QuietAction("立即清空") { vm.emptyTrash() }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Android 11 起，/Android/data 與 /Android/obb 對所有第三方 App 關閉，" +
                "這兩處的暫存只能由系統或專家模式處理。",
            style = Ty.Mono
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HeadlineNumber(state: TidyState, reclaimable: Long) {
    val showReclaim = state.report != null
    val bytes = if (showReclaim) reclaimable else state.storage.free
    val (number, unit) = splitBytes(bytes)

    Column {
        Eyebrow(if (showReclaim) "可回收" else "可用空間")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(number, style = Ty.Display)
            Spacer(Modifier.width(6.dp))
            Text(unit, style = Ty.Body.copy(color = T.MistDim))
        }
        if (state.lastFreed > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "上次清理釋出 ${formatBytes(state.lastFreed)}",
                style = Ty.Caption.copy(color = T.Jade)
            )
        }
    }
}

@Composable
private fun ExpertPanel(vm: TidyViewModel, state: TidyState) {
    Eyebrow("專家模式")
    Spacer(Modifier.height(6.dp))
    Panel {
        when {
            !state.shizukuInstalled -> {
                Text("未偵測到 Shizuku", style = Ty.Title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "一般 App 無法終止其他程式，也碰不到別人的快取。裝上 Shizuku 並用無線偵錯啟動後，" +
                        "這裡就能真的執行清理。沒有它，其餘功能照常運作。",
                    style = Ty.Caption
                )
                Spacer(Modifier.height(10.dp))
                QuietAction("重新偵測") { vm.refreshShizuku() }
            }

            !state.shizukuGranted -> {
                Text("Shizuku 已啟動，尚未授權", style = Ty.Title)
                Spacer(Modifier.height(8.dp))
                Text("授權後才能執行快取與背景程序清理。", style = Ty.Caption)
                Spacer(Modifier.height(12.dp))
                PrimaryAction("向 Shizuku 請求授權") { ShizukuBridge.requestPermission() }
                Spacer(Modifier.height(4.dp))
                QuietAction("重新檢查") { vm.refreshShizuku() }
            }

            else -> {
                Text("已連上 Shizuku", style = Ty.Title)
                Spacer(Modifier.height(10.dp))
                HandoffRow(
                    title = "清除全機 App 快取",
                    detail = "等同 pm trim-caches，不會動到帳號與設定",
                    actionLabel = if (state.shellBusy) "執行中" else "執行"
                ) { if (!state.shellBusy) vm.trimAllCaches() }
                Divider1()
                HandoffRow(
                    title = "結束背景 App",
                    detail = "只影響背景程序。系統可能在幾秒內重新拉起常駐服務。",
                    actionLabel = if (state.shellBusy) "執行中" else "執行"
                ) { if (!state.shellBusy) vm.killBackgroundApps() }

                if (state.shellOutput.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.shellOutput, style = Ty.Caption.copy(color = T.Jade))
                }
            }
        }
    }
}
