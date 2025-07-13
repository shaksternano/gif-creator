package com.shakster.gifcreator.worker

import org.w3c.dom.CanvasImageSource
import org.w3c.dom.ImageBitmap
import web.canvas.ID
import web.canvas.OffscreenCanvas
import web.canvas.OffscreenCanvasRenderingContext2D

fun CanvasImageSource.readArgb(width: Int, height: Int): IntArray {
    val canvas = OffscreenCanvas(width.toDouble(), height.toDouble())
    val context = canvas.getContext(OffscreenCanvasRenderingContext2D.ID)!!
    context.drawImage(asDynamic(), 0.0, 0.0)
    val rgba = context.getImageData(
        0,
        0,
        width,
        height,
    ).data
    return IntArray(rgba.length / 4) { i ->
        val index = i * 4
        val red = rgba[index].toInt() and 0xFF
        val green = rgba[index + 1].toInt() and 0xFF
        val blue = rgba[index + 2].toInt() and 0xFF
        val alpha = rgba[index + 3].toInt() and 0xFF
        (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}

fun web.canvas.CanvasImageSource.readArgb(width: Int, height: Int): IntArray {
    return unsafeCast<CanvasImageSource>().readArgb(width, height)
}

fun ImageBitmap.readArgb(): IntArray {
    return readArgb(width, height)
}
