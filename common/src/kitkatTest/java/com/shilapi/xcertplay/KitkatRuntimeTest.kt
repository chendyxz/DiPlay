package com.shilapi.xcertplay

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.orchestration.*
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19], application = KitkatTestApplication::class)
class KitkatRuntimeTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun homeStartsWithKitkatResourcesAndNoIdentity() {
        assertEquals(19, Build.VERSION.SDK_INT)
        Robolectric.buildActivity(DiPlayActivity::class.java).setup().use {
            assertFalse(it.get().isFinishing)
        }
    }

    @Test fun settingsPagesRenderWithoutLollipopWidgets() {
        for (page in listOf("settings", "connection", "about")) {
            val intent = Intent(context, DiPlayActivity::class.java).putExtra("page", page)
            Robolectric.buildActivity(DiPlayActivity::class.java, intent).setup().use {
                assertFalse(it.get().isFinishing)
            }
        }
    }

    @Test fun wiredControllerStartsAndClosesWithoutMarshmallowServiceApis() {
        val statuses = mutableListOf<CarPlayStatus>()
        val controller = CarPlayController(
            context = context,
            config = CarPlayRuntimeConfig(
                identification = Iap2IdentificationConfig("Test", "Test", "Test", "1", "1", "1", 3),
                mfiTarget = MfiTarget.LOCAL,
            ),
            airPlayConfig = AirPlayConfig("Test", "00:11:22:33:44:55", "00:11:22:33:44:55",
                sourceVersion = "950.7.1", main = AirPlayDisplayConfig(800, 480)),
            identity = AirPlayIdentity.generate(),
            pairings = PairingStore(),
            listener = object : AirPlaySessionListener {},
            media = object : AirPlayMediaHandler {},
            reportStatus = statuses::add,
        )
        try {
            controller.start()
            shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(statuses.contains(CarPlayStatus.DiscoveringMfi))
        } finally {
            controller.close()
            assertTrue(controller.awaitClosed(5000))
        }
    }

    @Test fun foregroundConnectionUsesLegacyNotification() {
        val service = Robolectric.buildService(DiPlaySessionService::class.java).create()
        try {
            service.get().onStartCommand(Intent(context, DiPlaySessionService::class.java), 0, 1)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            assertEquals(1, shadowOf(manager).allNotifications.size)
        } finally { service.destroy() }
    }

    @Test fun legacyWirelessModeAndPrivateStorageAreAvailable() {
        AirPlayPersistence.saveWirelessHotspotMode(context, WirelessHotspotMode.WIFI_P2P)
        assertEquals(WirelessHotspotMode.MANUAL, AirPlayPersistence.loadWirelessHotspotMode(context))
        val directory = androidx.core.content.ContextCompat.getNoBackupFilesDir(context)
        assertTrue(requireNotNull(directory).isDirectory)
        assertFalse(DiLink51ClusterMonitor.hasAccess(context))
    }
}

/** The API 19 test device has no vendor vehicle services. */
class KitkatTestApplication : android.app.Application() {
    override fun bindService(intent: Intent, connection: android.content.ServiceConnection, flags: Int): Boolean = false
}
