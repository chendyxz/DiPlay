package com.shilapi.xcertplay.media

import android.media.MediaCodec
import android.os.Build
import java.nio.ByteBuffer

@Suppress("DEPRECATION")
internal fun MediaCodec.compatInputBuffer(index: Int): ByteBuffer? =
    if (Build.VERSION.SDK_INT >= 21) getInputBuffer(index) else inputBuffers[index]

@Suppress("DEPRECATION")
internal fun MediaCodec.compatOutputBuffer(index: Int): ByteBuffer? =
    if (Build.VERSION.SDK_INT >= 21) getOutputBuffer(index) else outputBuffers[index]
