package com.womensafety.sos.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Immutable
data class WindowInfo(val widthDp: Int, val heightDp: Int) {
    val isLandscape: Boolean get() = widthDp > heightDp
    val useRail: Boolean get() = widthDp >= 600        // tablets, foldables, wide landscape
    val useBottomBar: Boolean get() = !useRail         // all phones in portrait
    val useTwoPane: Boolean get() = widthDp >= 600 && isLandscape
}

@Composable
fun rememberWindowInfo(): WindowInfo {
    val cfg = LocalConfiguration.current
    return remember(cfg.screenWidthDp, cfg.screenHeightDp) {
        WindowInfo(cfg.screenWidthDp, cfg.screenHeightDp)
    }
}
