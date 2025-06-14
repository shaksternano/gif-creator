package com.shakster.gifcreator.worker

import com.shakster.gifcreator.processor.GifProcessorInput
import com.shakster.gifcreator.processor.GifProcessorOutput
import com.shakster.gifcreator.processor.GifProcessorWorker
import com.shakster.gifcreator.shared.add
import com.shakster.gifcreator.shared.asIntArray
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

private class GifWorkerStrategy(
    private val postOutput: OutputDispatcher<GifWorkerOutput>,
) : WorkerStrategy<GifWorkerInput>() {

    private val hardwareConcurrency: Int = self.navigator.hardwareConcurrency.toInt()

    private val workerPool: WorkerPool<GifProcessorInput, GifProcessorOutput> = WorkerPool(
        size = hardwareConcurrency,
        createWorker = ::GifProcessorWorker,
    )

    private var messagePort: MessagePort? = null
    private var buffer: Buffer? = null
    private var encoder: WorkerGifEncoder? = null

    override fun onInput(inputMessage: InputMessage<GifWorkerInput>) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val inputTransferables = inputMessage.transferables
            var outputTransferables: Transferables = Transferables.Empty
            val output = try {
                val input = inputMessage.input
                when (input) {
                    is GifWorkerInput.MessagePort -> setMessagePort(inputTransferables)
                    is GifWorkerInput.EncoderInit -> initEncoder(input)
                    is GifWorkerInput.Frame -> writeFrame(input, inputTransferables)
                    is GifWorkerInput.EncoderClose -> outputTransferables = closeEncoder()
                    is GifWorkerInput.Shutdown -> shutdown()
                }
                GifWorkerOutput.Ok
            } catch (t: Throwable) {
                GifWorkerOutput.Error(t.message ?: "An error occurred during processing")
            }
            postOutput(output, outputTransferables)
        }
    }

    private fun setMessagePort(transferables: Transferables) {
        messagePort = transferables.getMessagePort("port")
    }

    private fun initEncoder(input: GifWorkerInput.EncoderInit) {
        val buffer = Buffer()
        this.buffer = buffer
        val messagePort = messagePort
        val onFrameWritten = if (messagePort == null) {
            { _, _ -> }
        } else {
            { framesWritten: Int, writtenDuration: Duration ->
                val event = GifFrameWrittenEvent(framesWritten, writtenDuration)
                val json = Json.encodeToString(event)
                messagePort.postMessage(json)
            }
        }
        encoder = WorkerGifEncoder(
            buffer,
            input.transparencyColorTolerance,
            input.quantizedTransparencyColorTolerance,
            input.loopCount,
            input.maxColors,
            input.colorQuantizerSettings,
            input.colorDistanceCalculatorSettings,
            input.comment,
            input.alphaFill,
            input.cropTransparent,
            input.minimumFrameDurationCentiseconds,
            hardwareConcurrency,
            workerPool,
            onFrameWritten,
        )
    }

    private suspend fun writeFrame(input: GifWorkerInput.Frame, transferables: Transferables) {
        val image = transferables.getInt32Array("argb")
            ?: throw IllegalStateException("Image argb is missing")
        getEncoder().writeFrame(
            image.asIntArray(),
            input.width,
            input.height,
            input.duration,
        )
    }

    private suspend fun closeEncoder(): Transferables {
        getEncoder().close()
        val buffer = this.buffer ?: throw IllegalStateException("Buffer not initialized")
        this.buffer = null
        this.encoder = null
        val bytes = buffer.readByteArray()
        return Transferables {
            add("bytes", bytes)
        }
    }

    private suspend fun shutdown() {
        workerPool.shutdown()
    }

    private fun getEncoder(): WorkerGifEncoder {
        return encoder ?: throw IllegalStateException("Encoder not initialized")
    }
}
