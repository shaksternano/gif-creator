package com.shakster.gifcreator.worker

import com.shakster.gifcreator.processor.GifProcessorInput
import com.shakster.gifcreator.processor.GifProcessorOutput
import com.shakster.gifcreator.processor.dimensions
import com.shakster.gifcreator.processor.info
import com.shakster.gifcreator.shared.*
import com.shakster.gifkt.AsyncGifEncoder
import com.shakster.gifkt.centiseconds
import com.shakster.gifkt.source
import com.varabyte.kobweb.worker.Attachments
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlin.time.Duration

class WorkerGifEncoder(
    sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    private val colorQuantizerSettings: ColorQuantizerSettings,
    colorDistanceCalculatorSettings: ColorDistanceCalculatorSettings,
    comment: String,
    alphaFill: Int,
    cropTransparent: Boolean,
    minimumFrameDurationCentiseconds: Int,
    maxConcurrency: Int,
    private val workerPool: WorkerPool<GifProcessorInput, GifProcessorOutput>,
    private val onFrameWrittenCallback: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : AsyncGifEncoder(
    sink,
    transparencyColorTolerance,
    quantizedTransparencyColorTolerance,
    loopCount,
    maxColors,
    colorQuantizerSettings.createQuantizer(),
    colorDistanceCalculatorSettings.createColorDistanceCalculator(),
    comment,
    alphaFill,
    cropTransparent,
    minimumFrameDurationCentiseconds,
    maxConcurrency,
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope,
) {

    override suspend fun quantizeImage(input: QuantizeInput): QuantizeOutput {
        val workerInput = GifProcessorInput.Quantize(
            maxColors,
            colorQuantizerSettings,
            optimizeQuantizedTransparency,
            input.optimizedImage.dimensions,
            input.originalImage.dimensions,
            input.durationCentiseconds,
            input.disposalMethod,
            input.optimizedPreviousFrame,
        )
        val inputAttachments = Attachments {
            add("optimizedImage", input.optimizedImage.argb)
            if (input.optimizedImage.argb !== input.originalImage.argb) {
                add("originalImage", input.originalImage.argb)
            }
        }

        val (output, attachments) = workerPool.submit(workerInput, inputAttachments)

        if (output !is GifProcessorOutput.Quantize) {
            throw createWrongOutputTypeException(output)
        }
        val imageColorIndices = attachments.getByteArray("imageColorIndices")
            ?: throw IllegalStateException("Image color indices are missing")
        val colorTable = attachments.getByteArray("colorTable")
            ?: throw IllegalStateException("Color table is missing")
        val originalImage = attachments.getIntArray("originalImage")
            ?: throw IllegalStateException("Original image data is missing")
        return QuantizeOutput(
            output.quantizedImageInfo.toData(imageColorIndices, colorTable),
            output.originalImage.toImage(originalImage),
            output.durationCentiseconds,
            output.disposalMethod,
            output.optimizedPreviousFrame,
        )
    }

    override suspend fun encodeGifImage(input: EncodeInput): EncodeOutput {
        val workerInput = GifProcessorInput.Encode(
            input.imageData.info,
            input.durationCentiseconds,
            input.disposalMethod,
        )
        val inputAttachments = Attachments {
            add("imageColorIndices", input.imageData.imageColorIndices)
            add("colorTable", input.imageData.colorTable)
        }

        val (output, attachments) = workerPool.submit(workerInput, inputAttachments)

        if (output !is GifProcessorOutput.Encode) {
            throw createWrongOutputTypeException(output)
        }
        val bytes = attachments.getByteArray("bytes")
            ?: throw IllegalStateException("Byte data is missing")
        return EncodeOutput(
            bytes.source().buffered(),
            output.durationCentiseconds.centiseconds,
        )
    }

    override suspend fun onFrameWritten(framesWritten: Int, writtenDuration: Duration) {
        onFrameWrittenCallback(framesWritten, writtenDuration)
    }

    private fun createWrongOutputTypeException(output: GifProcessorOutput): IllegalStateException {
        return IllegalStateException("Wrong output type received: $output")
    }
}
