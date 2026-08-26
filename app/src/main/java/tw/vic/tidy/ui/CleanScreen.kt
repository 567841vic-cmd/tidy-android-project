package tw.vic.tidy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.vic.tidy.TidyState
import tw.vic.tidy.TidyViewModel
import tw.vic.tidy.core.CategoryResult
import tw.vic.tidy.core.JunkCategory
import tw.vic.tidy.core.RecycleBin
import tw.vic.tidy.core.formatBytes
import tw.vic.tidy.core.shortPath
import tw.vic.tidy.core.splitBytes
import tw.vic.tidy.ui.components.Divider1
import tw.vic.tidy.ui.components.Eyebrow
import tw.vic.tidy.ui.components.Panel
import tw.vic.tidy.ui.components.PrimaryAction
import tw.vic.tidy.ui.components.QuietAction
import tw.vic.tidy.ui.components.Stratum
import tw.vic.tidy.ui.components.StratumBar
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.Ty

@Composable
fun CleanScreen(vm: TidyViewModel, state: TidyState) {
    val report = state.report
    var confirming by remember { mutableStateOf(false) }

    if (report == null) {
        EmptyClean(state, onScan = { vm.startScan() })
        return
    }

    val strata = report.results.map {
        Stratum(it.category.title, it.bytes, categoryColor(it.category), reclaimable = true)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Eyebrow("可回收總量")
                Spacer(Modifier.height(6.dp))
                val (n, u) = splitBytes(report.totalBytes)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(n, style = Ty.Display)
                    Spacer(Modifier.width(6.dp))
                    Text(u, style = Ty.Body.copy(color = T.MistDim))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "掃過 ${report.filesScanned} 個檔案，用了 ${report.elapsedMs / 1000} 秒",
                    style = Ty.Caption
                )
                Spacer(Modifier.height(18.dp))
                StratumBar(strata = strata, sweep = 0f, scanning = false)
                Spacer(Modifier.height(6.dp))
                Text("以可回收總量為比例，非全機空間。", style = Ty.Mono)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuietAction("建議選項") { vm.selectDefaults() }
                    QuietAction("全部取消") { vm.selectNone() }
                }
                Spacer(Modifier.height(6.dp))
            }

            items(report.results, key = { it.category.name }) { result ->
                CategoryCard(
                    result = result,
                    checked = result.category in state.selected,
                    onToggle = { vm.toggleCategory(result.category) }
                )
                Spacer(Modifier.height(10.dp))
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryAction(
                text = when {
                    state.cleaning -> "移動中…"
                    state.selected.isEmpty() -> "尚未選擇項目"
                    else -> "移到回收桶（${formatBytes(state.selectedBytes)}）"
                },
                enabled = state.selected.isNotEmpty() && !state.cleaning
            ) { confirming = true }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = T.Ink800,
            title = { Text("移到回收桶", style = Ty.Title) },
            text = {
                Text(
                    "${state.selectedItems.size} 個項目會移到回收桶，保留 " +
                        "${RecycleBin.RETENTION_DAYS} 天後才真正刪除。" +
                        "空資料夾沒有資料，會直接移除。",
                    style = Ty.Body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    vm.cleanSelected()
                }) { Text("移動", style = Ty.Body.copy(color = T.Brass)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text("取消", style = Ty.Body.copy(color = T.MistDim))
                }
            }
        )
    }
}

@Composable
private fun EmptyClean(state: TidyState, onScan: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (state.lastFreed > 0) "清理完成" else "還沒有掃描結果",
            style = Ty.Display.copy(fontSize = 28.sp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.lastFreed > 0)
                "這次釋出 ${formatBytes(state.lastFreed)}。再掃一次可以確認還有沒有殘留。"
            else
                "掃描會逐一讀取共用儲存空間的檔案清單，過程中不會刪除任何東西。",
            style = Ty.BodyDim
        )
        Spacer(Modifier.height(20.dp))
        PrimaryAction("開始掃描", enabled = !state.scanning) { onScan() }
    }
}

@Composable
private fun CategoryCard(
    result: CategoryResult,
    checked: Boolean,
    onToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = categoryColor(result.category)

    Panel {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(
                        Modifier
                            .width(3.dp)
                            .height(16.dp)
                    ) { drawRect(color) }
                    Spacer(Modifier.width(8.dp))
                    Text(result.category.title, style = Ty.Title)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${result.count} 項 · ${formatBytes(result.bytes)}",
                    style = Ty.Caption
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = T.Brass,
                    uncheckedColor = T.MistFaint,
                    checkmarkColor = T.Ink900
                )
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(result.category.hint, style = Ty.Caption)

        if (result.category == JunkCategory.BIG_FILE ||
            result.category == JunkCategory.BACKUP_RESIDUE ||
            result.category == JunkCategory.APK_RESIDUE
        ) {
            Spacer(Modifier.height(6.dp))
            Text("預設不勾選，請先確認內容。", style = Ty.Caption.copy(color = T.Clay))
        }

        Spacer(Modifier.height(10.dp))
        Divider1()
        Spacer(Modifier.height(8.dp))

        val preview = if (expanded) result.items.take(60) else result.items.take(3)
        preview.forEach { item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    shortPath(item.path),
                    style = Ty.Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.size > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(formatBytes(item.size), style = Ty.Mono)
                }
            }
        }
        if (result.count > 3) {
            QuietAction(
                if (expanded) "收合" else "展開全部 ${result.count} 項"
            ) { expanded = !expanded }
        }
    }
}
