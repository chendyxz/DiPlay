package com.shilapi.xcertplay.media

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyAudioStreamTest {
    @Test fun callsUseTheCallStream() {
        assertEquals(AudioManager.STREAM_VOICE_CALL, legacyAudioStream("telephony"))
    }
    @Test fun musicNavigationAndSiriUseTheMusicStream() {
        for (type in listOf("media", "navigation", "speechrecognition")) {
            assertEquals(AudioManager.STREAM_MUSIC, legacyAudioStream(type))
        }
    }
}
