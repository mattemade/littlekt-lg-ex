package com.lehaine.littlekt.audio

import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.seconds
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.*
import java.nio.IntBuffer

/**
 * @author Colton Daily
 * @date 12/19/2021
 */
class OpenALAudioStream(
    private val context: OpenALAudioContext,
    private val read: (ByteArray) -> Int,
    private val reset: () -> Unit,
    val channels: Int,
    val sampleRate: Int
) : AudioStream {
    private val NO_DEVICE get() = context.NO_DEVICE

    private val tempBytes = ByteArray(BUFFER_SIZE)
    private val tempBuffer = BufferUtils.createByteBuffer(BUFFER_SIZE)

    private var sourceID = -1

    private val maxSecondsToBuffer = (BUFFER_SIZE.toFloat() / (BYTES_PER_SAMPLE * channels * sampleRate)).seconds
    private val format = if (channels > 1) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
    private var buffers: IntBuffer? = null

    override var looping = false

    override var volume: Float = 1f
        set(value) {
            check(value > 0) { "Volume must be greater than 0!" }
            field = value
            if (NO_DEVICE) return
            if (sourceID != -1) {
                alSourcef(sourceID, AL_GAIN, value)
            }
        }

    private var isPlaying = false
    override val playing: Boolean
        get() {
            if (NO_DEVICE || sourceID == -1) {
                return false
            }
            return isPlaying
        }


    init {
        if (NO_DEVICE) {
            logger.error { "Unable to retrieve audio device!" }
        }
    }

    override fun play(volume: Float, loop: Boolean) = withDevice {
        if (sourceID == -1) {
            sourceID = context.obtainSource()
            if (sourceID == -1) return@withDevice

            context.audioStreams += this

            if (buffers == null) {
                buffers = BufferUtils.createIntBuffer(BUFFER_COUNT).also {
                    alGetError()
                    alGenBuffers(it)
                    val errorCode = alGetError()
                    if (errorCode != AL_NO_ERROR) {
                        error("Unable to allocate audio buffers. AL Error: $errorCode")
                    }
                }
            }
            alSourcei(sourceID, AL_LOOPING, AL_FALSE)

            this.volume = volume
            this.looping = looping
            alSourcef(sourceID, AL_GAIN, volume)

            alGetError()

            buffers?.let { buffers ->
                for (i in 0 until BUFFER_COUNT) {
                    val bufferID = buffers.get(i)
                    if (!fill(bufferID)) break
                    alSourceQueueBuffers(sourceID, bufferID)
                }
            }
            if (alGetError() != AL_NO_ERROR) {
                stop()
                return@withDevice
            }
        }

        if (!isPlaying) {
            alSourcePlay(sourceID)
            isPlaying = true
        }
    }

    override fun stop() = withDevice {
        if (sourceID == -1) return@withDevice
        context.audioStreams -= this
        reset()

        alSourceStop(sourceID);
        alSourcei(sourceID, AL_BUFFER, 0);

        sourceID = -1
        isPlaying = false
    }

    override fun resume() = withDevice {
        if (sourceID != -1) {
            alSourcePlay(sourceID)
            isPlaying = true
        }
    }

    override fun pause() = withDevice {
        if (sourceID != -1) {
            alSourcePause(sourceID)
        }
        isPlaying = false
    }

    override fun dispose() = withDevice {
        stop()
        buffers?.let {
            alDeleteBuffers(it)
        } ?: return@withDevice
        buffers = null
    }

    fun update() = withDevice {
        if (sourceID == -1) return@withDevice
        var end = false
        var buffers = alGetSourcei(sourceID, AL_BUFFERS_PROCESSED)

        while (buffers-- > 0) {
            val bufferID = alSourceUnqueueBuffers(sourceID)
            if (bufferID == AL_INVALID_VALUE) break
            if (end) continue
            if (fill(bufferID)) {
                alSourceQueueBuffers(sourceID, bufferID)
            } else {
                end = true
            }
        }
        if (end && alGetSourcei(sourceID, AL_BUFFERS_QUEUED) == 0) {
            stop()
        }

        if (isPlaying && alGetSourcei(sourceID, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(sourceID)
        }
    }

    private fun fill(bufferID: Int): Boolean {
        tempBuffer.clear()
        var length = read(tempBytes)
        if (length <= 0) {
            if (looping) {
                reset()
                length = read(tempBytes)
                if (length <= 0) return false
            } else {
                return false
            }
        }
        tempBuffer.put(tempBytes, 0, length).flip()
        alBufferData(bufferID, format, tempBuffer, sampleRate)

        return true
    }

    private inline fun withDevice(block: () -> Unit) {
        if (NO_DEVICE) return
        block()
    }

    companion object {
        private val logger = Logger<OpenALAudioStream>()
        private const val BUFFER_SIZE = 4096 * 10
        private const val BUFFER_COUNT = 3
        private const val BYTES_PER_SAMPLE = 2
    }
}