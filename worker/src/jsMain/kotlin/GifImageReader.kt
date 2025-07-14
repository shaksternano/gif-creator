package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.arrayBuffer
import com.shakster.gifkt.GifDecoder
import com.shakster.gifkt.ImageFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.w3c.files.File
import kotlin.time.Duration

class GifImageReader(
    private val decoder: GifDecoder,
) : ImageReader {

    override val frameCount: Int = decoder.frameCount

    override fun readFrames(): Flow<ImageFrame> {
        return decoder.asSequence().asFlow()
    }

    object Factory : ImageReaderFactory {

        override suspend fun create(file: File, frameDuration: Duration): GifImageReader {
            val decoder = GifDecoder(file.arrayBuffer(), 0)
            return GifImageReader(decoder)
        }
    }
}
