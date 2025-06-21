package com.shakster.gifcreator.processor

import com.shakster.gifkt.Image
import kotlinx.serialization.Serializable

@Serializable
data class ImageDimensions(
    val width: Int,
    val height: Int,
) {

    fun toImage(argb: IntArray): Image {
        return Image(argb, width, height)
    }
}

val Image.dimensions: ImageDimensions
    get() = ImageDimensions(width, height)
