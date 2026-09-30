package com.shilapi.xcertplay

import android.os.Looper
import com.shilapi.xcertplay.airplay.AirPlayConfig
import com.shilapi.xcertplay.airplay.AirPlayDisplayConfig
import com.shilapi.xcertplay.airplay.AirPlayIdentity
import com.shilapi.xcertplay.airplay.AirPlayMediaHandler
import com.shilapi.xcertplay.airplay.AirPlaySession
import com.shilapi.xcertplay.airplay.AirPlaySessionListener
import com.shilapi.xcertplay.airplay.PairingStore
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.orchestration.CarPlayRuntimeConfig
import com.shilapi.xcertplay.orchestration.CarPlayStatus
import com.shilapi.xcertplay.orchestration.CarPlayTransport
import com.shilapi.xcertplay.orchestration.MfiTarget
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.net.Socket
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class WirelessStartupTimeoutTest {
    private val statuses = mutableListOf<CarPlayStatus>()
    private val identity = AirPlayIdentity.generate()
    private val config = AirPlayConfig("Test", "00:11:22:33:44:55", "00:11:22:33:44:55",
        sourceVersion = "950.7.1", main = AirPlayDisplayConfig(1920, 1080))
    private val media = object : AirPlayMediaHandler {}
    private lateinit var controller: CarPlayController
    private lateinit var listener: AirPlaySessionListener

    @Before fun setup() {
        shadowOf(RuntimeEnvironment.getApplication()).declareActionUnbindable(
            "com.ts.car.someip.SomeIpServerService",
        )
        controller = CarPlayController(
            context = RuntimeEnvironment.getApplication(),
            config = CarPlayRuntimeConfig(
                identification = Iap2IdentificationConfig("Test", "Test", "Test", "1", "1", "1", 0),
                mfiTarget = MfiTarget.LOCAL,
                transport = CarPlayTransport.WIRELESS,
            ),
            airPlayConfig = config,
            identity = identity,
            pairings = PairingStore(),
            listener = object : AirPlaySessionListener {},
            media = media,
            reportStatus = statuses::add,
        )
        val phase = controller.javaClass.getDeclaredField("phase").apply { isAccessible = true }
        phase.set(controller, phase.type.enumConstants!!.single { it.toString() == "WIRELESS" })
        listener = ReflectionHelpers.callInstanceMethod(
            controller, "wirelessSessionListener", ClassParameter.from(Int::class.javaPrimitiveType, 0),
        )
    }

    @After fun cleanup() {
        controller.close()
        assertTrue(controller.awaitClosed(5000))
    }

    @Test fun authenticatedBluetoothWithoutAirPlayFailsInsteadOfWaitingForever() {
        arm()
        idle(44)
        assertTrue(statuses.isEmpty())
        idle(1)
        assertTrue(statuses.single() is CarPlayStatus.Failed)
    }

    @Test fun anActiveSessionWithoutVideoStillTimesOut() {
        arm()
        listener.onSessionActive(session())
        idle(45)
        assertTrue(statuses.single() is CarPlayStatus.Failed)
    }

    @Test fun renderedVideoKeepsAHealthySessionAliveWithoutATunnel() {
        arm()
        val session = session()
        listener.onSessionActive(session)
        listener.onVideoFrameRendered(session)
        idle(3600)
        assertTrue(statuses.isEmpty())
    }

    @Test fun framesFromAnOldSessionCannotCancelTheCurrentStartupTimeout() {
        arm()
        val old = session()
        listener.onSessionActive(old)
        listener.onSessionActive(session())
        listener.onVideoFrameRendered(old)
        idle(45)
        assertTrue(statuses.single() is CarPlayStatus.Failed)
    }

    @Test fun aPreviousGenerationCannotFailANewConnection() {
        arm()
        ReflectionHelpers.getField<AtomicInteger>(controller, "wirelessGeneration").incrementAndGet()
        idle(45)
        assertTrue(statuses.isEmpty())
    }

    private fun arm() = ReflectionHelpers.callInstanceMethod<Unit>(
        controller, "armWirelessStartupWatchdog", ClassParameter.from(Int::class.javaPrimitiveType, 0),
    )

    private fun idle(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))

    private fun session() = AirPlaySession(Socket(), config, identity, PairingStore(), null, listener, media)
}
