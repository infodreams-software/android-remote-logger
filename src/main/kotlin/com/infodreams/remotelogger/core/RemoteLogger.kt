package com.infodreams.remotelogger.core

import android.content.Context
import android.util.Log
import com.infodreams.remotelogger.models.LogEntry
import com.infodreams.remotelogger.models.RemoteLoggerEvent
import com.infodreams.remotelogger.models.SessionInfo
import com.infodreams.remotelogger.storage.FileLogStorage
import com.infodreams.remotelogger.storage.LogStorage
import com.infodreams.remotelogger.uploader.LogUploader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

class RemoteLogger private constructor() {

    private var storage: LogStorage? = null
    private var uploader: LogUploader? = null
    private var deviceInfoProvider: DeviceInfoProvider? = null
    private var sessionManager: SessionManager? = null
    private var currentSession: SessionInfo? = null
    private var isInitialized = false
    private var isEnabled = true
    private var eventListener: ((RemoteLoggerEvent) -> Unit)? = null
    private var remotePath: String? = null

    companion object {
        val instance: RemoteLogger by lazy { RemoteLogger() }
        private const val TAG = "RemoteLogger"
    }

    private var uploadTimer: java.util.Timer? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    fun initialize(
        context: Context,
        sessionId: String? = null,
        uploader: LogUploader? = null,
        isEnabled: Boolean = true,
        groupSessionId: String? = null,
        autoUploadIntervalMillis: Long? = null,
        remotePath: String? = null
    ) {
        if (isInitialized) return

        this.isEnabled = isEnabled
        if (!isEnabled) {
            Log.i(TAG, "RemoteLogger disabled.")
            isInitialized = true
            return
        }

        deviceInfoProvider = DeviceInfoProvider(context)
        this.uploader = uploader
        this.remotePath = remotePath
        
        // Determine groupSessionId
        var finalGroupId = groupSessionId
        if (finalGroupId == null) {
            // If no group ID provided, try to synchronize
            try {
                val synchronizer = SessionSynchronizer(context)
                finalGroupId = synchronizer.getOrGenerateSessionId()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to synchronize session", e)
            }
        }

        sessionManager = SessionManager()
        val finalSessionId = sessionId ?: sessionManager!!.sessionId
        
        storage = FileLogStorage(context)
        storage?.initialize(finalSessionId, finalGroupId)

        val deviceId = deviceInfoProvider?.getDeviceId() ?: "unknown"
        val metadata = deviceInfoProvider?.getDeviceMetadata() ?: emptyMap()

        currentSession = SessionInfo(
            sessionId = finalSessionId,
            deviceId = deviceId,
            startTime = sessionManager?.startTime ?: System.currentTimeMillis(),
            deviceMetadata = metadata,
            groupSessionId = finalGroupId
        )

        // Attempt to upload device info if uploader is present
        // Attempt to upload device info if uploader is present
        scope.launch {
            try {
                uploader?.uploadDeviceInfo(deviceId, metadata, remotePath)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to upload device info", e)
            }
        }

        scope.launch {
            processOldSessions()
        }

        if (autoUploadIntervalMillis != null) {
            uploadTimer = java.util.Timer()
            uploadTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
                override fun run() {
                    uploadCurrentSession()
                }
            }, autoUploadIntervalMillis, autoUploadIntervalMillis)
        }

        isInitialized = true
        Log.i(TAG, "RemoteLogger initialized. Session: ${currentSession?.sessionId}")
        Log.i(TAG, "RemoteLogger initialized. Session: ${currentSession?.sessionId}")
    }

    private suspend fun processOldSessions() {
        if (currentSession == null || storage == null || uploader == null) return

        try {
            val oldFiles = storage!!.getOldSessionFiles(currentSession!!.sessionId)
            for (file in oldFiles) {
                Log.i(TAG, "Uploading found orphan session: ${file.path}")
                
                 val filename = file.name
                 // clean up filename to extract simpler session id if possible, 
                 // though SessionInfo mostly needs sessionId for unique ID.
                 // log_XYZ_android.jsonl -> XYZ
                 val sessionId = filename
                     .replace("log_", "")
                     .replace(".android.jsonl", "")
                     .replace(Regex("_.*"), "") // Remove group suffix if present

                val recoveredSession = SessionInfo(
                    sessionId = sessionId,
                    deviceId = currentSession!!.deviceId,
                    startTime = 0, // Unknown
                    deviceMetadata = currentSession!!.deviceMetadata,
                    userId = currentSession!!.userId
                )

                try {
                    uploader!!.uploadSession(file, recoveredSession, remotePath)
                    // Now that upload is done (suspend), we can safely delete
                    file.delete()
                    notifyEvent(RemoteLoggerEvent.Success("Orphan session uploaded: $sessionId"))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload orphan session: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process old sessions", e)
        }
    }

    fun getDeviceId(): String? = currentSession?.deviceId
    
    fun setEventListener(listener: (RemoteLoggerEvent) -> Unit) {
        this.eventListener = listener
    }

    fun log(
        message: String,
        level: String = "INFO",
        tag: String = "APP",
        payload: Map<String, Any>? = null
    ) {
        if (!isInitialized || !isEnabled) {
            if (!isEnabled && isInitialized) return // Silent return if explicitly disabled
            Log.w(TAG, "RemoteLogger not initialized. Dropping log: $message")
            return
        }

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val timeString = dateFormat.format(Date(now))

        val entry = LogEntry(
            timestamp = now,
            time = timeString,
            level = level,
            tag = tag,
            message = message,
            payload = payload
        )

        storage?.write(entry)
    }

    fun uploadCurrentSession() {
        if (!isInitialized || !isEnabled || currentSession == null) return

        scope.launch {
            try {
                val file = storage?.getSessionFile()
                if (file != null && file.exists()) {
                    uploader?.uploadSession(file, currentSession!!, remotePath)
                    notifyEvent(RemoteLoggerEvent.Success("Session uploaded successfully", file.absolutePath))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload session", e)
                notifyEvent(RemoteLoggerEvent.Error("Failed to upload session", e))
            }
        }
    }
    
    fun identifyUser(userId: String) {
        if (!isInitialized || !isEnabled || currentSession == null) return

        scope.launch {
            try {
                uploader?.identifyUser(currentSession!!.deviceId, userId)
                
                // Update current session with user ID
                currentSession = currentSession!!.copy(userId = userId)
                
                Log.i(TAG, "User identified: $userId")
                notifyEvent(RemoteLoggerEvent.Success("User identified: $userId"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to identify user", e)
                notifyEvent(RemoteLoggerEvent.Error("Failed to identify user", e))
            }
        }
    }
    
    private fun notifyEvent(event: RemoteLoggerEvent) {
        eventListener?.invoke(event)
    }

    fun reset() {
        uploadTimer?.cancel()
        uploadTimer = null
        isInitialized = false
        currentSession = null
        storage = null
        uploader = null
        sessionManager = null
        deviceInfoProvider = null
        remotePath = null
    }
}
