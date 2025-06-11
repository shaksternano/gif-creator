package com.shakster.gifcreator.processor

import com.shakster.gifcreator.util.ColorQuantizerSettings
import com.shakster.gifcreator.util.InputTypeSerializer
import com.shakster.gifcreator.util.Typed
import com.shakster.gifkt.internal.DisposalMethod
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

@Serializable(with = GifProcessorInputSerializer::class)
sealed interface GifProcessorInput : Typed

@Serializable
data class QuantizeInput(
    val maxColors: Int,
    val colorQuantizerSettings: ColorQuantizerSettings,
    val optimizeQuantizedTransparency: Boolean,
    val optimizedImage: ImageDimensions,
    val originalImage: ImageDimensions,
    val durationCentiseconds: Int,
    val disposalMethod: DisposalMethod,
    val optimizedPreviousFrame: Boolean,
) : GifProcessorInput {
    override val type: String = "quantize"
}

@Serializable
data class EncodeInput(
    val quantizedImageInfo: QuantizedImageInfo,
    val durationCentiseconds: Int,
    val disposalMethod: DisposalMethod,
) : GifProcessorInput {
    override val type: String = "encode"
}

private object GifProcessorInputSerializer : InputTypeSerializer<GifProcessorInput>(
    GifProcessorInput::class,
) {
    override val serializers: Map<String, DeserializationStrategy<GifProcessorInput>> = mapOf(
        "quantize" to QuantizeInput.serializer(),
        "encode" to EncodeInput.serializer(),
    )
}
