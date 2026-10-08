package com.shilapi.xcertplay.media

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.AudioDeviceInfoBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25, 27, 28, 33], manifest = Config.NONE)
class AudioOutputDeviceCompatibilityTest {
    private val manager get() = RuntimeEnvironment.getApplication()
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Test fun savingUsesOnlyTheIdentityAvailableOnTheRunningSystem() {
        val device = device(7, "Navigation", "output:7")
        val saved = AudioOutputDevice.from(device)
        assertEquals(7, saved.id)
        assertEquals("Navigation", saved.name)
        assertEquals(if (Build.VERSION.SDK_INT >= 28) "output:7" else "", saved.address)
    }

    @Test fun resolvingAnAddressSavedOnANewerSystemKeepsTheMatchingNameOnLegacySystems() {
        val unrelated = device(7, "Media", "other")
        val target = device(8, "Navigation", "output:7")
        shadowOf(manager).setOutputDevices(listOf(unrelated, target))
        val saved = AudioOutputDevice(7, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "output:7", "Navigation")
        assertSame(target, saved.resolve(manager))
    }

    @Test fun inputPreferencesUseTheSameVersionSafeIdentity() {
        val input = device(9, "Microphone", "input:9", AudioDeviceInfo.TYPE_BUILTIN_MIC)
        shadowOf(manager).setInputDevices(listOf(input))
        assertSame(input, AudioOutputDevice.from(input).resolveInput(manager))
    }

    @Test
    @Config(sdk = [25, 27])
    fun legacySystemsDoNotChooseAnAmbiguousDeviceByEmptyAddress() {
        val first = device(8, "Navigation", "output:7")
        val second = device(9, "Navigation", "other")
        shadowOf(manager).setOutputDevices(listOf(first, second))
        val saved = AudioOutputDevice(7, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "output:7", "Navigation")
        assertNull(saved.resolve(manager))
    }

    @Test
    @Config(sdk = [28, 33])
    fun newerSystemsKeepAddressMatchingWhenAnIdIsReused() {
        val reusedId = device(7, "Navigation", "other")
        val target = device(8, "Navigation", "output:7")
        shadowOf(manager).setOutputDevices(listOf(reusedId, target))
        val saved = AudioOutputDevice(7, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "output:7", "Navigation")
        assertSame(target, saved.resolve(manager))
    }

    private fun device(id: Int, name: String, address: String,
        type: Int = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER): AudioDeviceInfo {
        val device = AudioDeviceInfoBuilder.newBuilder().setType(type).build()
        val port = ReflectionHelpers.getField<Any>(device, "mPort")
        ReflectionHelpers.setField(port, "mName", name)
        ReflectionHelpers.setField(port, "mAddress", address)
        val handle = ReflectionHelpers.getField<Any>(port, "mHandle")
        ReflectionHelpers.setField(handle, "mId", id)
        return device
    }
}
