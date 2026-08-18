package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager

object AccessibilityHelper {

    /**
     * Checks if AppAccessibilityService is actively enabled in Android System Settings.
     * Uses both AccessibilityManager enabled services check and secure settings query.
     */
    fun isAppAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false

        // 1. Check active accessibility services list
        val runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC or AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val targetComponent = ComponentName(context, AppAccessibilityService::class.java)
        for (service in runningServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == targetComponent.packageName &&
                serviceInfo.name == targetComponent.className
            ) {
                return true
            }
        }

        // 2. Fallback check via secure settings string splitter
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedFlattened = targetComponent.flattenToString()
            val expectedShort = targetComponent.flattenToShortString()

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServicesSetting)
            while (splitter.hasNext()) {
                val serviceName = splitter.next()
                if (serviceName.equals(expectedFlattened, ignoreCase = true) ||
                    serviceName.equals(expectedShort, ignoreCase = true)
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Context/permission handling
        }

        return false
    }

    /**
     * Creates an intent to navigate directly to System Accessibility Settings.
     */
    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
