package com.shakster.gifcreator.processor

import com.shakster.gifcreator.shared.WorkerMessage
import com.shakster.gifcreator.shared.add
import com.shakster.gifcreator.shared.getByteArray
import com.shakster.gifcreator.shared.getIntArray
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
        val (output, transferables) = try {
            val input = inputMessage.input
            when (input) {
                is GifProcessorInput.Quantize -> quantizeImage(input, inputMessage.transferables)
                is GifProcessorInput.Encode -> encodeGifImage(input, inputMessage.transferables)
            }
        } catch (t: Throwable) {
            WorkerMessage(
                GifProcessorOutput.Error(t.message ?: "An error occurred during processing"),
                Transferables.Empty,
            )
        }
        postOutput(output, transferables)
    }

    private fun quantizeImage(
        input: GifProcessorInput.Quantize,
        transferables: Transferables,
    ): WorkerMessage<GifProcessorOutput> {
        val optimizedArgb = transferables.getIntArray("optimizedImage") ?: error("Missing optimized image data")
        val originalArgb = transferables.getIntArray("originalImage") ?: optimizedArgb
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
        return WorkerMessage(
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
            },
        )
    }

    private fun encodeGifImage(
        input: GifProcessorInput.Encode,
        transferables: Transferables,
    ): WorkerMessage<GifProcessorOutput> {
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
        return WorkerMessage(
            GifProcessorOutput.Encode(input.durationCentiseconds),
            Transferables {
                add("bytes", buffer.readByteArray())
            },
        )
    }
}
