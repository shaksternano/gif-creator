package com.shakster.gifcreator.processor

import com.shakster.gifcreator.shared.WorkerOutput
import com.shakster.gifkt.internal.DisposalMethod
import kotlinx.serialization.Serializable

@Serializable
sealed class GifProcessorOutput : WorkerOutput {

    @Serializable
    data class Quantize(
        val quantizedImageInfo: QuantizedImageInfo,
        val originalImage: ImageDimensions,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    ) : GifProcessorOutput()

    @Serializable
    data class Encode(
        val durationCentiseconds: Int,
    ) : GifProcessorOutput()
}
