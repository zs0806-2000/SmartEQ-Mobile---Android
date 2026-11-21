package com.smarteq.pro.audio.engine

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smarteq.pro.data.model.AudioOutputDevice
import com.smarteq.pro.data.model.AudioOutputDeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Monitors the active audio output device (Bluetooth A2DP/LE, USB DAC,
 * Wired Headset, or Built-in Speakers). "Low-latency" only switches the effect to
 * Android's faster, lower-frequency-resolution variant; it cannot change the delay
 * that Bluetooth itself adds, so no latency figure is claimed.
 */
class AudioRoutingManager(
    private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val _currentOutputDevice = MutableStateFlow(AudioOutputDevice())
    val currentOutputDevice: StateFlow<AudioOutputDevice> = _currentOutputDevice.asStateFlow()

    private val _lowLatencyModeEnabled = MutableStateFlow(false)
    val lowLatencyModeEnabled: StateFlow<Boolean> = _lowLatencyModeEnabled.asStateFlow()

    private var onRoutingChangedListener: ((AudioOutputDevice, Boolean) -> Unit)? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }
    }

    init {
        try {
            audioManager?.registerAudioDeviceCallback(deviceCallback, handler)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register AudioDeviceCallback", e)
        }
        updateActiveDevice()
    }

    fun setOnRoutingChangedListener(listener: (AudioOutputDevice, Boolean) -> Unit) {
        onRoutingChangedListener = listener
        listener(_currentOutputDevice.value, _lowLatencyModeEnabled.value)
    }

    fun setLowLatencyMode(enabled: Boolean) {
        _lowLatencyModeEnabled.value = enabled
        updateActiveDevice()
    }

    fun updateActiveDevice() {
        val am = audioManager ?: return
        try {
            val outputDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val selected = pickPrimaryOutputDevice(outputDevices)
            val isLowLatency = _lowLatencyModeEnabled.value && (selected.isBluetooth || selected.type == AudioOutputDeviceType.USB_AUDIO)

            val updated = selected.copy(
                isLowLatencyActive = isLowLatency,
            )
            _currentOutputDevice.update { updated }
            onRoutingChangedListener?.invoke(updated, isLowLatency)
            Log.d(TAG, "Active audio output: ${updated.name} (type: ${updated.type}, lowLatency: $isLowLatency)")
        } catch (e: Exception) {
            Log.w(TAG, "Error querying audio devices", e)
        }
    }

    private fun pickPrimaryOutputDevice(devices: Array<AudioDeviceInfo>): AudioOutputDevice {
        // Priority: Bluetooth LE / A2DP -> USB -> Wired -> Speaker
        for (device in devices) {
            val type = device.type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER || type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
            ) {
                val name = resolveDeviceName(device, "Bluetooth LE Audio")
                return AudioOutputDevice(
                    id = device.id,
                    name = name,
                    type = AudioOutputDeviceType.BLUETOOTH_LE,
                    isBluetooth = true,
                )
            }
        }

        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                val name = resolveDeviceName(device, "Bluetooth Audio")
                return AudioOutputDevice(
                    id = device.id,
                    name = name,
                    type = AudioOutputDeviceType.BLUETOOTH_A2DP,
                    isBluetooth = true,
                )
            }
        }

        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            ) {
                val name = resolveDeviceName(device, "USB Audio Device")
                return AudioOutputDevice(
                    id = device.id,
                    name = name,
                    type = AudioOutputDeviceType.USB_AUDIO,
                    isBluetooth = false,
                )
            }
        }

        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            ) {
                val name = resolveDeviceName(device, "Wired Headphones")
                return AudioOutputDevice(
                    id = device.id,
                    name = name,
                    type = AudioOutputDeviceType.WIRED_HEADPHONES,
                    isBluetooth = false,
                )
            }
        }

        return AudioOutputDevice(
            id = 0,
            name = "Built-in Speaker",
            type = AudioOutputDeviceType.BUILTIN_SPEAKER,
            isBluetooth = false,
        )
    }

    private fun resolveDeviceName(device: AudioDeviceInfo, fallback: String): String {
        val prodName = device.productName?.toString()?.trim()
        return if (!prodName.isNullOrEmpty() && prodName != "null") prodName else fallback
    }

    fun release() {
        try {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering device callback", e)
        }
    }

    companion object {
        private const val TAG = "AudioRoutingManager"
    }
}
