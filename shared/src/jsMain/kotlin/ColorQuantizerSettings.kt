package com.shakster.gifcreator.shared

import com.shakster.gifkt.ColorQuantizer
import kotlinx.serialization.Serializable

@Serializable
sealed class ColorQuantizerSettings {

    abstract fun createQuantizer(): ColorQuantizer

    @Serializable
    data class NeuQuant(
        private val quality: Int,
    ) : ColorQuantizerSettings() {

        override fun createQuantizer(): ColorQuantizer {
            return ColorQuantizer.neuQuant(quality)
        }
    }
}
