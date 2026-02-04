package com.infodreams.remotelogger.core

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeviceInfoProviderTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var deviceInfoProvider: DeviceInfoProvider

    @Before
    fun setUp() {
        context = mockk()
        contentResolver = mockk()
        every { context.contentResolver } returns contentResolver
        deviceInfoProvider = DeviceInfoProvider(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getDeviceId returns android id when available`() {
        mockkStatic(Settings.Secure::class)
        every { 
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) 
        } returns "test-android-id"

        val deviceId = deviceInfoProvider.getDeviceId()

        assertEquals("test-android-id", deviceId)
    }

    @Test
    fun `getDeviceId returns unknown_android when null`() {
        mockkStatic(Settings.Secure::class)
        every { 
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) 
        } returns null

        val deviceId = deviceInfoProvider.getDeviceId()

        assertEquals("unknown_android", deviceId)
    }
}
