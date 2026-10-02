package com.shilapi.xcertplay.network

import java.net.MulticastSocket
import java.net.SocketAddress

/** KitKat binds MulticastSocket before setting SO_REUSEADDR; mDNS must share port 5353. */
class KitkatMdnsSocket(address: SocketAddress?) : MulticastSocket(null as SocketAddress?) {
    init {
        try {
            reuseAddress = true
            bind(address)
        } catch (error: Exception) {
            close()
            throw error
        }
    }
}
