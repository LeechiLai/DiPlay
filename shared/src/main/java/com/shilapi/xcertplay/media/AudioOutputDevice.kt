package com.shilapi.xcertplay.media

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import org.json.JSONObject

/** A saved output preference. Device IDs alone may change when the audio service restarts. */
data class AudioOutputDevice(val id: Int, val type: Int, val address: String, val name: String) {
    fun resolve(manager: AudioManager?): AudioDeviceInfo? {
        return resolveFrom(outputs(manager))
    }
    // carlito | Input and output preferences share the same stable device identity representation.
    fun resolveInput(manager: AudioManager?): AudioDeviceInfo? = resolveFrom(inputs(manager))
    private fun resolveFrom(devices: List<AudioDeviceInfo>): AudioDeviceInfo? =
        if (Build.VERSION.SDK_INT >= 28 && address.isNotBlank()) devices.firstOrNull { it.id == id && it.type == type && it.routingAddress() == address && it.productName.toString() == name }
            ?: devices.filter { it.type == type && it.routingAddress() == address }.singleOrNull()
        else devices.firstOrNull { it.id == id && it.type == type && it.productName.toString() == name }
            ?: devices.filter { it.type == type && it.productName.toString() == name }.singleOrNull()

    fun encode(): String = JSONObject().put("id", id).put("type", type)
        .put("address", address).put("name", name).toString()

    companion object {
        fun outputs(manager: AudioManager?): List<AudioDeviceInfo> =
            runCatching { manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList().orEmpty() }
                .getOrDefault(emptyList())
        fun inputs(manager: AudioManager?): List<AudioDeviceInfo> =
            runCatching { manager?.getDevices(AudioManager.GET_DEVICES_INPUTS)?.toList().orEmpty() }.getOrDefault(emptyList())

        fun from(device: AudioDeviceInfo) = AudioOutputDevice(
            device.id, device.type, device.routingAddress(), device.productName.toString(),
        )

        fun decode(value: String?): AudioOutputDevice? = runCatching {
            if (value == null) null else JSONObject(value).let {
                AudioOutputDevice(it.getInt("id"), it.getInt("type"), it.getString("address"), it.getString("name"))
            }
        }.getOrNull()
    }
}

// carlito | Android 7/8 expose no address; retain the existing ID/type/name identity there.
private fun AudioDeviceInfo.routingAddress(): String = if (Build.VERSION.SDK_INT >= 28) address else ""
