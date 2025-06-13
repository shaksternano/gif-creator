package com.shakster.gifcreator.shared

import com.shakster.gifkt.CieLabDistanceCalculator
import com.shakster.gifkt.ColorDistanceCalculator
import kotlinx.serialization.Serializable

@Serializable
sealed class ColorDistanceCalculatorSettings {

    abstract fun createColorDistanceCalculator(): ColorDistanceCalculator

    @Serializable
    data object CieLab : ColorDistanceCalculatorSettings() {

        override fun createColorDistanceCalculator(): ColorDistanceCalculator {
            return CieLabDistanceCalculator
        }
    }
}
