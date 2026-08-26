package tw.vic.tidy.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.vic.tidy.TidyState
import tw.vic.tidy.TidyViewModel
import tw.vic.tidy.core.AppUsage
import tw.vic.tidy.core.Perms
import tw.vic.tidy.core.formatDuration
import tw.vic.tidy.ui.components.Divider1
import tw.vic.tidy.ui.components.Eyebrow
import tw.vic.tidy.ui.components.HandoffRow
import tw.vic.tidy.ui.components.Panel
import tw.vic.tidy.ui.components.PrimaryAction
import tw.vic.tidy.ui.components.QuietAction
import tw.vic.tidy.ui.components.ThinMeter
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.Ty

@Composable
fun BatteryScreen(vm: TidyViewModel, state: TidyState) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.refreshUsage() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Eyebrow("電池")
        Spacer(Modifier.height(8.dp))

        state.battery?.let { b ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${b.level}", style = Ty.Display)
                Spacer(Modifier.width(4.dp))
                Text("%", style = Ty.Body.copy(color = T.MistDim))
            }
            Spacer(Modifier.height(12.dp))
            ThinMeter(
                fraction = b.level / 100f,
                color = if (b.level < 20) T.Clay else T.Jade
            )
            Spacer(Modifier.height(16.dp))
            Panel {
                Reading("狀態", b.status)
                Divider1()
                Reading("電源", b.plugged)
                Divider1()
                Reading("溫度", "${b.tempC} °C")
                Divider1()
                Reading("電壓", "${b.voltageMv} mV")
                if (b.currentMa != 0) {
                    Divider1()
                    Reading("即時電流", "${b.currentMa} mA")
                }
                Divider1()
                Reading("健康度", b.health)
            }
        }

        Spacer(Modifier.height(22.dp))
        Eyebrow("耗電推估 · 近 24 小時")
        Spacer(Modifier.height(6.dp))

        if (!state.hasUsageAccess) {
            Panel {
                Text("需要使用權限存取", style = Ty.Title)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android 不開放第三方 App 讀取各程式的實際耗電量。這裡改用前景使用時間推估，" +
                        "需要你在系統設定裡把「使用權限存取」打開。",
                    style = Ty.Caption
                )
                Spacer(Modifier.height(14.dp))
                PrimaryAction("前往設定") { Perms.openUsageAccess(context) }
                Spacer(Modifier.height(4.dp))
                QuietAction("我已經開好了，重新檢查") { vm.refreshUsage() }
            }
        } else if (state.usage.isEmpty()) {
            Panel {
                Text("近 24 小時沒有可用的使用紀錄。", style = Ty.BodyDim)
                Spacer(Modifier.height(10.dp))
                QuietAction("重新讀取") { vm.refreshUsage() }
            }
        } else {
            val top = state.usage.first().foregroundMs.coerceAtLeast(1L)
            Panel {
                state.usage.forEachIndexed { index, app ->
                    if (index > 0) Divider1()
                    UsageRow(app, top) { Perms.openAppDetails(context, app.pkg) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "前景時間長不等於耗電高，但兩者高度相關。想確認實際耗電，請看系統設定裡的電池用量。",
                style = Ty.Mono
            )
        }

        Spacer(Modifier.height(22.dp))
        Eyebrow("省電設定")
        Spacer(Modifier.height(6.dp))
        Panel {
            HandoffRow(
                title = "電池最佳化",
                detail = "逐一設定哪些 App 可以在背景活動",
                actionLabel = "開啟"
            ) { Perms.openBatteryOptimization(context) }
            Divider1()
            HandoffRow(
                title = "自啟動管理（ColorOS）",
                detail = "OPPO 把背景喚醒的控制權放在自家的手機管家裡",
                actionLabel = "開啟"
            ) { Perms.openOppoAutoStart(context) }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Reading(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Ty.BodyDim, modifier = Modifier.weight(1f))
        Text(value, style = Ty.Body)
    }
}

@Composable
private fun UsageRow(app: AppUsage, topMs: Long, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                app.label,
                style = Ty.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(formatDuration(app.foregroundMs), style = Ty.Body.copy(fontSize = 13.sp))
        }
        Spacer(Modifier.height(7.dp))
        ThinMeter(
            fraction = app.foregroundMs.toFloat() / topMs,
            color = if (app.isSystem) T.Steel else T.Brass
        )
    }
}
