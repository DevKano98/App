package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.StateFlow

object FocusVpnHelper {

    /**
     * Live StateFlow reflecting whether FocusVpnService is actively running and connected.
     */
    val isVpnRunning: StateFlow<Boolean> = FocusVpnService.isVpnRunning

    /**
     * Checks if system VPN permission has been prepared/granted by user.
     * Returns null if already granted, or an Intent to present to the user via startActivityForResult.
     */
    fun checkVpnPrepareIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Starts the FocusVpnService.
     */
    fun startVpnService(context: Context) {
        val intent = Intent(context, FocusVpnService::class.java).apply {
            action = FocusVpnService.ACTION_START_VPN
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Stops the FocusVpnService.
     */
    fun stopVpnService(context: Context) {
        val intent = Intent(context, FocusVpnService::class.java).apply {
            action = FocusVpnService.ACTION_STOP_VPN
        }
        context.startService(intent)
    }
}
