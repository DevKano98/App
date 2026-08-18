package com.example.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.example.receiver.FocusDeviceAdminReceiver

/**
 * DevicePolicyController manages Device Owner policy enforcement during active lock sessions.
 *
 * SAFETY MANDATE:
 * Every call checks `dpm.isDeviceOwnerApp(packageName)` and cleanly no-ops if false.
 * Prevents crashes and never pretends protection is active when Device Owner is unprovisioned.
 */
class DevicePolicyController(private val context: Context) {

    companion object {
        private const val TAG = "DevicePolicyController"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }

    private val dpm: DevicePolicyManager? by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    }

    val adminComponent: ComponentName by lazy {
        ComponentName(context, FocusDeviceAdminReceiver::class.java)
    }

    /**
     * Verifies if FocusLock is currently granted Device Owner status on this device.
     */
    fun isDeviceOwner(): Boolean {
        val manager = dpm ?: return false
        return try {
            manager.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query device owner status: ${e.message}")
            false
        }
    }

    /**
     * Applies device-owner enforcement restrictions when a LockSession is ACTIVE:
     * 1. Suspends Play Store package and all restricted AppRule packages.
     * 2. Adds user restrictions DISALLOW_INSTALL_APPS and DISALLOW_UNINSTALL_APPS.
     */
    fun applyLockRestrictions(blockedPackages: List<String>) {
        if (!isDeviceOwner()) {
            Log.d(TAG, "Device Owner not present — skipping DevicePolicyManager restrictions cleanly.")
            return
        }

        val manager = dpm ?: return

        try {
            // 1. Add Install / Uninstall restrictions
            manager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            manager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)

            // 2. Suspend Play Store and blocked app packages
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val packagesToSuspend = (blockedPackages + PLAY_STORE_PACKAGE)
                    .filter { it.isNotBlank() && it != context.packageName }
                    .distinct()
                    .toTypedArray()

                if (packagesToSuspend.isNotEmpty()) {
                    val unhandled = manager.setPackagesSuspended(adminComponent, packagesToSuspend, true)
                    Log.i(TAG, "Suspended ${packagesToSuspend.size} packages (unhandled: ${unhandled?.size ?: 0})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying lock restrictions via DPM: ${e.message}", e)
        }
    }

    /**
     * Clears all device-owner restrictions when a LockSession ends or is overridden:
     * 1. Clears DISALLOW_INSTALL_APPS and DISALLOW_UNINSTALL_APPS user restrictions.
     * 2. Un-suspends all suspended application packages.
     */
    fun clearLockRestrictions(blockedPackages: List<String> = emptyList()) {
        if (!isDeviceOwner()) {
            Log.d(TAG, "Device Owner not present — skipping DPM clear restrictions cleanly.")
            return
        }

        val manager = dpm ?: return

        try {
            // 1. Clear user restrictions
            manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            manager.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)

            // 2. Un-suspend Play Store and blocked app packages
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val packagesToUnsuspend = (blockedPackages + PLAY_STORE_PACKAGE)
                    .filter { it.isNotBlank() && it != context.packageName }
                    .distinct()
                    .toTypedArray()

                if (packagesToUnsuspend.isNotEmpty()) {
                    manager.setPackagesSuspended(adminComponent, packagesToUnsuspend, false)
                    Log.i(TAG, "Un-suspended ${packagesToUnsuspend.size} packages")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing lock restrictions via DPM: ${e.message}", e)
        }
    }
}
