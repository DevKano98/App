package com.example.data.model

import android.graphics.Bitmap

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val iconBitmap: Bitmap? = null,
    val isSystemApp: Boolean = false
)
