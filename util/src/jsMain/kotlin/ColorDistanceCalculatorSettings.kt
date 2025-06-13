package com.shakster.gifcreator.util

import com.shakster.gifkt.CieLabDistanceCalculator
import com.shakster.gifkt.ColorDistanceCalculator
import kotlinx.serialization.Serializable

@Serializable
sealed class ColorDistanceCalculatorSettings {

    abstract fun createColorDistanceCalculator(): ColorDistanceCalculator

    @Serializable
    object CieLab : ColorDistanceCalculatorSettings() {
        override fun createColorDistanceCalculator(): ColorDistanceCalculator {
            return CieLabDistanceCalculator
        }
    }
}
