package com.shakster.gifcreator.processor

import com.shakster.gifcreator.shared.ColorQuantizerSettings
import com.shakster.gifkt.DisposalMethod
import kotlinx.serialization.Serializable

@Serializable
sealed class GifProcessorInput {

    @Serializable
    data class Quantize(
        val maxColors: Int,
        val colorQuantizerSettings: ColorQuantizerSettings,
        val optimizeQuantizedTransparency: Boolean,
        val optimizedImage: ImageDimensions,
        val originalImage: ImageDimensions,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    ) : GifProcessorInput()

    @Serializable
    data class Encode(
        val quantizedImageInfo: QuantizedImageInfo,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    ) : GifProcessorInput()
}
