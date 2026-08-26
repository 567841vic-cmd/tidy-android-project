package tw.vic.tidy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import rikka.shizuku.Shizuku
import tw.vic.tidy.ui.BatteryScreen
import tw.vic.tidy.ui.CleanScreen
import tw.vic.tidy.ui.HomeScreen
import tw.vic.tidy.ui.components.GlyphCell
import tw.vic.tidy.ui.components.GlyphFunnel
import tw.vic.tidy.ui.components.GlyphStrata
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.TidyTheme
import tw.vic.tidy.ui.theme.Ty

class MainActivity : ComponentActivity() {

    private var onShizukuResult: (() -> Unit)? = null

    private val shizukuListener =
        Shizuku.OnRequestPermissionResultListener { _, _ -> onShizukuResult?.invoke() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
        } catch (t: Throwable) {
            // Shizuku is optional; without it the expert panel simply stays closed.
        }

        setContent {
            TidyTheme {
                val vm: TidyViewModel = viewModel()
                onShizukuResult = { vm.refreshShizuku() }
                AppRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onShizukuResult?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuListener)
        } catch (t: Throwable) {
            // Already gone.
        }
    }
}

@Composable
fun AppRoot(vm: TidyViewModel) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        val message = state.notice
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.dismissNotice()
        }
    }

    Scaffold(
        containerColor = T.Ink900,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = T.Ink800, tonalElevation = 0.dp) {
                TabItem(tab == 0, "剖面", { tab = 0 }) { GlyphStrata(it) }
                TabItem(tab == 1, "清理", { tab = 1 }) { GlyphFunnel(it) }
                TabItem(tab == 2, "電池", { tab = 2 }) { GlyphCell(it) }
            }
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (tab) {
                0 -> HomeScreen(vm, state, onGoClean = { tab = 1 })
                1 -> CleanScreen(vm, state)
                else -> BatteryScreen(vm, state)
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    glyph: @Composable (Boolean) -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { glyph(selected) },
        label = { Text(label, style = Ty.Caption) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = T.Ink700,
            selectedTextColor = T.Brass,
            unselectedTextColor = T.MistFaint
        )
    )
}
