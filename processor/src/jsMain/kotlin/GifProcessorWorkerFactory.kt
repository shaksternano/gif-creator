package com.shakster.gifcreator.processor

import com.shakster.gifcreator.shared.WorkerMessage
import com.shakster.gifcreator.shared.add
import com.shakster.gifcreator.shared.getByteArray
import com.shakster.gifcreator.shared.getIntArray
import com.shakster.gifkt.Image
import com.shakster.gifkt.quantizeGifImage
import com.shakster.gifkt.writeGifImage
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
        val (output, attachments) = try {
            val input = inputMessage.input
            when (input) {
                is GifProcessorInput.Quantize -> quantizeImage(input, inputMessage.attachments)
                is GifProcessorInput.Encode -> encodeGifImage(input, inputMessage.attachments)
            }
        } catch (t: Throwable) {
            WorkerMessage(
                GifProcessorOutput.Error(t.message ?: "An error occurred during processing"),
                Attachments.Empty,
            )
        }
        postOutput(output, attachments)
    }

    private fun quantizeImage(
        input: GifProcessorInput.Quantize,
        attachments: Attachments,
    ): WorkerMessage<GifProcessorOutput> {
        val optimizedArgb = attachments.getIntArray("optimizedImage") ?: error("Missing optimized image data")
        val originalArgb = attachments.getIntArray("originalImage") ?: optimizedArgb
        val image = Image(
            optimizedArgb,
            input.optimizedImage.width,
            input.optimizedImage.height,
        )
        val output = quantizeGifImage(
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
            Attachments {
                add("imageColorIndices", output.imageColorIndices)
                add("colorTable", output.colorTable)
                add("originalImage", originalArgb)
            },
        )
    }

    private fun encodeGifImage(
        input: GifProcessorInput.Encode,
        attachments: Attachments,
    ): WorkerMessage<GifProcessorOutput> {
        val imageColorIndices = attachments.getByteArray("imageColorIndices")
            ?: error("Missing image color indices data")
        val colorTable = attachments.getByteArray("colorTable")
            ?: error("Missing color table data")
        val buffer = Buffer()
        buffer.writeGifImage(
            input.quantizedImageInfo.toData(imageColorIndices, colorTable),
            input.durationCentiseconds,
            input.disposalMethod,
        )
        return WorkerMessage(
            GifProcessorOutput.Encode(input.durationCentiseconds),
            Attachments {
                add("bytes", buffer.readByteArray())
            },
        )
    }
}
