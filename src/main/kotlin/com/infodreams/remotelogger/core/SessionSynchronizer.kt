package com.infodreams.remotelogger.core

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.UUID

class SessionSynchronizer(private val context: Context) {
    private val lockFileName = "session.lock"
    // private val freshnessThresholdMs = 5000L // Deprecated: using PID check

    /**
     * Returns the synchronized session ID.
     *
     * Algorithm:
     * 1. Check if `session.lock` exists in app documents.
     * 2. Read content (Format: "PID:SESSION_ID").
     * 3. If file PID matches current process PID, return SESSION_ID.
     * 4. Else, generate new ID, overwrite file with "PID:NEW_ID", and return new ID.
     */
    fun getOrGenerateSessionId(): String {
        val lockFile = File(context.filesDir, lockFileName)
        val currentPid = android.os.Process.myPid()

        if (lockFile.exists()) {
            try {
                val content = lockFile.readText().trim()
                val parts = content.split(":")
                
                if (parts.size == 2) {
                    val filePid = parts[0].toIntOrNull()
                    val sessionId = parts[1]
                    
                    if (filePid == currentPid && sessionId.isNotEmpty()) {
                        android.util.Log.i("SessionSynchronizer", "Reusing synchronized session: $sessionId (PID match: $currentPid)")
                        return sessionId
                    }
                }
            } catch (e: Exception) {
                // Ignore read/parse errors
            }
        }

        // Generate new ID if file doesn't exist, is invalid, or PID mismatch (started new process)
        // Note: We use UUID for session ID generation. 
        // Flutter side will also generate UUID if it runs first.
        val newId = UUID.randomUUID().toString()
        try {
            android.util.Log.i("SessionSynchronizer", "Generating new synchronized session: $newId (PID: $currentPid)")
            lockFile.writeText("$currentPid:$newId")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return newId
    }
}
