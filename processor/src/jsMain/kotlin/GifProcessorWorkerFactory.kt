package com.shakster.gifcreator.processor

import com.shakster.gifcreator.util.add
import com.shakster.gifcreator.util.getByteArray
import com.shakster.gifcreator.util.getIntArray
import com.shakster.gifkt.internal.Image
import com.shakster.gifkt.internal.getImageData
import com.shakster.gifkt.internal.writeGifImage
import com.varabyte.kobweb.serialization.IOSerializer
import com.varabyte.kobweb.serialization.createIOSerializer
import com.varabyte.kobweb.worker.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

internal class GifProcessorWorkerFactory : WorkerFactory<GifProcessorInput, GifProcessorOutput> {

    override fun createStrategy(postOutput: OutputDispatcher<GifProcessorOutput>): WorkerStrategy<GifProcessorInput> {
        return GifProcessorWorkerStrategy(postOutput)
    }

    override fun createIOSerializer(): IOSerializer<GifProcessorInput, GifProcessorOutput> {
        return Json.createIOSerializer()
    }
}

private class GifProcessorWorkerStrategy(
    private val postOutput: OutputDispatcher<GifProcessorOutput>,
) : WorkerStrategy<GifProcessorInput>() {

    override fun onInput(inputMessage: InputMessage<GifProcessorInput>) {
        val input = inputMessage.input
        when (input) {
            is GifProcessorInput.Quantize -> quantizeImage(input, inputMessage.transferables)
            is GifProcessorInput.Encode -> encodeGifImage(input, inputMessage.transferables)
        }
    }

    private fun quantizeImage(input: GifProcessorInput.Quantize, transferables: Transferables) {
        val optimizedArgb = transferables.getIntArray("optimizedImage") ?: error("Missing optimized image data")
        val originalArgb = transferables.getIntArray("originalImage") ?: error("Missing original image data")
        val image = Image(
            optimizedArgb,
            input.optimizedImage.width,
            input.optimizedImage.height,
        )
        val output = getImageData(
            image,
            input.maxColors,
            input.colorQuantizerSettings.createQuantizer(),
            input.optimizeQuantizedTransparency,
        )
        postOutput(
            GifProcessorOutput.Quantize(
                output.info,
                input.originalImage,
                input.durationCentiseconds,
                input.disposalMethod,
                input.optimizedPreviousFrame,
            ),
            Transferables {
                add("imageColorIndices", output.imageColorIndices)
                add("colorTable", output.colorTable)
                add("originalImage", originalArgb)
            }
        )
    }

    private fun encodeGifImage(input: GifProcessorInput.Encode, transferables: Transferables) {
        val imageColorIndices = transferables.getByteArray("imageColorIndices")
            ?: error("Missing image color indices data")
        val colorTable = transferables.getByteArray("colorTable")
            ?: error("Missing color table data")
        val buffer = Buffer()
        buffer.writeGifImage(
            input.quantizedImageInfo.toData(imageColorIndices, colorTable),
            input.durationCentiseconds,
            input.disposalMethod,
        )
        postOutput(
            GifProcessorOutput.Encode(input.durationCentiseconds),
            Transferables {
                add("bytes", buffer.readByteArray())
            }
        )
    }
}
