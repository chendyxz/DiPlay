package com.shilapi.xcertplay

import com.shilapi.xcertplay.network.KitkatMdnsSocket
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19])
class KitkatMdnsSocketTest {
    @Test fun sharesThePortWithAnExistingMdnsListener() {
        DatagramSocket(null as SocketAddress?).use { systemSocket ->
            systemSocket.reuseAddress = true
            systemSocket.bind(InetSocketAddress(0))
            KitkatMdnsSocket(InetSocketAddress(systemSocket.localPort)).use {
                assertTrue(it.reuseAddress)
                assertTrue(it.isBound)
            }
        }
    }
}
