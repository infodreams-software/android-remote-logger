package com.infodreams.remotelogger.core

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DeviceInfoProviderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var deviceInfoProvider: DeviceInfoProvider
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        context = mockk()
        filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        deviceInfoProvider = DeviceInfoProvider(context)
    }

    @Test
    fun `getDeviceId creates new ID if file missing`() {
        val deviceId = deviceInfoProvider.getDeviceId()
        assertTrue(deviceId.isNotEmpty())
        
        // Verify file created
        val file = File(filesDir, "remote_logger_device_id")
        assertTrue(file.exists())
        assertEquals(deviceId, file.readText())
    }

    @Test
    fun `getDeviceId reads existing ID from file`() {
        val file = File(filesDir, "remote_logger_device_id")
        file.writeText("existing-uuid-123")

        val deviceId = deviceInfoProvider.getDeviceId()

        assertEquals("existing-uuid-123", deviceId)
    }
}
