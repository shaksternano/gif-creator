package com.shakster.gifcreator.processor

import com.shakster.gifcreator.util.InputTypeSerializer
import com.shakster.gifcreator.util.Typed
import com.shakster.gifkt.internal.DisposalMethod
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

@Serializable(with = GifProcessorOutputSerializer::class)
sealed interface GifProcessorOutput : Typed

@Serializable
data class QuantizeOutput(
    val quantizedImageInfo: QuantizedImageInfo,
    val originalImage: ImageDimensions,
    val durationCentiseconds: Int,
    val disposalMethod: DisposalMethod,
    val optimizedPreviousFrame: Boolean,
) : GifProcessorOutput {
    override val type: String = "quantizeOutput"
}

@Serializable
data class EncodeOutput(
    val durationCentiseconds: Int,
) : GifProcessorOutput {
    override val type: String = "encodeOutput"
}

private object GifProcessorOutputSerializer : InputTypeSerializer<GifProcessorOutput>(
    GifProcessorOutput::class,
) {
    override val serializers: Map<String, DeserializationStrategy<GifProcessorOutput>> = mapOf(
        "quantizeOutput" to QuantizeOutput.serializer(),
        "encodeOutput" to EncodeOutput.serializer(),
    )
}
