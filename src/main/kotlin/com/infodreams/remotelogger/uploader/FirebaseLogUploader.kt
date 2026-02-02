package com.infodreams.remotelogger.uploader

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.infodreams.remotelogger.models.SessionInfo
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

/**
 * Implementation of LogUploader using Firebase Storage and Firestore.
 * Requires Firebase to be initialized in the host application.
 */
class FirebaseLogUploader(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LogUploader {

    override suspend fun uploadSession(logFile: File, sessionInfo: SessionInfo, path: String?) {
        // TODO: Implement Firebase upload using suspend/await
        // val basePath = if (path != null) path else "logs"
        // val ref = storage.reference.child("$basePath/${sessionInfo.deviceId}/${sessionInfo.sessionId}.jsonl")
        // ...
    }

    override suspend fun identifyUser(deviceId: String, userId: String) {
         // TODO: Implement Firebase identifyUser using suspend/await
    }

    override suspend fun uploadDeviceInfo(deviceId: String, deviceInfo: Map<String, Any>, path: String?) {
         // TODO: Implement Firebase uploadDeviceInfo using suspend/await
    }

    private fun sessionInfoToMap(session: SessionInfo): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()
        map["sessionId"] = session.sessionId
        map["deviceId"] = session.deviceId
        map["startTime"] = session.startTime
        map["deviceMetadata"] = session.deviceMetadata
        map["userId"] = session.userId
        return map
    }
}
