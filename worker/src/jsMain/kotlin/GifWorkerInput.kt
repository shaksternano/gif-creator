package com.shakster.gifcreator.worker

import com.shakster.gifcreator.util.ColorQuantizerSettings
import com.shakster.gifcreator.util.InputTypeSerializer
import com.shakster.gifcreator.util.Typed
import com.shakster.gifkt.CieLabDistanceCalculator
import com.shakster.gifkt.ColorDistanceCalculator
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

@Serializable(GifWorkerInputSerializer::class)
sealed interface GifWorkerInput : Typed

@Serializable
data class GifEncoderInit(
    val transparencyColorTolerance: Double,
    val quantizedTransparencyColorTolerance: Double,
    val loopCount: Int,
    val maxColors: Int,
    val colorQuantizerSettings: ColorQuantizerSettings,
    val colorDistanceCalculatorSettings: CieLabColorDistanceCalculatorSettings,
    val comment: String,
    val alphaFill: Int,
    val cropTransparent: Boolean,
    val minimumFrameDurationCentiseconds: Int,
) : GifWorkerInput {
    override val type: String = "initEncoder"
}

@Serializable
data class GifFrame(
    val width: Int,
    val height: Int,
    val durationMilliseconds: Long,
) : GifWorkerInput {
    override val type: String = "gifFrame"
}

@Serializable
object GifEncoderClose : GifWorkerInput {
    override val type: String = "gifClose"
}

@Serializable
object WorkerShutdown : GifWorkerInput {
    override val type: String = "shutdown"
}

@Serializable(ColorDistanceCalculatorSettingsSerializer::class)
sealed interface ColorDistanceCalculatorSettings : Typed {
    fun createColorDistanceCalculator(): ColorDistanceCalculator
}

@Serializable
object CieLabColorDistanceCalculatorSettings : ColorDistanceCalculatorSettings {
    override val type: String = "cieLab"

    override fun createColorDistanceCalculator(): ColorDistanceCalculator {
        return CieLabDistanceCalculator
    }
}

private object GifWorkerInputSerializer : InputTypeSerializer<GifWorkerInput>(
    GifWorkerInput::class,
) {
    override val serializers: Map<String, DeserializationStrategy<GifWorkerInput>> = mapOf(
        "initEncoder" to GifEncoderInit.serializer(),
        "gifFrame" to GifFrame.serializer(),
        "gifClose" to GifEncoderClose.serializer(),
        "shutdown" to WorkerShutdown.serializer(),
    )
}

private object ColorDistanceCalculatorSettingsSerializer : InputTypeSerializer<ColorDistanceCalculatorSettings>(
    ColorDistanceCalculatorSettings::class,
) {
    override val serializers: Map<String, DeserializationStrategy<ColorDistanceCalculatorSettings>> = mapOf(
        "cieLab" to CieLabColorDistanceCalculatorSettings.serializer(),
    )
}
