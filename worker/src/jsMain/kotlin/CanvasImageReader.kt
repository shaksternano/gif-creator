package com.shakster.gifcreator.worker

import com.shakster.gifkt.ImageFrame
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.ImageBitmap
import org.w3c.files.Blob
import kotlin.time.Duration

private external val self: DedicatedWorkerGlobalScope

class CanvasImageReader(
    private val image: ImageBitmap,
    private val frameDuration: Duration,
) : ImageReader {

    override val frameCount: Int = 1

    override fun readFrames(): Flow<ImageFrame> {
        return flow {
            val argb = image.readArgb()
            val image = ImageFrame(
                argb = argb,
                width = image.width,
                height = image.height,
                duration = frameDuration,
                timestamp = Duration.ZERO,
                index = 0,
            )
            emit(image)
        }
    }

    object Factory : ImageReaderFactory {
        override suspend fun create(blob: Blob, frameDuration: Duration): ImageReader {
            val image = self.createImageBitmap(blob).await()
            return CanvasImageReader(image, frameDuration)
        }
    }
}
