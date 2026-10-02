package com.shilapi.xcertplay.transport

/** KitKat flattens every configuration/alternate setting and drops their ids. */
internal object LegacyUsbDescriptors {
    data class Interface(val index: Int, val id: Int, val alternateSetting: Int,
        val interfaceClass: Int, val subclass: Int, val protocol: Int)
    data class Configuration(val id: Int, val interfaces: List<Interface>) {
        val usbMux: Interface? get() = interfaces.firstOrNull {
            it.interfaceClass == 0xff && it.subclass == 0xfe && it.protocol == 2
        }
        val control: Interface? get() = interfaces.firstOrNull {
            it.interfaceClass == 2 && it.subclass == 0x0d
        }
    }

    fun parse(raw: ByteArray): List<Configuration> {
        val configurations = mutableListOf<Configuration>()
        var interfaces = mutableListOf<Interface>()
        var configurationId: Int? = null
        var index = 0
        var offset = 0
        fun byte(at: Int) = raw[at].toInt() and 0xff
        while (offset + 2 <= raw.size) {
            val length = byte(offset)
            if (length < 2 || offset + length > raw.size) return emptyList()
            when (byte(offset + 1)) {
                2 -> {
                    if (length < 9) return emptyList()
                    configurationId?.let { configurations += Configuration(it, interfaces) }
                    configurationId = byte(offset + 5)
                    interfaces = mutableListOf()
                }
                4 -> {
                    if (length < 9 || configurationId == null) return emptyList()
                    interfaces += Interface(index++, byte(offset + 2), byte(offset + 3),
                        byte(offset + 5), byte(offset + 6), byte(offset + 7))
                }
            }
            offset += length
        }
        if (offset != raw.size) return emptyList()
        configurationId?.let { configurations += Configuration(it, interfaces) }
        return configurations
    }
}
