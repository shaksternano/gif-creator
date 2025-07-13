package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.arrayBuffer
import com.shakster.gifkt.ImageFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import web.codecs.*
import kotlin.js.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class Mp4ImageReader(
    private val info: Movie,
    private val samples: Array<Sample>,
    private val description: Uint8Array,
    private val frameDuration: Duration,
) : ImageReader {

    override val frameCount: Int = samples.size

    override fun readFrames(): Flow<ImageFrame> {
        return channelFlow {
            var frameIndex = 0
            val decoderInit = VideoDecoderInit(
                error = { exception ->
                    console.error("VideoDecoder error: ${exception.message.ifBlank { exception }}")
                    close(exception)
                },
                output = { frame ->
                    val argb = frame.readArgb()
                    val image = ImageFrame(
                        argb = argb,
                        width = frame.displayWidth,
                        height = frame.displayHeight,
                        duration = frame.duration?.microseconds ?: frameDuration,
                        timestamp = frame.timestamp.microseconds,
                        index = frameIndex++,
                    )
                    frame.close()
                    trySend(image)
                    if (frameIndex >= frameCount) {
                        close()
                    }
                }
            )
            val decoder = VideoDecoder(decoderInit)

            val videoTrack = info.videoTracks[0]
            val decoderConfig = VideoDecoderConfig(
                codec = videoTrack.codec,
                codedHeight = videoTrack.height,
                codedWidth = videoTrack.width,
                hardwareAcceleration = HardwareAcceleration.preferHardware,
                description = description.asDynamic(),
            )
            decoder.configure(decoderConfig)

            for (sample in samples) {
                val chunkInit = EncodedVideoChunkInit(
                    data = sample.data.asDynamic(),
                    duration = sample.duration * 1000000.0 / sample.timescale,
                    timestamp = sample.cts * 1000000.0 / sample.timescale,
                    type = if (sample.isSync) EncodedVideoChunkType.key else EncodedVideoChunkType.delta,
                )
                val chunk = EncodedVideoChunk(chunkInit)
                decoder.decode(chunk)
            }
            decoder.flush()
            decoder.close()
            awaitClose()
        }.buffer(Channel.UNLIMITED)
    }

    object Factory : ImageReaderFactory {

        override suspend fun create(blob: Blob, frameDuration: Duration): ImageReader {
            val arrayBuffer = blob.arrayBuffer()

            val resultDeferred = CompletableDeferred<LoadResult>()
            val samplesDeferred = CompletableDeferred<SamplesData>()

            val mp4boxInputFile = createFile()

            mp4boxInputFile.onReady = { info ->
                resultDeferred.complete(LoadResult.Ready(info))
                val extractionOptions = json("nbSamples" to Double.POSITIVE_INFINITY)
                mp4boxInputFile.setExtractionOptions(info.videoTracks[0].id, null, extractionOptions.asDynamic())
                // start() doesn't work when called outside this callback for some reason
                mp4boxInputFile.start()
            }

            mp4boxInputFile.onError = { module, message ->
                resultDeferred.complete(LoadResult.Error(module, message))
            }

            mp4boxInputFile.onSamples = { sampleId, user, samples ->
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    val result = resultDeferred.await()
                    if (result is LoadResult.Ready && sampleId == result.info.videoTracks[0].id) {
                        samplesDeferred.complete(SamplesData(sampleId, user, samples))
                    }
                }
            }

            mp4boxInputFile.appendBuffer(MP4BoxBuffer.fromArrayBuffer(arrayBuffer, 0))
            mp4boxInputFile.flush()

            val result = resultDeferred.await()
            val info = when (result) {
                is LoadResult.Ready -> result.info
                is LoadResult.Error -> throw Exception("Error loading MP4 file: ${result.message}")
            }

            val videoTrackId = info.videoTracks[0].id
            val trak = mp4boxInputFile.getTrackById(videoTrackId)
            val description = trak.mdia.minf.stbl.stsd.entries.firstNotNullOf { entry ->
                val box = entry.avcC ?: entry.hvcC
                if (box == null) {
                    null
                } else {
                    val stream = DataStream(null, 0, Endianness.BIG_ENDIAN)
                    box.write(stream)
                    val boxHeaderOffset = 8
                    Uint8Array(stream.buffer, boxHeaderOffset)
                }
            }

            val samplesData = samplesDeferred.await()
            return Mp4ImageReader(info, samplesData.samples, description, frameDuration)
        }

        private sealed interface LoadResult {
            data class Ready(val info: Movie) : LoadResult
            data class Error(val module: String, val message: String) : LoadResult
        }

        private data class SamplesData(
            val sampleId: Int,
            val user: Any?,
            @Suppress("ArrayInDataClass")
            val samples: Array<Sample>,
        )
    }
}
