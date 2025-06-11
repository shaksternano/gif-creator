package com.shakster.gifcreator.processor

import com.shakster.gifkt.internal.QuantizedImageData
import kotlinx.serialization.Serializable

@Serializable
data class QuantizedImageInfo(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val transparentColorIndex: Int,
) {
    fun toData(imageColorIndices: ByteArray, colorTable: ByteArray): QuantizedImageData {
        return QuantizedImageData(
            imageColorIndices,
            width,
            height,
            x,
            y,
            colorTable,
            transparentColorIndex,
        )
    }
}

val QuantizedImageData.info: QuantizedImageInfo
    get() = QuantizedImageInfo(
        width,
        height,
        x,
        y,
        transparentColorIndex,
    )
