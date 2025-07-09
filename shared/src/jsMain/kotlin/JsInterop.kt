package com.shakster.gifcreator.shared

import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import web.canvas.ID
import web.canvas.OffscreenCanvas
import web.canvas.OffscreenCanvasRenderingContext2D
import kotlin.js.Promise

fun OffscreenCanvas.getContext2d(): OffscreenCanvasRenderingContext2D {
    return getContext(OffscreenCanvasRenderingContext2D.ID)!!
}

fun Blob.arrayBuffer(): Promise<ArrayBuffer> {
    return asDynamic().arrayBuffer()
}

fun Number.toFixed(digits: Int = 0): String {
    return if (jsTypeOf(this) == "number") {
        asDynamic().toFixed(digits)
    } else if (this is Long) {
        if (digits == 0) {
            toString()
        } else {
            toString() + "." + "0".repeat(digits)
        }
    } else {
        toDouble().asDynamic().toFixed(digits)
    }
}
