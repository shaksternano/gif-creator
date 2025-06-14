package com.shakster.gifcreator.worker

import com.shakster.gifcreator.processor.GifProcessorInput
import com.shakster.gifcreator.processor.GifProcessorOutput
import com.shakster.gifcreator.processor.dimensions
import com.shakster.gifcreator.processor.info
import com.shakster.gifcreator.shared.*
import com.shakster.gifkt.SuspendClosable
import com.shakster.gifkt.internal.*
import com.varabyte.kobweb.worker.Transferables
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration

class WorkerGifEncoder(
    private val sink: Sink,
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
    private val onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : SuspendClosable {

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
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
    )

    private var throwable: Throwable? = null

    private val quantizeExecutor: AsyncExecutor<Pair<GifProcessorInput.Quantize, Transferables>, WorkerResult<GifProcessorOutput>> =
        AsyncExecutor(
            maxConcurrency,
            task = ::submitToWorkerPool,
            onOutput = ::writeOrOptimizeGifImage,
        )

    private val encodeExecutor: AsyncExecutor<Pair<GifProcessorInput.Encode, Transferables>, WorkerResult<GifProcessorOutput>> =
        AsyncExecutor(
            maxConcurrency,
            task = ::submitToWorkerPool,
            onOutput = ::transferToSink,
        )

    private val writtenFrameNotifications: Channel<Duration> = Channel(capacity = Channel.UNLIMITED)

    @OptIn(DelicateCoroutinesApi::class)
    private val writtenFrameListener: Job = GlobalScope.launch {
        var framesWritten = 0
        var writtenDuration = Duration.ZERO
        for (duration in writtenFrameNotifications) {
            framesWritten++
            writtenDuration += duration
            try {
                onFrameWritten(framesWritten, writtenDuration)
            } catch (t: Throwable) {
                if (throwable == null) {
                    throwable = Exception("Error running onFrameWritten callback", t)
                }
                break
            }
        }
    }

    suspend fun writeFrame(input: GifWorkerInput.Frame, transferables: Transferables) {
        val throwable = throwable
        if (throwable != null) {
            throw createException(throwable)
        }

        val image = transferables.getInt32Array("argb")
            ?: throw IllegalStateException("Image argb is missing")

        val written = baseEncoder.writeFrame(
            image.asIntArray(),
            input.width,
            input.height,
            input.duration,
            quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                quantizeAndWriteFrame(
                    optimizedImage,
                    originalImage,
                    durationCentiseconds,
                    disposalMethod,
                    optimizedPreviousFrame,
                )
            },
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            writtenFrameNotifications.send(Duration.ZERO)
        }
    }

    private suspend fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) {
        quantizeExecutor.submit(
            GifProcessorInput.Quantize(
                baseEncoder.maxColors,
                colorQuantizerSettings,
                baseEncoder.optimizeQuantizedTransparency,
                optimizedImage.dimensions,
                originalImage.dimensions,
                durationCentiseconds,
                disposalMethod,
                optimizedPreviousFrame,
            ) to Transferables {
                add("optimizedImage", optimizedImage.argb)
                if (optimizedImage.argb !== originalImage.argb) {
                    add("originalImage", originalImage.argb)
                }
            },
        )
    }

    private suspend fun writeOrOptimizeGifImage(result: Result<WorkerResult<GifProcessorOutput>>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            if (throwable == null) {
                throwable = error
            }
            return
        }

        val (output, transferables) = result.getOrThrow()
        if (output !is GifProcessorOutput.Quantize) {
            throw createWrongOutputTypeException(output)
        }
        val imageColorIndices = transferables.getByteArray("imageColorIndices")
            ?: throw IllegalStateException("Image color indices are missing")
        val colorTable = transferables.getByteArray("colorTable")
            ?: throw IllegalStateException("Color table is missing")
        val originalImage = transferables.getIntArray("originalImage")
            ?: throw IllegalStateException("Original image data is missing")
        writeOrOptimizeGifImage(
            output.quantizedImageInfo.toData(imageColorIndices, colorTable),
            output.originalImage.toImage(originalImage),
            output.durationCentiseconds,
            output.disposalMethod,
            output.optimizedPreviousFrame,
        )
    }

    private suspend fun writeOrOptimizeGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) {
        baseEncoder.writeOrOptimizeGifImage(
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
            encodeAndWriteImage = { imageData1, durationCentiseconds1, disposalMethod1 ->
                encodeAndWriteImage(imageData1, durationCentiseconds1, disposalMethod1)
            },
        )
    }

    private suspend fun encodeAndWriteImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        encodeExecutor.submit(
            GifProcessorInput.Encode(
                imageData.info,
                durationCentiseconds,
                disposalMethod,
            ) to Transferables {
                add("imageColorIndices", imageData.imageColorIndices)
                add("colorTable", imageData.colorTable)
            },
        )
    }

    private suspend fun transferToSink(result: Result<WorkerResult<GifProcessorOutput>>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            if (throwable == null) {
                throwable = error
            }
            return
        }

        val (output, transferables) = result.getOrThrow()
        if (output !is GifProcessorOutput.Encode) {
            throw createWrongOutputTypeException(output)
        }
        val bytes = transferables.getByteArray("bytes")
            ?: throw IllegalStateException("Byte data is missing")
        sink.write(bytes)
        try {
            writtenFrameNotifications.send(output.durationCentiseconds.centiseconds)
        } catch (t: Throwable) {
            if (throwable == null) {
                throwable = t
            }
        }
    }

    private suspend fun submitToWorkerPool(
        input: Pair<GifProcessorInput, Transferables>,
    ): WorkerResult<GifProcessorOutput> {
        return workerPool.submit(input.first, input.second)
    }

    private fun createException(cause: Throwable): IOException {
        return IOException("Error while writing GIF frame", cause)
    }

    private fun createWrongOutputTypeException(output: GifProcessorOutput): IllegalStateException {
        return IllegalStateException("Wrong output type received: $output")
    }

    override suspend fun close() {
        var closeThrowable: Throwable? = null
        try {
            quantizeExecutor.close()
            baseEncoder.close(
                quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                    writeOrOptimizeGifImage(
                        baseEncoder.getImageData(optimizedImage),
                        originalImage,
                        durationCentiseconds,
                        disposalMethod,
                        optimizedPreviousFrame,
                    )
                },
                encodeAndWriteImage = { imageData, durationCentiseconds, disposalMethod ->
                    encodeAndWriteImage(imageData, durationCentiseconds, disposalMethod)
                },
                afterFinalWrite = {
                    encodeExecutor.close()
                },
            )
            writtenFrameNotifications.close()
            writtenFrameListener.join()
        } catch (t: Throwable) {
            closeThrowable = t
            throw t
        } finally {
            val throwable = throwable
            if (throwable != null) {
                val exception = createException(throwable)
                if (closeThrowable == null) {
                    throw exception
                } else {
                    closeThrowable.addSuppressed(exception)
                }
            }
        }
    }
}
