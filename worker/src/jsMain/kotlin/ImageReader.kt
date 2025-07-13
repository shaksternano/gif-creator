package com.shakster.gifcreator.worker

import com.shakster.gifkt.ImageFrame
import kotlinx.coroutines.flow.Flow
import org.w3c.files.Blob
import kotlin.time.Duration

interface ImageReader {

    val frameCount: Int

    fun readFrames(): Flow<ImageFrame>

    companion object {
        private val imageReaderFactories: List<ImageReaderFactory> = listOf(
            GifImageReader.Factory,
            CanvasImageReader.Factory,
            Mp4ImageReader.Factory,
        )

        suspend fun create(blob: Blob, frameDuration: Duration): ImageReader {
            var throwable: Throwable? = null
            for (factory in imageReaderFactories) {
                try {
                    return factory.create(blob, frameDuration)
                } catch (t: Throwable) {
                    throwable = t
                }
            }
            throw IllegalArgumentException("Unsupported format", throwable)
        }
    }
}

interface ImageReaderFactory {

    suspend fun create(blob: Blob, frameDuration: Duration): ImageReader
}
