package com.shilapi.xcertplay

import android.os.Looper
import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowInputDevice
import org.robolectric.shadows.ShadowInputEvent
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19], application = KitkatTestApplication::class)
class HsaeHomeKeyTest {
    private lateinit var service: HsaeHomeKeyService
    private var deviceName = "cyttsp6_btn"

    @Before fun setup() {
        service = Robolectric.buildService(HsaeHomeKeyService::class.java).create().get()
    }

    private fun key(action: Int, time: Long, code: Int = 251, repeat: Int = 0, flags: Int = 0) {
        val event = KeyEvent(1000, time, action, code, repeat, 0, 1, code, flags)
        Shadow.extract<ShadowInputEvent>(event).setDevice(ShadowInputDevice.makeInputDeviceNamed(deviceName))
        assertFalse(service.onKeyEvent(event))
    }

    private fun nextLaunch() = run {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        shadowOf(service).nextStartedActivity
    }

    @Test fun longHomeOpensCarPlayAfterOriginalHomeFinishes() {
        key(KeyEvent.ACTION_DOWN, 1000)
        key(KeyEvent.ACTION_UP, 1800)
        assertNull(shadowOf(service).nextStartedActivity)
        assertEquals(DiPlayActivity::class.java.name, nextLaunch()?.component?.className)
        assertNull(nextLaunch())
    }

    @Test fun shortHomeRemainsOriginalHome() {
        key(KeyEvent.ACTION_DOWN, 1000)
        key(KeyEvent.ACTION_UP, 1799)
        assertNull(nextLaunch())
    }

    @Test fun repeatsDoNotResetHoldOrLaunchTwice() {
        key(KeyEvent.ACTION_DOWN, 1000)
        key(KeyEvent.ACTION_DOWN, 1600, repeat = 1)
        key(KeyEvent.ACTION_UP, 1900)
        assertNotNull(nextLaunch())
        key(KeyEvent.ACTION_UP, 2100)
        assertNull(nextLaunch())
    }

    @Test fun canceledOrUnpairedReleaseDoesNotLaunch() {
        key(KeyEvent.ACTION_UP, 2000)
        assertNull(nextLaunch())
        key(KeyEvent.ACTION_DOWN, 3000)
        key(KeyEvent.ACTION_UP, 4000, flags = KeyEvent.FLAG_CANCELED)
        assertNull(nextLaunch())
    }

    @Test fun otherKeyOrDeviceDoesNotLaunch() {
        key(KeyEvent.ACTION_DOWN, 1000, code = KeyEvent.KEYCODE_HOME)
        key(KeyEvent.ACTION_UP, 2000, code = KeyEvent.KEYCODE_HOME)
        deviceName = "hsaeio-input"
        key(KeyEvent.ACTION_DOWN, 3000)
        key(KeyEvent.ACTION_UP, 4000)
        assertNull(nextLaunch())
    }

    @Test fun destroyedServiceCancelsPendingLaunch() {
        key(KeyEvent.ACTION_DOWN, 1000)
        key(KeyEvent.ACTION_UP, 2000)
        service.onDestroy()
        assertNull(nextLaunch())
    }
}
