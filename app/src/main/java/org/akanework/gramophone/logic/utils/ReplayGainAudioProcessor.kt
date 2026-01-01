package org.akanework.gramophone.logic.utils

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.Log
import org.nift4.gramophone.hificore.AdaptiveDynamicRangeCompression
import java.nio.ByteBuffer

class ReplayGainAudioProcessor : BaseAudioProcessor() {
    companion object {
        private const val TAG = "ReplayGainAP"
    }

    private var compressor: AdaptiveDynamicRangeCompression? = null
    private var waitingForFlush = false
    var mode = ReplayGainUtil.Mode.None
        private set
    var rgGain = 0 // dB
        private set
    var nonRgGain = 0 // dB
        private set
    var boostGain = 0 // dB
        private set
    var offloadEnabled = false
        private set
    var reduceGain = false
        private set
    var settingsChangedListener: (() -> Unit)? = null
    var boostGainChangedListener: (() -> Unit)? = null
    var offloadEnabledChangedListener: (() -> Unit)? = null
    private var gain = 1f
    private var kneeThresholdDb: Float? = null
    private var tags: ReplayGainUtil.ReplayGainInfo? = null
    override fun queueInput(inputBuffer: ByteBuffer) {
        val frameCount = inputBuffer.remaining() / inputAudioFormat.bytesPerFrame
        val outputBuffer = replaceOutputBuffer(frameCount * outputAudioFormat.bytesPerFrame)
        if (inputBuffer.hasRemaining()) {
            if (compressor != null) {
                compressor!!.compress(
                    inputAudioFormat.channelCount,
                    gain,
                    kneeThresholdDb!!, 1f, inputBuffer,
                    outputBuffer, frameCount
                )
                inputBuffer.position(inputBuffer.limit())
                outputBuffer.position(frameCount * outputAudioFormat.bytesPerFrame)
            } else {
                if (gain == 1f) {
                    outputBuffer.put(inputBuffer)
                } else {
                    while (inputBuffer.hasRemaining()) {
                        outputBuffer.putFloat(inputBuffer.getFloat() * gain)
                    }
                }
            }
        }
        outputBuffer.flip()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw UnhandledAudioFormatException(
                "Invalid PCM encoding. Expected float PCM.", inputAudioFormat
            )
        }
        if (Flags.TEST_RG_OFFLOAD) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    @Synchronized
    fun setMode(mode: ReplayGainUtil.Mode, doNotNotifyListener: Boolean) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.mode == mode) {
                return
            }
            listener = settingsChangedListener
            this.mode = mode
        }
        if (!doNotNotifyListener) {
            listener?.invoke()
            computeGain()
        }
    }

    @Synchronized
    fun setRgGain(rgGain: Int) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.rgGain == rgGain) {
                return
            }
            listener = settingsChangedListener
            this.rgGain = rgGain
        }
        listener?.invoke()
        computeGain()
    }

    @Synchronized
    fun setNonRgGain(nonRgGain: Int) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.nonRgGain == nonRgGain) {
                return
            }
            listener = settingsChangedListener
            this.nonRgGain = nonRgGain
        }
        listener?.invoke()
        computeGain()
    }

    fun setBoostGain(boostGain: Int) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.boostGain == boostGain) {
                return
            }
            listener = boostGainChangedListener
            this.boostGain = boostGain
        }
        listener?.invoke()
        computeGain()
    }

    @Synchronized
    fun setReduceGain(reduceGain: Boolean) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.reduceGain == reduceGain) {
                return
            }
            listener = settingsChangedListener
            this.reduceGain = reduceGain
        }
        listener?.invoke()
        computeGain()
    }

    fun setOffloadEnabled(offloadEnabled: Boolean) {
        val listener: (() -> Unit)?
        synchronized(this) {
            if (this.offloadEnabled == offloadEnabled) {
                return
            }
            listener = offloadEnabledChangedListener
            this.offloadEnabled = offloadEnabled
        }
        listener?.invoke()
        computeGain()
    }

    fun setRootFormat(inputFormat: Format) {
        tags = ReplayGainUtil.parse(inputFormat)
    }

    private fun computeGain() {
        val mode: ReplayGainUtil.Mode
        val rgGain: Int
        val nonRgGain: Int
        val reduceGain: Boolean
        synchronized(this) {
            mode = this.mode
            rgGain = this.rgGain
            nonRgGain = this.nonRgGain
            reduceGain = this.reduceGain
        }
        val gain = ReplayGainUtil.calculateGain(
            tags, mode, rgGain,
            reduceGain, ReplayGainUtil.RATIO
        )
        this.gain = gain?.first ?: ReplayGainUtil.dbToAmpl(nonRgGain.toFloat())
        this.kneeThresholdDb = gain?.second
        if (kneeThresholdDb != null) {
            if (compressor == null)
                compressor = AdaptiveDynamicRangeCompression()
            Log.w(TAG, "using dynamic range compression")
            compressor!!.init(
                inputAudioFormat.sampleRate,
                ReplayGainUtil.TAU_ATTACK, ReplayGainUtil.TAU_RELEASE,
                ReplayGainUtil.RATIO
            )
        } else {
            onReset() // delete compressor
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        waitingForFlush = false
        computeGain()
    }

    override fun onReset() {
        compressor?.release()
        compressor = null
    }
}