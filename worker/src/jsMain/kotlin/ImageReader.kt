package com.shakster.gifcreator.worker

import com.shakster.gifkt.ImageFrame
import kotlinx.coroutines.flow.Flow
import org.w3c.files.File
import kotlin.time.Duration

interface ImageReader {

    val frameCount: Int
    val isVideo: Boolean
        get() = false

    fun readFrames(): Flow<ImageFrame>

    companion object {
        private val imageReaderFactories: List<ImageReaderFactory> = listOf(
            GifImageReader.Factory,
            CanvasImageReader.Factory,
            VideoImageReader.Factory,
        )

        suspend fun create(file: File, frameDuration: Duration): ImageReader {
            var throwable: Throwable? = null
            for (factory in imageReaderFactories) {
                try {
                    return factory.create(file, frameDuration)
                } catch (t: Throwable) {
                    throwable = t
                }
            }
            throw IllegalArgumentException("Unsupported format", throwable)
        }
    }
}

interface ImageReaderFactory {

    suspend fun create(file: File, frameDuration: Duration): ImageReader
}
