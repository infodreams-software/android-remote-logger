package com.infodreams.remotelogger.uploader

import com.infodreams.remotelogger.models.SessionInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import kotlinx.serialization.json.buildJsonArray
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

            // Use JsonObject because Map<String, Any> is not serializable by kotlinx.serialization
            val row = buildJsonObject {
                put("session_id", sessionInfo.sessionId)
                put("device_id", sessionInfo.deviceId)
                put("start_time", startTimeIso)
                put("user_id", sessionInfo.userId)
                put("log_file_url", logFileUrl)
                put("uploaded_at", uploadTimeIso)
                
                // Convert Map<String, Any> to JsonObject for custom_data
                val metadataJson = sessionInfo.deviceMetadata.toJsonElement()
                put("custom_data", metadataJson)
            }

            supabase.postgrest.from(sessionsTable).upsert(row) {
                // Force return type to avoid Any serialization issues?
                // Actually upsert just takes the object.
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseLogUploader", "Upload failed for session ${sessionInfo.sessionId}: ${e.message}", e)
        }
    }

    override suspend fun identifyUser(deviceId: String, userId: String) {
        try {
            // Map<String, String> works fine, but let's be consistent
            val row = buildJsonObject {
                put("device_id", deviceId)
                put("user_id", userId)
                put("linked_at", toIso8601(System.currentTimeMillis()))
            }
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

    // Helper to convert Any? to JsonElement
    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            null -> kotlinx.serialization.json.JsonNull
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Map<*, *> -> {
                buildJsonObject {
                    for ((k, v) in this@toJsonElement) {
                        put(k.toString(), v.toJsonElement())
                    }
                }
            }
            is List<*> -> {
                kotlinx.serialization.json.buildJsonArray {
                    for (item in this@toJsonElement) {
                        add(item.toJsonElement())
                    }
                }
            }
            else -> JsonPrimitive(this.toString()) // Fallback
        }
    }
}
