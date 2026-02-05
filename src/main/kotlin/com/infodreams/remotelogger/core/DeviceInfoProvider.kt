package com.infodreams.remotelogger.core

import android.content.Context
import android.os.Build
import java.io.File
import java.util.UUID

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
        // Use File-based implementation to match Flutter's logic
        // This ensures shared ID between Flutter and Native in the same app context
        try {
            val file = File(context.filesDir, "remote_logger_device_id")
            if (file.exists()) {
                val storedId = file.readText()
                if (storedId.isNotEmpty()) {
                    return storedId
                }
            }

            // Generate new ID
            val newId = UUID.randomUUID().toString()
            file.writeText(newId)
            return newId
        } catch (e: Exception) {
            return "unknown_android_${UUID.randomUUID()}"
        }
    }
}
