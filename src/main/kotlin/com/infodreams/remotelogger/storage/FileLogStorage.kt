package com.infodreams.remotelogger.storage

import android.content.Context
import com.infodreams.remotelogger.models.LogEntry
import java.io.File
import java.io.FileWriter
import java.io.IOException

class FileLogStorage(private val context: Context) : LogStorage {
    private var currentFile: File? = null

    override fun initialize(sessionId: String, groupSessionId: String?) {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        // Appending .android.jsonl as requested to distinguish from Flutter logs
        val suffix = if (groupSessionId != null) "_$groupSessionId" else ""
        currentFile = File(logDir, "log_$sessionId$suffix.android.jsonl")
    }

    override fun write(entry: LogEntry) {
        currentFile?.let { file ->
            try {
                val writer = FileWriter(file, true)
                writer.append(entry.toJson()).append("\n")
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun getSessionFile(): File? {
        return currentFile
    }

    override fun getOldSessionFiles(currentSessionId: String): List<File> {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) return emptyList()

        return logDir.listFiles { file ->
            val name = file.name
            name.startsWith("log_") &&
                    name.endsWith(".android.jsonl") &&
                    !name.contains(currentSessionId)
        }?.toList() ?: emptyList()
    }


}
