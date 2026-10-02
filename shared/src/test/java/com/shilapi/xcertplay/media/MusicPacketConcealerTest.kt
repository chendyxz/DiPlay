package com.shilapi.xcertplay.media

import org.junit.Assert.*
import org.junit.Test

class MusicPacketConcealerTest {
    private fun pcm(frames: Int, vararg channels: Int): ByteArray = ByteArray(frames * channels.size * 2).also {
        for (frame in 0 until frames) for (channel in channels.indices) {
            val offset = (frame * channels.size + channel) * 2
            it[offset] = channels[channel].toByte()
            it[offset + 1] = (channels[channel] shr 8).toByte()
        }
    }
    private fun sample(data: ByteArray, frame: Int, channel: Int, channels: Int): Int {
        val offset = (frame * channels + channel) * 2
        return (data[offset].toInt() and 0xff) or (data[offset + 1].toInt() shl 8)
    }
    private fun time(sample: Long, rate: Int = 48000): Long = (sample and 0xffff_ffffL) * 1_000_000L / rate

    @Test fun uninterruptedMusicRemainsByteExactAtBothSampleRates() {
        for (rate in listOf(44100, 48000)) {
            val plc = MusicPacketConcealer(rate, 2)
            val original = pcm(1024, 12345, -23456)
            var frames = 0
            repeat(100) { index ->
                val data = original.copyOf()
                plc.render(data, data.size, time(index * 1024L, rate)) { bytes, size ->
                    assertSame(data, bytes)
                    assertEquals(data.size, size)
                    assertArrayEquals(original, bytes)
                    frames += size / 4
                }
            }
            assertEquals(102400, frames)
            assertEquals(0L, plc.concealedFrames)
        }
    }

    @Test fun oneLostAacPacketKeepsDurationAndSmoothsBothStereoChannels() {
        val plc = MusicPacketConcealer(48000, 2)
        val output = mutableListOf<ByteArray>()
        val write: (ByteArray, Int) -> Unit = { bytes, size -> output += bytes.copyOf(size) }
        val first = pcm(1024, 12000, -24000)
        val next = pcm(1024, -10000, 20000)
        plc.render(first, first.size, time(0), write)
        plc.render(next, next.size, time(2048), write)
        assertEquals(3, output.size)
        assertEquals(3072, output.sumOf { it.size / 4 })
        assertEquals(1024L, plc.concealedFrames)
        assertEquals(1, plc.concealments)
        assertTrue(sample(output[1], 0, 0, 2) in 11000..12000)
        assertTrue(sample(output[1], 0, 1, 2) in -24000..-22000)
        assertEquals(0, sample(output[1], 239, 0, 2))
        assertEquals(0, sample(output[1], 1023, 1, 2))
        assertTrue(sample(output[2], 0, 0, 2) in -100..0)
        assertTrue(sample(output[2], 0, 1, 2) in 0..100)
        assertEquals(-10000, sample(output[2], 239, 0, 2))
        assertEquals(20000, sample(output[2], 239, 1, 2))
    }

    @Test fun repeatedLossDoesNotGraduallyConsumeTheMusicBuffer() {
        val plc = MusicPacketConcealer(48000, 2)
        var playedFrames = 0
        for (index in 0..100) {
            if (index % 5 == 4) continue
            val data = pcm(1024, 1000, -1000)
            plc.render(data, data.size, time(index * 1024L)) { _, size -> playedFrames += size / 4 }
        }
        assertEquals(101 * 1024, playedFrames)
        assertEquals(20 * 1024L, plc.concealedFrames)
        assertEquals(20, plc.concealments)
    }

    @Test fun lossAcrossUnsignedRtpTimestampWrapIsStillDetected() {
        val plc = MusicPacketConcealer(48000, 1)
        var frames = 0
        val data = pcm(1024, 1000)
        plc.render(data, data.size, time(0xffff_fc00L)) { _, size -> frames += size / 2 }
        plc.render(data, data.size, time(1024)) { _, size -> frames += size / 2 }
        assertEquals(3072, frames)
        assertEquals(1024L, plc.concealedFrames)
    }

    @Test fun lateOrDuplicateDecodedPacketsDoNotReplayMusicOrChangeTheTimeline() {
        val plc = MusicPacketConcealer(48000, 2)
        var frames = 0
        for (start in listOf(0L, 1024L, 1024L, 0L, 2048L)) {
            val data = pcm(1024, 1000, -1000)
            plc.render(data, data.size, time(start)) { _, size -> frames += size / 4 }
        }
        assertEquals(3072, frames)
        assertEquals(0L, plc.concealedFrames)
        assertEquals(2, plc.lateBuffers)
    }

    @Test fun longOutagesAndClockResetsDoNotGenerateUnboundedSyntheticAudio() {
        val plc = MusicPacketConcealer(48000, 2)
        var frames = 0
        for (start in listOf(0L, 48000L * 60, 0L, 1024L)) {
            val data = pcm(1024, 1000, -1000)
            plc.render(data, data.size, time(start)) { _, size -> frames += size / 4 }
        }
        assertEquals(4096, frames)
        assertEquals(0L, plc.concealedFrames)
    }

    @Test fun concealmentIsBoundedToTwoHundredMilliseconds() {
        for (gap in listOf(9600, 9601)) {
            val plc = MusicPacketConcealer(48000, 2)
            var frames = 0
            val data = pcm(128, Short.MAX_VALUE.toInt(), Short.MIN_VALUE.toInt())
            plc.render(data, data.size, time(0)) { _, size -> frames += size / 4 }
            plc.render(data, data.size, time(128L + gap)) { _, size -> frames += size / 4 }
            assertEquals(256 + if (gap == 9600) gap else 0, frames)
            assertEquals(if (gap == 9600) gap.toLong() else 0L, plc.concealedFrames)
        }
    }

    @Test fun reusedDecoderStorageDoesNotPlayStaleBytesOrAdvanceOnEmptyOutput() {
        val plc = MusicPacketConcealer(48000, 2)
        val data = pcm(4096, 1000, -1000)
        var frames = 0
        plc.render(data, 0, time(0)) { _, _ -> fail("Empty codec output must not be played") }
        plc.render(data, 1024 * 4, time(0)) { _, size -> frames += size / 4 }
        plc.render(data, 1024 * 4, time(2048)) { _, size -> frames += size / 4 }
        assertEquals(3072, frames)
        assertEquals(1024L, plc.concealedFrames)
    }
}
