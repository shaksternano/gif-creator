package com.shakster.gifcreator.util

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.NeuQuantizer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

@Serializable(ColorQuantizerSettingsSerializer::class)
sealed interface ColorQuantizerSettings : Typed {
    fun createQuantizer(): ColorQuantizer
}

@Serializable
data class NeuQuantizerSettings(
    val quality: Int,
) : ColorQuantizerSettings {
    override val type: String = "neuQuantizer"

    override fun createQuantizer(): ColorQuantizer {
        return NeuQuantizer(quality)
    }
}

private object ColorQuantizerSettingsSerializer : InputTypeSerializer<ColorQuantizerSettings>(
    ColorQuantizerSettings::class,
) {
    override val serializers: Map<String, DeserializationStrategy<ColorQuantizerSettings>> = mapOf(
        "neuQuantizer" to NeuQuantizerSettings.serializer(),
    )
}
