package com.infodreams.remotelogger.storage

import com.infodreams.remotelogger.models.LogEntry
import java.io.File

interface LogStorage {
    fun initialize(sessionId: String, groupSessionId: String? = null)
    fun write(entry: LogEntry)
    fun getSessionFile(): File?
    fun getOldSessionFiles(currentSessionId: String): List<File>
}

