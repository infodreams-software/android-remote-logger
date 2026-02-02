package com.infodreams.remotelogger.uploader

import com.infodreams.remotelogger.models.SessionInfo
import java.io.File

interface LogUploader {
    suspend fun uploadSession(logFile: File, sessionInfo: SessionInfo, path: String? = null)
    suspend fun identifyUser(deviceId: String, userId: String)
    suspend fun uploadDeviceInfo(deviceId: String, deviceInfo: Map<String, Any>, path: String? = null)
}
