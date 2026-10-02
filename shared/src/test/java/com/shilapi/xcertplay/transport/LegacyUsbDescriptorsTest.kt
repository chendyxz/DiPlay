package com.shilapi.xcertplay.transport

import org.junit.Assert.*
import org.junit.Test

class LegacyUsbDescriptorsTest {
    private fun config(id: Int) = byteArrayOf(9, 2, 0, 0, 3, id.toByte(), 0, 0, 0)
    private fun iface(id: Int, alt: Int, cls: Int, sub: Int, protocol: Int = 0) =
        byteArrayOf(9, 4, id.toByte(), alt.toByte(), 0, cls.toByte(), sub.toByte(), protocol.toByte(), 0)

    @Test fun preservesConfigurationAndFlattenedInterfaceIndices() {
        val raw = config(1) + iface(0, 0, 6, 1) +
            config(6) + iface(1, 0, 0xff, 0xfe, 2) + iface(2, 0, 2, 0x0d) +
            iface(3, 0, 0x0a, 0) + iface(3, 1, 0x0a, 0)
        val configurations = LegacyUsbDescriptors.parse(raw)
        assertEquals(listOf(1, 6), configurations.map { it.id })
        assertNull(configurations[0].usbMux)
        val carPlay = configurations[1]
        assertEquals(1, carPlay.usbMux!!.index)
        assertEquals(2, carPlay.control!!.index)
        assertEquals(4, carPlay.interfaces.last().index)
        assertEquals(1, carPlay.interfaces.last().alternateSetting)
    }

    @Test fun doesNotCombineInterfacesFromDifferentConfigurations() {
        val configurations = LegacyUsbDescriptors.parse(config(1) + iface(1, 0, 0xff, 0xfe, 2) +
            config(2) + iface(2, 0, 2, 0x0d))
        assertFalse(configurations.any { it.usbMux != null && it.control != null })
    }

    @Test fun rejectsMalformedOrTruncatedDescriptors() {
        val valid = config(6) + iface(1, 0, 0xff, 0xfe, 2)
        for (raw in listOf(valid + byteArrayOf(0, 4), valid + byteArrayOf(9, 4, 1),
            valid + byteArrayOf(9), iface(1, 0, 0xff, 0xfe, 2))) {
            assertTrue(LegacyUsbDescriptors.parse(raw).isEmpty())
        }
    }
}
