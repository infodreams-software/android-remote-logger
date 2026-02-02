package com.infodreams.remotelogger.models

sealed class RemoteLoggerEvent {
    data class Success(val message: String, val fileUrl: String? = null) : RemoteLoggerEvent()
    data class Error(val message: String, val error: Throwable? = null) : RemoteLoggerEvent()
}
