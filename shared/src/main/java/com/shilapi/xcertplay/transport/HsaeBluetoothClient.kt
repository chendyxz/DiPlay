package com.shilapi.xcertplay.transport

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.util.Base64
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** NTDA3's AnwPhoneLink Binder ABI; its Bluetooth stack has no Android BluetoothManager. */
class HsaeBluetoothClient(context: Context) : BlockingDuplexByteStream {
    data class Device(val name: String, val address: String)

    private val context = context.applicationContext
    private val previousSocket = this.context.getSharedPreferences("hsae_rfcomm", Context.MODE_PRIVATE)
    private val ready = CountDownLatch(1)
    private val lock = Object()
    private val pending = ArrayDeque<ByteArray>()
    private var pendingBytes = 0
    @Volatile private var binder: IBinder? = null
    @Volatile private var closed = false
    @Volatile private var index = -1
    @Volatile private var failure: IOException? = null
    private var registered = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (!closed) binder = service
            ready.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            fail(IOException("HSAE Bluetooth service disconnected"))
        }
    }
    private val bound = this.context.bindService(Intent().setComponent(SERVICE), connection, Context.BIND_AUTO_CREATE)
    private val callback = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) { reply?.writeString(CALLBACK); return true }
            if (code != 1) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(CALLBACK)
            val receivedIndex = data.readInt()
            val bytes = data.createByteArray()
            val length = data.readInt()
            synchronized(lock) {
                if (!closed && receivedIndex == index) {
                    if (bytes == null || length !in 0..bytes.size || pendingBytes + length > 256 * 1024) {
                        failure = IOException("Invalid or overflowing HSAE RFCOMM data")
                    } else if (length > 0) {
                        pending.addLast(bytes.copyOf(length))
                        pendingBytes += length
                    }
                    lock.notifyAll()
                }
            }
            reply?.writeNoException()
            return true
        }
    }

    fun isEnabled(): Boolean = call(3) { it.readInt() == 1 }

    fun pairedDevices(): List<Device> = call(16, {
        it.writeInt(1); it.writeInt(16); it.writeInt(16); it.writeInt(16)
    }) { reply ->
        checkResult(reply.readInt(), "read paired phones")
        val count = reply.createIntArray()?.singleOrNull() ?: throw IOException("Invalid HSAE paired phone count")
        val names = reply.createStringArray() ?: throw IOException("Missing HSAE paired phone names")
        val addresses = reply.createStringArray() ?: throw IOException("Missing HSAE paired phone addresses")
        reply.createIntArray()
        if (count !in 0..minOf(names.size, addresses.size)) throw IOException("Invalid HSAE paired phone list")
        (0 until count).map { Device(names[it].orEmpty().ifBlank { "Paired device" }, addresses[it].orEmpty()) }
            .filter { ADDRESS.matches(it.address) }
    }

    fun localAddress(): String {
        val service = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
            .invoke(null, "com.hsae.auto.IBINDER_BT_PHONE_MANAGER") as? IBinder
            ?: throw IOException("HSAE Bluetooth address service is unavailable")
        return transact(service, "com.hsae.autosdk.bt.phone.IBTPhoneManager", 19, {}) {
            it.readString()?.takeIf(ADDRESS::matches) ?: throw IOException("Invalid HSAE Bluetooth address")
        }
    }

    fun connect(address: String, uuid: UUID) {
        if (!isEnabled()) throw IOException("Bluetooth is not enabled")
        checkResult(call(222) { it.readInt() }, "initialize RFCOMM")
        synchronized(lock) {
            checkOpen()
            call(231, { it.writeStrongBinder(callback) }) { Unit }
            registered = true
        }
        // Anw uses Windows GUID byte order for the first three UUID fields.
        val guid = ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
        guid.reverse(0, 4); guid.reverse(4, 6); guid.reverse(6, 8)
        recoverPreviousSocket(address, guid)
        val socketIndex = call(225, {
            it.writeInt(1); it.writeString(address); it.writeByteArray(guid)
            it.writeInt(0); it.writeLong(0); it.writeLong(0); it.writeInt(1)
        }) {
            checkResult(it.readInt(), "connect RFCOMM")
            it.createIntArray()?.singleOrNull()?.takeIf { value -> value in 0 until 30 }
                ?: throw IOException("Invalid HSAE RFCOMM socket index")
        }
        synchronized(lock) {
            index = socketIndex
            previousSocket.edit().putInt("index", socketIndex).putString("address", address)
                .putString("guid", Base64.encodeToString(guid, Base64.NO_WRAP)).commit()
        }
        if (closed) {
            disconnect(socketIndex)
            throw IOException("HSAE RFCOMM connection was cancelled")
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline) {
            if (socketConnected()) return
            synchronized(lock) {
                checkOpen()
                lock.wait(100)
            }
        }
        throw IOException("HSAE RFCOMM connect timed out")
    }

    override fun send(data: ByteArray) {
        synchronized(this) {
            var offset = 0
            while (offset < data.size) {
                checkOpen()
                val chunk = data.copyOfRange(offset, minOf(data.size, offset + 4096))
                val written = call(227, {
                    it.writeInt(index); it.writeByteArray(chunk); it.writeInt(chunk.size); it.writeInt(1)
                }) {
                    checkResult(it.readInt(), "write RFCOMM")
                    it.createIntArray()?.singleOrNull() ?: 0
                }
                if (written !in 1..chunk.size) throw IOException("HSAE RFCOMM write made no progress")
                offset += written
            }
        }
    }

    override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? {
        require(maxBytes > 0 && timeoutMillis >= 0)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            synchronized(lock) {
                failure?.let { throw it }
                if (closed) return ByteArray(0)
                if (pending.isNotEmpty()) {
                    val bytes = pending.removeFirst()
                    val size = minOf(bytes.size, maxBytes)
                    pendingBytes -= size
                    if (size < bytes.size) pending.addFirst(bytes.copyOfRange(size, bytes.size))
                    return bytes.copyOf(size)
                }
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) return null
                lock.wait(minOf(250, maxOf(1, TimeUnit.NANOSECONDS.toMillis(remaining))))
            }
            if (!closed && !socketConnected()) fail(IOException("HSAE RFCOMM peer disconnected"))
        }
    }

    override fun close() {
        val socketIndex = synchronized(lock) {
            if (closed) return
            closed = true
            pending.clear()
            lock.notifyAll()
            index
        }
        ready.countDown()
        try {
            if (socketIndex >= 0) {
                disconnect(socketIndex)
                // Anw disconnects asynchronously; retain ownership until the next recovery.
            }
        } finally {
            try {
                if (registered) binder?.let { service ->
                    transact(service, DESCRIPTOR, 232, { it.writeStrongBinder(callback) }) { Unit }
                }
            } finally { if (bound) context.unbindService(connection) }
        }
    }

    private fun disconnect(value: Int) {
        binder?.let { service -> transact(service, DESCRIPTOR, 226, { it.writeInt(value) }) { it.readInt() } }
    }
    private fun recoverPreviousSocket(address: String, guid: ByteArray) {
        val oldIndex = previousSocket.getInt("index", -1)
        val oldAddress = previousSocket.getString("address", null)
        val oldGuid = runCatching { Base64.decode(previousSocket.getString("guid", ""), Base64.NO_WRAP) }.getOrNull()
        if (oldIndex in 0 until 30 && socketMatches(oldIndex, oldAddress, oldGuid)) recoverSocket(oldIndex)
        // Reinstalling or changing the package/signature loses preferences, but not Anw's sockets.
        // Match both the selected phone and service UUID before closing an orphaned channel.
        for (value in 0 until 30) {
            if (value != oldIndex && socketMatches(value, address, guid)) recoverSocket(value)
        }
        previousSocket.edit().clear().commit()
    }
    private fun socketMatches(value: Int, expectedAddress: String?, expectedGuid: ByteArray?): Boolean =
        call(228, { it.writeInt(value); it.writeInt(1) }) {
            if (it.readInt() != 1 || it.readInt() != 1 || it.readInt() != 1) false else {
                val address = it.readString()
                val guid = it.createByteArray()
                address != null && address.equals(expectedAddress, ignoreCase = true) &&
                    expectedGuid != null && expectedGuid.size == 16 && guid != null && guid.contentEquals(expectedGuid)
            }
        }
    private fun recoverSocket(value: Int) {
        android.util.Log.i("xcertplay-usb", "wireless HSAE recovering previous CarPlay socket index=$value")
        disconnect(value)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (socketConnected(value)) {
            checkOpen()
            if (System.nanoTime() >= deadline) throw IOException("Previous HSAE RFCOMM socket is still connected")
            Thread.sleep(100)
        }
    }
    private fun socketConnected(value: Int = index): Boolean = call(23, { it.writeInt(17); it.writeInt(value) }) { it.readInt() == 1 }
    private fun checkOpen() {
        failure?.let { throw it }
        if (closed) throw IOException("HSAE RFCOMM stream is closed")
    }
    private fun fail(error: IOException) = synchronized(lock) { failure = error; lock.notifyAll() }
    private fun <T> call(code: Int, write: (Parcel) -> Unit = {}, read: (Parcel) -> T): T {
        if (!bound || !ready.await(5, TimeUnit.SECONDS)) throw IOException("HSAE Bluetooth service is unavailable")
        checkOpen()
        val service = binder ?: throw IOException("HSAE Bluetooth service is unavailable")
        return transact(service, DESCRIPTOR, code, write, read)
    }

    companion object {
        private val SERVICE = ComponentName("com.anwsdk.service", "com.anwsdk.service.AnwSdkService")
        private const val DESCRIPTOR = "com.anwsdk.service.IAnwPhoneLink"
        private const val CALLBACK = "com.anwsdk.service.IAnwSocketDataCallBack"
        private val ADDRESS = Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")

        fun isAvailable(context: Context): Boolean = Build.VERSION.SDK_INT < 21 &&
            Build.MANUFACTURER.equals("HSAE", ignoreCase = true) &&
            runCatching { context.packageManager.getServiceInfo(SERVICE, 0).enabled }.getOrDefault(false)

        private fun checkResult(result: Int, operation: String) {
            if (result != 1) throw IOException("HSAE Bluetooth could not $operation (result=$result)")
        }
        private fun <T> transact(service: IBinder, descriptor: String, code: Int,
            write: (Parcel) -> Unit, read: (Parcel) -> T): T {
            val request = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                request.writeInterfaceToken(descriptor)
                write(request)
                if (!service.transact(code, request, reply, 0)) throw IOException("HSAE Bluetooth API $code is unavailable")
                reply.readException()
                return read(reply)
            } catch (error: android.os.RemoteException) {
                throw IOException("HSAE Bluetooth service failed", error)
            } finally { request.recycle(); reply.recycle() }
        }
    }
}
