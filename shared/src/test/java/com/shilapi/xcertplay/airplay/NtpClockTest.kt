package com.shilapi.xcertplay.airplay

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class NtpClockTest {
    @Test fun answersTimingRequestsOnTheIpv4ControlInterface() {
        val address = InetAddress.getByName("127.0.0.1")
        NtpClock().use { clock ->
            val port = clock.listen(address)
            DatagramSocket(0, address).use { phone ->
                phone.soTimeout = 2000
                val request = ByteArray(32).apply {
                    this[0] = 0x80.toByte(); this[1] = 210.toByte(); this[3] = 7
                    byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8).copyInto(this, 24)
                }
                phone.send(DatagramPacket(request, request.size, address, port))
                val response = DatagramPacket(ByteArray(64), 64)
                phone.receive(response)
                assertEquals(address, response.address)
                assertEquals(32, response.length)
                assertEquals(211, response.data[1].toInt() and 0xff)
                assertArrayEquals(request.copyOfRange(24, 32), response.data.copyOfRange(8, 16))
            }
        }
    }
}
