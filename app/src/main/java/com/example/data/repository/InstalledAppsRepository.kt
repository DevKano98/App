package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsRepository(private val context: Context) {

    suspend fun getLaunchableInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<InstalledAppInfo>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            // Exclude our own app
            if (packageName == context.packageName) continue
            if (seenPackages.contains(packageName)) continue

            seenPackages.add(packageName)

            val appName = resolveInfo.loadLabel(pm)?.toString() ?: packageName
            val iconBitmap = try {
                val drawable = resolveInfo.loadIcon(pm)
                drawableToBitmap(drawable)
            } catch (e: Exception) {
                null
            }

            val isSystemApp = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) {
                false
            }

            appList.add(
                InstalledAppInfo(
                    packageName = packageName,
                    appName = appName,
                    iconBitmap = iconBitmap,
                    isSystemApp = isSystemApp
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int = 96): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
