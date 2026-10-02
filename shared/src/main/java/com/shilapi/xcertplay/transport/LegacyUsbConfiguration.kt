package com.shilapi.xcertplay.transport

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface

internal class LegacyUsbConfiguration private constructor(
    val descriptor: LegacyUsbDescriptors.Configuration,
    private val device: UsbDevice,
) {
    fun usbInterface(value: LegacyUsbDescriptors.Interface): UsbInterface = device.getInterface(value.index)

    companion object {
        fun find(connection: UsbDeviceConnection, device: UsbDevice): LegacyUsbConfiguration? {
            val configuration = LegacyUsbDescriptors.parse(connection.rawDescriptors).firstOrNull {
                it.usbMux != null && it.control != null &&
                    it.interfaces.all { value -> value.index < device.interfaceCount }
            } ?: return null
            return LegacyUsbConfiguration(configuration, device)
        }
    }
}
