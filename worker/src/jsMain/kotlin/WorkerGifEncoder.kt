package com.shakster.gifcreator.worker

import com.shakster.gifcreator.processor.*
import com.shakster.gifcreator.util.*
import com.shakster.gifkt.SuspendClosable
import com.shakster.gifkt.internal.*
import com.varabyte.kobweb.worker.Transferables
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    ) -> Unit,
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

    private val quantizeExecutor: AsyncExecutor<Pair<QuantizeInput, Transferables>, Pair<GifProcessorOutput, Transferables>> =
        AsyncExecutor(
            maxConcurrency,
            task = ::submitToWorkerPool,
            onOutput = ::writeOrOptimizeGifImage,
        )

    private val encodeExecutor: AsyncExecutor<Pair<EncodeInput, Transferables>, Pair<GifProcessorOutput, Transferables>> =
        AsyncExecutor(
            maxConcurrency,
            task = ::submitToWorkerPool,
            onOutput = ::transferToSink,
        )

    private var framesWritten: Int = 0
    private var writtenDuration: Duration = Duration.ZERO

    suspend fun writeFrame(input: GifFrame, transferables: Transferables) {
        val throwable = throwable
        if (throwable != null) {
            throw createException(throwable)
        }

        val image = transferables.getInt32Array("image")
            ?: throw IllegalStateException("Image data is missing")

        val written = baseEncoder.writeFrame(
            image.asIntArray(),
            input.width,
            input.height,
            input.durationMilliseconds.milliseconds,
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
            encodedFrame(Duration.ZERO)
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
            QuantizeInput(
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
                add("originalImage", originalImage.argb)
            },
        )
    }

    private suspend fun writeOrOptimizeGifImage(result: Result<Pair<GifProcessorOutput, Transferables>>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            if (throwable == null) {
                throwable = error
            }
            return
        }

        val (output, transferables) = result.getOrThrow()
        if (output !is QuantizeOutput) {
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
            EncodeInput(
                imageData.info,
                durationCentiseconds,
                disposalMethod,
            ) to Transferables {
                add("imageColorIndices", imageData.imageColorIndices)
                add("colorTable", imageData.colorTable)
            },
        )
    }

    private suspend fun transferToSink(result: Result<Pair<GifProcessorOutput, Transferables>>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            if (throwable == null) {
                throwable = error
            }
            return
        }

        val (output, transferables) = result.getOrThrow()
        if (output !is EncodeOutput) {
            throw createWrongOutputTypeException(output)
        }
        val bytes = transferables.getByteArray("bytes")
            ?: throw IllegalStateException("Byte data is missing")
        sink.write(bytes)
        encodedFrame(output.durationCentiseconds.centiseconds)
    }

    private suspend fun submitToWorkerPool(
        input: Pair<GifProcessorInput, Transferables>,
    ): Pair<GifProcessorOutput, Transferables> {
        return workerPool.submit(input.first, input.second)
    }

    private suspend fun encodedFrame(duration: Duration) {
        framesWritten++
        writtenDuration += duration
        onFrameWritten(framesWritten, writtenDuration)
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
