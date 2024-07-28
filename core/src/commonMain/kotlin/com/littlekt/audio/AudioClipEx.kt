package com.littlekt.audio

interface AudioClipEx : AudioClip {

    /**
     * Play this spatial audio clip.
     *
     * @param volume the volume to the audio. Defaults to the current [volume].
     * @param positionX the X world coordinate of the audio. Defaults to 0.
     * @param positionY the Y world coordinate of the audio. Defaults to 0.
     * @param referenceDistance the distance at which volume starts to reduce. Defaults to 1.
     * @param maxDistance the distance at which sound is silenced completely. Defaults to 10000.
     * @param rolloffFactor the speed of sound getting quieter with distance. Defaults to 1.
     * @param loop whether to loop this audio.
     *
     * @return sound ID that could be used in other methods ([stop], [pause], [play], [setVolume],
     *   [setPosition]).
     */
    fun play(
        volume: Float = this.volume,
        positionX: Float = 0f,
        positionY: Float = 0f,
        referenceDistance: Float = 1f,
        maxDistance: Float = 10000f,
        rolloffFactor: Float = 1f,
        loop: Boolean = false
    ): Int

    fun setVolumeAll(volume: Float)
    fun setVolume(id: Int, volume: Float)
    fun setPositionAll(positionX: Float, positionY: Float)
    fun setPosition(id: Int, positionX: Float, positionY: Float)

    fun stopAll()
    fun stop(id: Int)
    fun pauseAll()
    fun pause(id: Int)
    fun resumeAll()
    fun resume(id: Int)
}
