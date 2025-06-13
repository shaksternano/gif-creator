package com.shakster.gifcreator.worker

import com.shakster.gifcreator.util.ColorDistanceCalculatorSettings
import com.shakster.gifcreator.util.ColorQuantizerSettings
import kotlinx.serialization.Serializable

@Serializable
sealed class GifWorkerInput {

    @Serializable
    data class EncoderInit(
        val transparencyColorTolerance: Double,
        val quantizedTransparencyColorTolerance: Double,
        val loopCount: Int,
        val maxColors: Int,
        val colorQuantizerSettings: ColorQuantizerSettings,
        val colorDistanceCalculatorSettings: ColorDistanceCalculatorSettings.CieLab,
        val comment: String,
        val alphaFill: Int,
        val cropTransparent: Boolean,
        val minimumFrameDurationCentiseconds: Int,
    ) : GifWorkerInput()

    @Serializable
    data class Frame(
        val width: Int,
        val height: Int,
        val durationMilliseconds: Long,
    ) : GifWorkerInput()

    @Serializable
    data object EncoderClose : GifWorkerInput()

    @Serializable
    data object Shutdown : GifWorkerInput()
}
