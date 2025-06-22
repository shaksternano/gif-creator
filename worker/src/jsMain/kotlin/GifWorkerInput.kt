package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.ColorDistanceCalculatorSettings
import com.shakster.gifcreator.shared.ColorQuantizerSettings
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed class GifWorkerInput {

    @Serializable
    data object MediaQuery : GifWorkerInput()

    @Serializable
    data object MessagePort : GifWorkerInput()

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
        val duration: Duration,
    ) : GifWorkerInput()

    @Serializable
    data class Frames(
        val duration: Duration,
    ) : GifWorkerInput()

    @Serializable
    data object EncoderClose : GifWorkerInput()

    @Serializable
    data object Shutdown : GifWorkerInput()
}
