package com.zik.music.audio.spatial

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class SpatialMotionProcessor(
    initialParameters: SpatialParameters = SpatialParameters(),
    initialMotionModel: MotionModel = OrbitMotion(initialParameters.radius, initialParameters.speedHz),
    initialRenderer: SpatialRenderer = StereoSpatialRenderer()
) : BaseAudioProcessor() {

    @Volatile
    var parameters: SpatialParameters = initialParameters
        set(value) {
            field = value
            if (motionModel is OrbitMotion) {
                motionModel = OrbitMotion(value.radius, value.speedHz)
            }
        }

    @Volatile
    var motionModel: MotionModel = initialMotionModel

    @Volatile
    var renderer: SpatialRenderer = initialRenderer

    @Volatile
    private var configuredInputFormat: AudioFormat = AudioFormat.NOT_SET

    private var processedFrameCount: Long = 0L
    private var transitionProgress: Float = if (initialParameters.isEnabled) 1.0f else 0.0f

    // 50ms transition ramp step
    private var rampStepPerFrame: Float = 1.0f / (44100f * 0.050f)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // Spatial motion DSP currently supports stereo 16-bit PCM
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            configuredInputFormat = AudioFormat.NOT_SET
            throw UnhandledAudioFormatException(inputAudioFormat)
        }

        configuredInputFormat = inputAudioFormat
        val rate = inputAudioFormat.sampleRate
        rampStepPerFrame = 1.0f / (rate * 0.050f)
        renderer.configure(rate, inputAudioFormat.channelCount)

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }

        val frameCount = remaining / 4 // 2 channels * 2 bytes per sample = 4 bytes per stereo frame
        val targetProgress = if (parameters.isEnabled) 1.0f else 0.0f

        // True byte-for-byte passthrough branch when fully bypassed
        if (!parameters.isEnabled && transitionProgress <= 0.0f) {
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            processedFrameCount += frameCount
            return
        }

        // Active spatial processing or transition ramp branch
        val outputBuffer = replaceOutputBuffer(remaining)
        val rate = configuredInputFormat.sampleRate.toDouble()

        val inputShorts = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val outputShorts = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()

        for (i in 0 until frameCount) {
            // Update transition ramp smoothly without clicks
            if (transitionProgress < targetProgress) {
                transitionProgress = (transitionProgress + rampStepPerFrame).coerceAtMost(targetProgress)
            } else if (transitionProgress > targetProgress) {
                transitionProgress = (transitionProgress - rampStepPerFrame).coerceAtLeast(targetProgress)
            }

            val timeSeconds = (processedFrameCount + i) / rate
            val position = motionModel.positionAt(timeSeconds)

            val leftIn = inputShorts.get()
            val rightIn = inputShorts.get()

            val (leftOut, rightOut) = renderer.processFrame(
                leftSample = leftIn,
                rightSample = rightIn,
                position = position,
                parameters = parameters,
                transitionWeight = transitionProgress
            )

            outputShorts.put(leftOut)
            outputShorts.put(rightOut)
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(0)
        outputBuffer.limit(remaining)

        processedFrameCount += frameCount
    }

    fun getProcessedFrameCount(): Long = processedFrameCount
    fun getTransitionProgress(): Float = transitionProgress
    fun getActiveAudioFormat(): AudioFormat = configuredInputFormat

    override fun onFlush() {
        renderer.reset()
    }

    override fun onReset() {
        configuredInputFormat = AudioFormat.NOT_SET
        processedFrameCount = 0L
        transitionProgress = if (parameters.isEnabled) 1.0f else 0.0f
        renderer.reset()
    }
}
