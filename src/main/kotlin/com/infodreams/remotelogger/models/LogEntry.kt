package com.infodreams.remotelogger.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LogEntry(
    val timestamp: Long,
    val time: String,
    val level: String,
    val tag: String,
    val message: String,
    val payload: Map<String, Any>? = null
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): LogEntry {
            val type = object : TypeToken<LogEntry>() {}.type
            return Gson().fromJson(json, type)
        }
    }
}
