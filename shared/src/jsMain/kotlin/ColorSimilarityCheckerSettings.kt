package com.shakster.gifcreator.shared

import com.shakster.gifkt.ColorSimilarityChecker
import kotlinx.serialization.Serializable

@Serializable
sealed class ColorSimilarityCheckerSettings {

    abstract fun createColorSimilarityChecker(): ColorSimilarityChecker

    @Serializable
    data object CieLab : ColorSimilarityCheckerSettings() {

        override fun createColorSimilarityChecker(): ColorSimilarityChecker {
            return ColorSimilarityChecker.CIELAB
        }
    }

    @Serializable
    data class Euclidean(
        private val redWeight: Double,
        private val greenWeight: Double,
        private val blueWeight: Double,
    ) : ColorSimilarityCheckerSettings() {

        override fun createColorSimilarityChecker(): ColorSimilarityChecker {
            return ColorSimilarityChecker.euclidean(redWeight, greenWeight, blueWeight)
        }
    }
}
