package com.baohao.esimkeeper

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.baohao.esimkeeper.ui.ESimKeeperApp
import com.baohao.esimkeeper.ui.ESimKeeperTheme
import com.baohao.esimkeeper.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyHighestRefreshRate()
        setContent {
            val darkMode = viewModel.isDarkMode
            SideEffect {
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkMode
                    isAppearanceLightNavigationBars = !darkMode
                }
            }
            ESimKeeperTheme(darkTheme = darkMode) {
                ESimKeeperApp(viewModel = viewModel)
            }
        }
    }

    /**
     * Ask the system to drive this window at the display's highest available
     * refresh rate instead of the default 60Hz cap, so card scrolling stays
     * smooth on 90/120Hz phones. The system still falls back gracefully when
     * the device only supports 60Hz.
     *
     * We only consider modes that keep the *current* physical resolution, so we
     * never trade resolution for a higher refresh rate. Both `preferredRefreshRate`
     * and `preferredDisplayModeId` are set on a fresh copy of the window
     * attributes and re-assigned, which is what triggers the WindowManager to
     * apply the change.
     *
     * Check it took effect with:
     *   adb shell dumpsys SurfaceFlinger | grep -i refresh
     * or filter logcat for the "RefreshRate" tag below.
     */
    private fun applyHighestRefreshRate() {
        val targetDisplay: Display? =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
        val activeDisplay = targetDisplay ?: return

        val modes = activeDisplay.supportedModes
        if (modes.isNullOrEmpty()) return

        val currentMode = activeDisplay.mode
        // Keep the same resolution; only vary the refresh rate.
        val bestMode = modes
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: modes.maxByOrNull { it.refreshRate }
            ?: return

        if (bestMode.refreshRate <= currentMode.refreshRate) {
            Log.d("RefreshRate", "Already at best: ${currentMode.refreshRate}Hz")
            return
        }

        window.attributes = window.attributes.let { params ->
            params.preferredDisplayModeId = bestMode.modeId
            params.preferredRefreshRate = bestMode.refreshRate
            params
        }
        Log.d(
            "RefreshRate",
            "Requested ${bestMode.refreshRate}Hz (modeId=${bestMode.modeId}), was ${currentMode.refreshRate}Hz",
        )
    }
}
