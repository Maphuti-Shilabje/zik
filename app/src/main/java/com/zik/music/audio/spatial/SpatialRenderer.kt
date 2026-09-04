package com.zik.music.audio.spatial

interface SpatialRenderer {
    fun configure(sampleRate: Int, channelCount: Int)
    fun processFrame(
        leftSample: Short,
        rightSample: Short,
        position: Vector3,
        parameters: SpatialParameters,
        transitionWeight: Float
    ): Pair<Short, Short>
    fun reset()
}
