package com.shakster.gifcreator.util

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.NeuQuantizer
import kotlinx.serialization.Serializable

@Serializable
sealed class ColorQuantizerSettings {

    abstract fun createQuantizer(): ColorQuantizer

    @Serializable
    data class NeuQuant(
        val quality: Int,
    ) : ColorQuantizerSettings() {

        override fun createQuantizer(): ColorQuantizer {
            return NeuQuantizer(quality)
        }
    }
}
