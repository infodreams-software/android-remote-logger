package com.infodreams.remotelogger.models

import com.google.gson.Gson

data class SessionInfo(
    val sessionId: String,
    val deviceId: String,
    val startTime: Long,
    val deviceMetadata: Map<String, Any>,
    val userId: String? = null,
    val groupSessionId: String? = null
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}
