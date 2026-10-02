package com.shilapi.xcertplay.network

import android.content.Context
import java.io.IOException

@Suppress("UNUSED_PARAMETER")
class WifiP2pGroupManager(context: Context, onDiagnostic: (String) -> Unit = {}) : WirelessHotspotManager {
    override fun start(timeoutMillis: Long): WirelessHotspotInfo =
        throw IOException("The KitKat build requires a built-in car hotspot; configure it in Connection setup")

    override fun close() = Unit
}
