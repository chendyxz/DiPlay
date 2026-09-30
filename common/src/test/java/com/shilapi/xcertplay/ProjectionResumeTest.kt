package com.shilapi.xcertplay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionResumeTest {
    @Test
    fun normalAppEntryRestoresRunningProjection() {
        assertTrue(
            shouldAutomaticallyOpenProjection(
                sessionRunning = true,
                setupReady = true,
                homePage = true,
                automaticEntry = true,
            ),
        )
    }

    @Test
    fun explicitHomeOrSettingsNavigationDoesNotBounceBackToProjection() {
        assertFalse(
            shouldAutomaticallyOpenProjection(
                sessionRunning = true,
                setupReady = true,
                homePage = true,
                automaticEntry = false,
            ),
        )
        assertFalse(
            shouldAutomaticallyOpenProjection(
                sessionRunning = true,
                setupReady = true,
                homePage = false,
                automaticEntry = true,
            ),
        )
    }
}
