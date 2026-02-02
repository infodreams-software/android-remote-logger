package com.infodreams.remotelogger.core

import android.content.Context
import android.os.Build

class DeviceInfoProvider(private val context: Context) {

    fun getDeviceMetadata(): Map<String, Any> {
        return mapOf(
            "platform" to "android",
            "model" to Build.MODEL,
            "brand" to Build.BRAND,
            "version" to Build.VERSION.RELEASE,
            "sdkInt" to Build.VERSION.SDK_INT,
            "id" to Build.ID
        )
    }

    fun getDeviceId(): String {
        // Using ANDROID_ID as a persistent ID
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_android"
    }
}
