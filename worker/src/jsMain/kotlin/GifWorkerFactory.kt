package com.shakster.gifcreator.worker

import com.shakster.gifcreator.processor.GifProcessorInput
import com.shakster.gifcreator.processor.GifProcessorOutput
import com.shakster.gifcreator.processor.GifProcessorWorker
import com.shakster.gifcreator.shared.WorkerMessage
import com.shakster.gifcreator.shared.asInt8Array
import com.varabyte.kobweb.serialization.IOSerializer
import com.varabyte.kobweb.serialization.createIOSerializer
import com.varabyte.kobweb.worker.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import org.w3c.dom.MessagePort
import kotlin.time.Duration

internal class GifWorkerFactory : WorkerFactory<GifWorkerInput, GifWorkerOutput> {

    override fun createStrategy(postOutput: OutputDispatcher<GifWorkerOutput>): WorkerStrategy<GifWorkerInput> {
        return GifWorkerStrategy(postOutput)
    }

    override fun createIOSerializer(): IOSerializer<GifWorkerInput, GifWorkerOutput> {
        return Json.createIOSerializer()
    }
}

private val OK_OUTPUT: WorkerMessage<GifWorkerOutput> = WorkerMessage(
    GifWorkerOutput.Ok,
    Attachments.Empty,
)

private class GifWorkerStrategy(
    private val postOutput: OutputDispatcher<GifWorkerOutput>,
) : WorkerStrategy<GifWorkerInput>() {

    private val hardwareConcurrency: Int = self.navigator.hardwareConcurrency.toInt()

    private val workerPool: WorkerPool<GifProcessorInput, GifProcessorOutput> = WorkerPool(
        size = hardwareConcurrency,
        createWorker = ::GifProcessorWorker,
    )

    private var messagePort: MessagePort? = null
    private var encoder: WorkerGifEncoder? = null

    override fun onInput(inputMessage: InputMessage<GifWorkerInput>) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val (output, outputAttachments) = try {
                val input = inputMessage.input
                val inputAttachments = inputMessage.attachments
                when (input) {
                    is GifWorkerInput.MediaQuery -> getMediaInfo(inputAttachments)
                    is GifWorkerInput.MessagePort -> setMessagePort(inputAttachments)
                    is GifWorkerInput.EncoderInit -> initEncoder(input)
                    is GifWorkerInput.Frame -> writeFrame(input, inputAttachments)
                    is GifWorkerInput.Frames -> writeFrames(input, inputAttachments)
                    is GifWorkerInput.EncoderClose -> closeEncoder()
                    is GifWorkerInput.Shutdown -> shutdown()
                }
            } catch (t: Throwable) {
                WorkerMessage(
                    GifWorkerOutput.Error(t.message ?: "An error occurred during processing"),
                    Attachments.Empty,
                )
            }
            postOutput(output, outputAttachments)
        }
    }

    private fun setMessagePort(attachments: Attachments): WorkerMessage<GifWorkerOutput> {
        messagePort = attachments.getMessagePort("port")
            ?: throw IllegalStateException("Message port is missing")
        return OK_OUTPUT
    }

    private suspend fun getMediaInfo(attachments: Attachments): WorkerMessage<GifWorkerOutput> {
        val file = attachments.getFile("file")
            ?: throw IllegalStateException("Decoder input file is missing")
        val reader = ImageReader.create(file, Duration.ZERO)
        return WorkerMessage(
            GifWorkerOutput.MediaQueryResult(
                reader.frameCount,
                reader.isVideo,
            ),
            Attachments.Empty,
        )
    }

    private fun initEncoder(input: GifWorkerInput.EncoderInit): WorkerMessage<GifWorkerOutput> {
        val messagePort = messagePort
        val onFrameWritten = if (messagePort == null) {
            { _, _ -> }
        } else {
            { framesWritten: Int, writtenDuration: Duration ->
                val event = GifFrameWrittenEvent(
                    framesWritten,
                    writtenDuration,
                )
                val json = Json.encodeToString(event)
                messagePort.postMessage(json)
            }
        }
        encoder = WorkerGifEncoder(
            Buffer(),
            input.transparencyColorTolerance,
            input.quantizedTransparencyColorTolerance,
            input.loopCount,
            input.maxColors,
            input.colorQuantizerSettings,
            input.colorSimilarityCheckerSettings,
            input.comment,
            input.alphaFill,
            input.cropTransparent,
            input.minimumFrameDurationCentiseconds,
            hardwareConcurrency,
            workerPool,
            onFrameWritten,
        )
        return OK_OUTPUT
    }

    private suspend fun writeFrame(
        input: GifWorkerInput.Frame,
        attachments: Attachments,
    ): WorkerMessage<GifWorkerOutput> {
        val image = attachments.getImageBitmap("image")
            ?: throw IllegalStateException("Image data is missing")
        getEncoder().writeFrame(image, input.duration)
        return OK_OUTPUT
    }

    private suspend fun writeFrames(
        input: GifWorkerInput.Frames,
        attachments: Attachments,
    ): WorkerMessage<GifWorkerOutput> {
        val file = attachments.getFile("file")
            ?: throw IllegalStateException("Decoder input file is missing")
        val encoder = getEncoder()
        val reader = ImageReader.create(file, input.duration)
        reader.readFrames().collect { frame ->
            encoder.writeFrame(frame)
        }
        return OK_OUTPUT
    }

    private suspend fun closeEncoder(): WorkerMessage<GifWorkerOutput> {
        return try {
            val encoder = getEncoder()
            encoder.close()
            val bytes = encoder.buffer.readByteArray().asInt8Array().buffer
            WorkerMessage(
                GifWorkerOutput.Ok,
                Attachments {
                    add("bytes", bytes)
                },
            )
        } finally {
            this.encoder = null
        }
    }

    private suspend fun shutdown(): WorkerMessage<GifWorkerOutput> {
        workerPool.shutdown()
        return OK_OUTPUT
    }

    private fun getEncoder(): WorkerGifEncoder {
        return encoder ?: throw IllegalStateException("Encoder not initialized")
    }
}
