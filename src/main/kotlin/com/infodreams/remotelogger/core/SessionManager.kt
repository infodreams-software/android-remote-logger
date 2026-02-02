package com.infodreams.remotelogger.core

import java.util.UUID

class SessionManager {
    val sessionId: String = UUID.randomUUID().toString()
    val startTime: Long = System.currentTimeMillis()
}
