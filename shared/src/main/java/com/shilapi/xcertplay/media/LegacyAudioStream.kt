package com.shilapi.xcertplay.media

import android.media.AudioManager

internal fun legacyAudioStream(audioType: String?): Int =
    if (audioType == "telephony") AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC
