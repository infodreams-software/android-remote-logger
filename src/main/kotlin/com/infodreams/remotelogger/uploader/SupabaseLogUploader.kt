package com.infodreams.remotelogger.uploader

import com.infodreams.remotelogger.models.SessionInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Implementation of LogUploader using Supabase.
 * Requires Supabase to be initialized in the host application.
 */
class SupabaseLogUploader(
    private val supabase: SupabaseClient,
    private val storageBucket: String = "remote_logs",
    private val sessionsTable: String = "remote_log_sessions",
    private val deviceLinksTable: String = "remote_log_device_links"
) : LogUploader {



    override suspend fun uploadSession(logFile: File, sessionInfo: SessionInfo, path: String?) {
        try {
            val suffix = if (sessionInfo.groupSessionId != null) "_${sessionInfo.groupSessionId}" else ""
            val pathPrefix = if (path != null) "$path/" else ""
            val fileName = "$pathPrefix${sessionInfo.deviceId}/log_${sessionInfo.sessionId}$suffix.android.jsonl"
            val bucket = supabase.storage.from(storageBucket)
            
            bucket.upload(fileName, logFile.readBytes(), upsert = true)
            
            val logFileUrl = bucket.publicUrl(fileName)

            val startTimeIso = toIso8601(sessionInfo.startTime)
            val uploadTimeIso = toIso8601(System.currentTimeMillis())

            val row = mapOf(
                "session_id" to sessionInfo.sessionId,
                "device_id" to sessionInfo.deviceId,
                "start_time" to startTimeIso,
                "user_id" to sessionInfo.userId,
                "custom_data" to sessionInfo.deviceMetadata,
                "log_file_url" to logFileUrl,
                "uploaded_at" to uploadTimeIso
            )

            supabase.postgrest.from(sessionsTable).upsert(row)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseLogUploader", "Upload failed", e)
        }
    }

    override suspend fun identifyUser(deviceId: String, userId: String) {
        try {
            val row = mapOf(
                "device_id" to deviceId,
                "user_id" to userId,
                "linked_at" to toIso8601(System.currentTimeMillis())
            )
            supabase.postgrest.from(deviceLinksTable).insert(row)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseLogUploader", "Identify User failed", e)
        }
    }

    override suspend fun uploadDeviceInfo(deviceId: String, deviceInfo: Map<String, Any>, path: String?) {
        try {
            val pathPrefix = if (path != null) "$path/" else ""
            val fileName = "$pathPrefix$deviceId/device_info.json"
            val json = com.google.gson.Gson().toJson(deviceInfo)
            
            supabase.storage.from(storageBucket).upload(
                fileName, 
                json.toByteArray(), 
                upsert = true
            )
        } catch (e: Exception) {
            android.util.Log.e("SupabaseLogUploader", "Device Info upload failed", e)
        }
    }

    private fun toIso8601(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date(timestamp))
    }
}
