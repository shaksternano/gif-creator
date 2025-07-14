package com.shakster.gifcreator.worker

import com.shakster.gifcreator.worker.webdemuxer.MediaType
import com.shakster.gifcreator.worker.webdemuxer.VIDEO
import com.shakster.gifcreator.worker.webdemuxer.WebDemuxer
import com.shakster.gifkt.ImageFrame
import js.iterable.iterator
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import org.w3c.files.File
import web.codecs.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class VideoImageReader(
    override val frameCount: Int,
    private val demuxer: WebDemuxer,
    private val frameDuration: Duration,
) : ImageReader {

    override val isVideo: Boolean = true

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
            val decoderConfig = demuxer.getDecoderConfig(MediaType.VIDEO).await()
            decoderConfig.hardwareAcceleration = HardwareAcceleration.preferHardware
            decoder.configure(decoderConfig)

            val frames = demuxer.read(MediaType.VIDEO)
            for (frame in frames) {
                decoder.decode(frame)
            }

            decoder.flush()
            decoder.close()
            demuxer.destroy()
            awaitClose()
        }.buffer(Channel.UNLIMITED)
    }

    object Factory : ImageReaderFactory {

        override suspend fun create(file: File, frameDuration: Duration): ImageReader {
            val demuxer = WebDemuxer("https://cdn.jsdelivr.net/npm/web-demuxer@latest/dist/wasm-files/web-demuxer.wasm")
            demuxer.load(file).await()
            val stream = demuxer.getMediaStream(MediaType.VIDEO).await()
            val frameCount = stream.nbFrames.toInt()
            return VideoImageReader(
                frameCount,
                demuxer,
                frameDuration,
            )
        }
    }
}
