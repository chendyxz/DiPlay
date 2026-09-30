package com.shilapi.xcertplay.orchestration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WirelessHandoffTest {
    @Test
    fun activeTunnelKeepsHandoffAliveAfterBluetoothBootstrapCloses() {
        assertTrue(
            isWirelessHandoffInProgress(
                handoffRequested = false,
                tunnelActive = true,
                sessionActive = true,
            ),
        )
    }

    @Test
    fun ordinaryBootstrapLossStillFailsWithoutHandoffState() {
        assertFalse(
            isWirelessHandoffInProgress(
                handoffRequested = false,
                tunnelActive = false,
                sessionActive = false,
            ),
        )
    }

    @Test
    fun activeAirPlaySessionSurvivesMissingTunnelAtHandoffTimeout() {
        assertFalse(
            shouldFailWirelessHandoffTimeout(
                wirelessActive = false,
                sessionActive = true,
            ),
        )
    }

    @Test
    fun handoffTimeoutFailsWhenNeitherTunnelNorAirPlayIsActive() {
        assertTrue(
            shouldFailWirelessHandoffTimeout(
                wirelessActive = false,
                sessionActive = false,
            ),
        )
    }
}
