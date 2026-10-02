package com.shilapi.xcertplay.media

/** Keeps decoded music on the RTP sample timeline when short AAC packets are lost. */
internal class MusicPacketConcealer(private val sampleRate: Int, private val channels: Int) {
    private val frameBytes = channels * 2
    private val maxGapFrames = sampleRate / 5 // Only conceal gaps up to 200 ms.
    private val fadeFrames = maxOf(1, sampleRate / 200)
    private val tail = IntArray(channels)
    private var nextSample: Int? = null
    var concealedFrames = 0L
        private set
    var concealments = 0
        private set
    var lateBuffers = 0
        private set

    init { require(sampleRate > 0 && channels in 1..2) }

    fun render(data: ByteArray, length: Int, presentationTimeUs: Long, write: (ByteArray, Int) -> Unit) {
        require(length in 0..data.size && length % frameBytes == 0)
        if (length == 0) return
        // Recover the RTP sample from MediaCodec's PTS, rounding away microsecond truncation.
        val sample = ((presentationTimeUs * sampleRate + 500_000L) / 1_000_000L).toInt()
        val frames = length / frameBytes
        val gap = nextSample?.let { sample - it } ?: 0 // Int subtraction handles RTP wrap.
        if (gap in -maxGapFrames until 0) {
            lateBuffers++
            return
        }
        nextSample = sample + frames
        if (gap in 1..maxGapFrames) {
            val replacement = ByteArray(gap * frameBytes)
            val fadeOut = minOf(gap, fadeFrames)
            for (frame in 0 until fadeOut) for (channel in 0 until channels) {
                putSample(replacement, frame, channel, tail[channel] * (fadeOut - frame - 1) / fadeOut)
            }
            write(replacement, replacement.size)
            concealedFrames += gap
            concealments++
            val fadeIn = minOf(frames, fadeFrames)
            for (frame in 0 until fadeIn) for (channel in 0 until channels) {
                putSample(data, frame, channel, getSample(data, frame, channel) * (frame + 1) / fadeIn)
            }
        }
        write(data, length)
        for (channel in 0 until channels) tail[channel] = getSample(data, frames - 1, channel)
    }

    private fun getSample(data: ByteArray, frame: Int, channel: Int): Int {
        val offset = frame * frameBytes + channel * 2
        return (data[offset].toInt() and 0xff) or (data[offset + 1].toInt() shl 8)
    }
    private fun putSample(data: ByteArray, frame: Int, channel: Int, sample: Int) {
        val offset = frame * frameBytes + channel * 2
        data[offset] = sample.toByte()
        data[offset + 1] = (sample shr 8).toByte()
    }
}
