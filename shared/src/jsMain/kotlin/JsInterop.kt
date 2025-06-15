package com.shakster.gifcreator.shared

import org.w3c.dom.*
import org.w3c.dom.events.EventTarget

/**
 * Exposes the JavaScript [OffscreenCanvas](https://developer.mozilla.org/en-US/docs/Web/API/OffscreenCanvas) to Kotlin
 */
@Suppress("unused")
open external class OffscreenCanvas(
    open var width: Int,
    open var height: Int,
) : EventTarget {

    fun getContext(contextId: String, options: Any = definedExternally): OffscreenRenderingContext?
}

fun OffscreenCanvas.getContext2d(): OffscreenCanvasRenderingContext2D {
    return getContext("2d") as OffscreenCanvasRenderingContext2D
}

external interface OffscreenRenderingContext

/**
 * Exposes the JavaScript [OffscreenCanvasRenderingContext2D](https://developer.mozilla.org/en-US/docs/Web/API/OffscreenCanvasRenderingContext2D) to Kotlin
 */
@Suppress("unused")
abstract external class OffscreenCanvasRenderingContext2D : CanvasCompositing, CanvasDrawImage, CanvasDrawPath,
    CanvasFillStrokeStyles, CanvasFilters, CanvasImageData, CanvasImageSmoothing, CanvasPath, CanvasPathDrawingStyles,
    CanvasRect, CanvasShadowStyles, CanvasState, CanvasText, CanvasTextDrawingStyles, CanvasTransform,
    OffscreenRenderingContext {

    val canvas: OffscreenCanvas
}

