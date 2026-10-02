package com.shilapi.xcertplay

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Base64
import com.shilapi.xcertplay.transport.HsaeBluetoothClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19], application = HsaeTestApplication::class, qualifiers = "en")
class HsaeBluetoothTest {
    private val context get() = RuntimeEnvironment.getApplication() as HsaeTestApplication

    @Before fun vendorService() {
        ShadowBuild.setManufacturer("HSAE")
        shadowOf(context.packageManager).addOrUpdateService(ServiceInfo().apply {
            packageName = "com.anwsdk.service"
            name = "com.anwsdk.service.AnwSdkService"
            enabled = true
        })
    }

    @Test fun readsVendorPhonesWhenAndroidHasNoBluetoothService() {
        assertTrue(HsaeBluetoothClient.isAvailable(context))
        HsaeBluetoothClient(context).use {
            assertTrue(it.isEnabled())
            assertEquals(listOf(HsaeBluetoothClient.Device("iPhone", "00:11:22:33:44:55")), it.pairedDevices())
        }
        assertEquals(1, context.unbinds)
    }

    @Test fun connectsWithVendorGuidByteOrderAndKeepsPartialWritesOrdered() {
        HsaeBluetoothClient(context).use {
            it.connect("00:11:22:33:44:55", UUID.fromString("00000000-deca-fade-deca-deafdecacafe"))
            assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0xca.toByte(), 0xde.toByte(), 0xde.toByte(), 0xfa.toByte(),
                0xde.toByte(), 0xca.toByte(), 0xde.toByte(), 0xaf.toByte(), 0xde.toByte(), 0xca.toByte(), 0xca.toByte(), 0xfe.toByte()), context.vendor.guid)
            it.send(byteArrayOf(1, 2, 3, 4, 5))
            assertEquals(listOf<Byte>(1, 2, 3, 4, 5), context.vendor.written)
        }
        assertEquals(1, context.vendor.disconnects)
        assertEquals(1, context.vendor.unregisters)
        assertFalse(context.vendor.calls.contains(224)) // Never deinitialize another app's sockets.
    }

    @Test fun ignoresOtherSocketsAndPreservesReadRemainders() {
        HsaeBluetoothClient(context).use {
            it.connect("00:11:22:33:44:55", UUID.randomUUID())
            context.vendor.emit(8, byteArrayOf(9))
            context.vendor.emit(7, byteArrayOf(1, 2, 3, 4))
            assertArrayEquals(byteArrayOf(1, 2), it.recv(2, 0))
            assertArrayEquals(byteArrayOf(3, 4), it.recv(2, 0))
            assertNull(it.recv(2, 0))
        }
    }

    @Test fun malformedVendorDataFailsInsteadOfCorruptingIap2() {
        HsaeBluetoothClient(context).use { client ->
            client.connect("00:11:22:33:44:55", UUID.randomUUID())
            context.vendor.emit(7, byteArrayOf(1), 2)
            assertThrows(IOException::class.java) { client.recv(32, 0) }
        }
    }

    @Test fun failedConnectStillUnregistersAndUnbinds() {
        context.vendor.connectResult = -1
        HsaeBluetoothClient(context).use { client ->
            assertThrows(IOException::class.java) { client.connect("00:11:22:33:44:55", UUID.randomUUID()) }
        }
        assertEquals(1, context.vendor.unregisters)
        assertEquals(1, context.unbinds)
    }

    @Test fun reconnectsAfterThePreviousProcessLeftItsVendorSocketOpen() {
        rememberPreviousSocket()
        HsaeBluetoothClient(context).use { it.connect("00:11:22:33:44:55", UUID.randomUUID()) }
        assertEquals(listOf(6, 7), context.vendor.disconnectedIndices)
        assertTrue(context.vendor.calls.indexOf(226) < context.vendor.calls.indexOf(225))
        assertEquals(7, context.getSharedPreferences("hsae_rfcomm", 0).getInt("index", -1))
    }

    @Test fun recoversMatchingCarPlaySocketAfterAnUpgradeLostPreferences() {
        val uuid = UUID.fromString("00000000-deca-fade-deca-deafdecacafe")
        context.vendor.orphanedCarPlaySocket = true
        context.vendor.orphanDisconnectPolls = 2
        HsaeBluetoothClient(context).use { it.connect("00:11:22:33:44:55", uuid) }
        assertEquals(listOf(0, 7), context.vendor.disconnectedIndices)
        assertTrue(context.vendor.calls.indexOf(226) < context.vendor.calls.indexOf(225))
    }

    @Test fun doesNotRecoverAnOrphanBelongingToAnotherPhoneOrService() {
        context.vendor.orphanedCarPlaySocket = true
        HsaeBluetoothClient(context).use { it.connect("00:11:22:33:44:55", UUID.randomUUID()) }
        context.vendor.infoAddress = "11:22:33:44:55:66"
        HsaeBluetoothClient(context).use {
            it.connect("00:11:22:33:44:55", UUID.fromString("00000000-deca-fade-deca-deafdecacafe"))
        }
        assertEquals(listOf(7, 7), context.vendor.disconnectedIndices)
    }

    @Test fun preservesSocketRecordWhileVendorDisconnectIsAsynchronous() {
        HsaeBluetoothClient(context).use { it.connect("00:11:22:33:44:55", UUID.randomUUID()) }
        assertEquals(7, context.getSharedPreferences("hsae_rfcomm", 0).getInt("index", -1))
    }

    @Test fun doesNotCloseAReusedSocketWithAnotherPhoneOrService() {
        for (otherPhone in listOf(false, true)) {
            rememberPreviousSocket()
            context.vendor.infoAddress = if (otherPhone) "11:22:33:44:55:66" else "00:11:22:33:44:55"
            context.vendor.infoGuid = if (otherPhone) ByteArray(16) { 1 } else ByteArray(16) { 2 }
            HsaeBluetoothClient(context).use { it.connect("00:11:22:33:44:55", UUID.randomUUID()) }
        }
        assertEquals(listOf(7, 7), context.vendor.disconnectedIndices)
    }

    private fun rememberPreviousSocket() {
        context.getSharedPreferences("hsae_rfcomm", 0).edit().putInt("index", 6)
            .putString("address", "00:11:22:33:44:55")
            .putString("guid", Base64.encodeToString(ByteArray(16) { 1 }, Base64.NO_WRAP)).commit()
    }

    @Test fun bluetoothHelpOpensTheCarSettingsInsteadOfTheBlankAndroidPage() {
        val intent = Intent(context, DiPlayActivity::class.java).putExtra("page", "settings")
        Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup().use {
            fun find(view: android.view.View): android.widget.Button? {
                if (view is android.widget.Button && view.text.toString() == "Bluetooth settings") return view
                if (view is android.view.ViewGroup) for (index in 0 until view.childCount) {
                    find(view.getChildAt(index))?.let { return it }
                }
                return null
            }
            assertTrue(checkNotNull(find(it.get().window.decorView)).performClick())
            val opened = shadowOf(it.get()).nextStartedActivity
            assertEquals(ComponentName("com.hsae.d531mc.systemsetting",
                "com.hsae.d531mc.systemsetting.connect.activity.BlueActivity"), opened.component)
        }
    }
}

class HsaeTestApplication : Application() {
    val vendor = FakeAnwService()
    var unbinds = 0
    override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int): Boolean {
        if (intent.component?.packageName != "com.anwsdk.service") return false
        connection.onServiceConnected(checkNotNull(intent.component), vendor)
        return true
    }
    override fun unbindService(connection: ServiceConnection) { unbinds++ }
}

class FakeAnwService : Binder() {
    val calls = mutableListOf<Int>()
    val written = mutableListOf<Byte>()
    var guid: ByteArray? = null
    var connectResult = 1
    var disconnects = 0
    var unregisters = 0
    val disconnectedIndices = mutableListOf<Int>()
    var infoAddress = "00:11:22:33:44:55"
    var infoGuid = ByteArray(16) { 1 }
    var orphanedCarPlaySocket = false
    var orphanDisconnectPolls = 0
    private var orphanDisconnectRequested = false
    private var previousConnected = true
    private var callback: IBinder? = null

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        data.enforceInterface("com.anwsdk.service.IAnwPhoneLink")
        val output = checkNotNull(reply)
        calls += code
        output.writeNoException()
        when (code) {
            3, 222 -> output.writeInt(1)
            16 -> {
                assertEquals(listOf(1, 16, 16, 16), List(4) { data.readInt() })
                output.writeInt(1)
                output.writeIntArray(intArrayOf(1))
                output.writeStringArray(Array(16) { if (it == 0) "iPhone" else "" })
                output.writeStringArray(Array(16) { if (it == 0) "00:11:22:33:44:55" else "" })
                output.writeIntArray(IntArray(16))
            }
            231 -> callback = data.readStrongBinder()
            232 -> { assertEquals(callback, data.readStrongBinder()); callback = null; unregisters++ }
            225 -> {
                if (orphanDisconnectRequested) assertFalse(orphanedCarPlaySocket)
                assertEquals(1, data.readInt())
                assertEquals("00:11:22:33:44:55", data.readString())
                guid = data.createByteArray()
                assertEquals(0, data.readInt())
                assertEquals(0L, data.readLong()); assertEquals(0L, data.readLong())
                assertEquals(1, data.readInt())
                output.writeInt(connectResult); output.writeIntArray(intArrayOf(7))
            }
            23 -> {
                assertEquals(17, data.readInt())
                val index = data.readInt()
                assertTrue(index in 0 until 30)
                if (index == 0 && orphanDisconnectRequested && orphanDisconnectPolls-- <= 0) {
                    orphanedCarPlaySocket = false
                }
                output.writeInt(if (index == 7 || index == 6 && previousConnected ||
                    index == 0 && orphanedCarPlaySocket) 1 else 0)
            }
            227 -> {
                assertEquals(7, data.readInt())
                val bytes = checkNotNull(data.createByteArray())
                assertEquals(bytes.size, data.readInt()); assertEquals(1, data.readInt())
                val count = minOf(2, bytes.size)
                written += bytes.take(count)
                output.writeInt(1); output.writeIntArray(intArrayOf(count))
            }
            226 -> {
                val index = data.readInt()
                assertTrue(index == 0 || index == 6 || index == 7)
                disconnectedIndices += index
                if (index == 6) previousConnected = false
                if (index == 0) orphanDisconnectRequested = true
                output.writeInt(1); disconnects++
            }
            228 -> {
                val index = data.readInt(); assertEquals(1, data.readInt())
                if (index != 6 && !(index == 0 && orphanedCarPlaySocket)) {
                    output.writeInt(-1); output.writeInt(1); output.writeInt(0)
                    return true
                }
                output.writeInt(1); output.writeInt(1); output.writeInt(1)
                output.writeString(infoAddress)
                output.writeByteArray(if (index == 0) byteArrayOf(0, 0, 0, 0, 0xca.toByte(), 0xde.toByte(),
                    0xde.toByte(), 0xfa.toByte(), 0xde.toByte(), 0xca.toByte(), 0xde.toByte(), 0xaf.toByte(),
                    0xde.toByte(), 0xca.toByte(), 0xca.toByte(), 0xfe.toByte()) else infoGuid)
                output.writeInt(0); output.writeInt(if (previousConnected) 1 else 0)
            }
            else -> return false
        }
        return true
    }

    fun emit(index: Int, bytes: ByteArray, length: Int = bytes.size) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.anwsdk.service.IAnwSocketDataCallBack")
            data.writeInt(index); data.writeByteArray(bytes); data.writeInt(length)
            checkNotNull(callback).transact(1, data, reply, 0)
            reply.readException()
        } finally { data.recycle(); reply.recycle() }
    }
}
