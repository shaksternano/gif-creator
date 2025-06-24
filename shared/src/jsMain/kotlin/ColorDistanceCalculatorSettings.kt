package com.shakster.gifcreator.shared

import com.shakster.gifkt.CieLabDistanceCalculator
import com.shakster.gifkt.ColorDistanceCalculator
import com.shakster.gifkt.EuclideanDistanceCalculator
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

    @Serializable
    data class Euclidean(
        private val redWeight: Double,
        private val greenWeight: Double,
        private val blueWeight: Double,
    ) : ColorDistanceCalculatorSettings() {

        override fun createColorDistanceCalculator(): ColorDistanceCalculator {
            return EuclideanDistanceCalculator(redWeight, greenWeight, blueWeight)
        }
    }
}
