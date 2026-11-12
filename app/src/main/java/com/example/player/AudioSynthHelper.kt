package com.example.player

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import kotlin.random.Random

object AudioSynthHelper {

    private const val SAMPLE_RATE = 22050
    private const val DURATION_SEC = 6

    // Musical scale frequencies (Pentatonic major & minor for pleasant sounding synth harmonies)
    private val NOTES = doubleArrayOf(
        220.0, 246.94, 261.63, 293.66, 329.63, 392.00, 440.0, 493.88, 523.25, 587.33, 659.25, 783.99
    )

    /**
     * Generates a cached WAV audio track for any song/artist.
     * Uses a deterministic seed based on track id/artist so it always plays the same unique melody for that song.
     */
    fun getOrCreateTrackAudio(context: Context, trackId: String, artistName: String): File {
        val cacheDir = File(context.cacheDir, "audio_tracks")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val sanitized = trackId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val file = File(cacheDir, "track_$sanitized.wav")
        if (file.exists() && file.length() > 1000) {
            return file
        }

        try {
            generateWav(file, artistName, trackId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    private fun generateWav(file: File, artistName: String, trackTitle: String) {
        val totalSamples = SAMPLE_RATE * DURATION_SEC
        val dataSize = totalSamples * 2 // 16-bit mono
        val buffer = ShortArray(totalSamples)

        val seed = (artistName + trackTitle).hashCode().toLong()
        val rng = Random(seed)

        // Generate chord progression
        val chordCount = 4
        val chordRootIndices = intArrayOf(
            rng.nextInt(NOTES.size - 4),
            rng.nextInt(NOTES.size - 4),
            rng.nextInt(NOTES.size - 4),
            rng.nextInt(NOTES.size - 4)
        )
        val chordDuration = totalSamples / chordCount
        val tempoBpm = 95 + rng.nextInt(35)
        val samplesPerBeat = (SAMPLE_RATE * 60) / tempoBpm

        var phaseBass = 0.0
        var phaseLead = 0.0
        var phasePad = 0.0

        for (i in 0 until totalSamples) {
            val chordIdx = (i / chordDuration) % chordCount
            val baseFreq = NOTES[chordRootIndices[chordIdx]]

            // Arpeggio note
            val beatPhase = (i % samplesPerBeat).toDouble() / samplesPerBeat
            val arpStep = ((i / (samplesPerBeat / 4)) % 4)
            val leadFreq = baseFreq * when (arpStep) {
                0 -> 1.0
                1 -> 1.2599
                2 -> 1.4983
                else -> 2.0
            }

            // Envelope for notes
            val leadEnv = 1.0 - (beatPhase * 0.7)
            val padFreq = baseFreq * 0.5
            val bassFreq = baseFreq * 0.25

            phaseLead += 2.0 * Math.PI * leadFreq / SAMPLE_RATE
            phasePad += 2.0 * Math.PI * padFreq / SAMPLE_RATE
            phaseBass += 2.0 * Math.PI * bassFreq / SAMPLE_RATE

            val leadWave = sin(phaseLead) * leadEnv * 0.4
            val padWave = sin(phasePad) * 0.25
            val bassWave = (sin(phaseBass) + 0.3 * sin(phaseBass * 2.0)) * 0.35

            // Kick / rhythm pulse
            val kickPhase = (i % samplesPerBeat).toDouble() / samplesPerBeat
            val kick = if (kickPhase < 0.15) {
                val decay = 1.0 - (kickPhase / 0.15)
                sin(2.0 * Math.PI * (60.0 + 80.0 * decay) * kickPhase) * decay * 0.45
            } else 0.0

            val mixed = (leadWave + padWave + bassWave + kick).coerceIn(-1.0, 1.0)
            buffer[i] = (mixed * 32767.0 * 0.75).toInt().toShort()
        }

        FileOutputStream(file).use { fos ->
            writeWavHeader(fos, totalSamples, SAMPLE_RATE, 1, 16)
            val byteBuf = ByteBuffer.allocate(buffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in buffer) {
                byteBuf.putShort(s)
            }
            fos.write(byteBuf.array())
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen * channels * (bitsPerSample / 8) + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val header = ByteArray(44)

        // "RIFF"
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // file length - 8
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // "WAVE"
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // SubChunk1Size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat (1 for PCM)
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        // SampleRate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        // BlockAlign
        header[32] = (channels * (bitsPerSample / 8)).toByte()
        header[33] = 0
        // BitsPerSample
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // "data"
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        // Data length
        val dataLen = totalAudioLen * channels * (bitsPerSample / 8)
        header[40] = (dataLen and 0xff).toByte()
        header[41] = ((dataLen shr 8) and 0xff).toByte()
        header[42] = ((dataLen shr 16) and 0xff).toByte()
        header[43] = ((dataLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
