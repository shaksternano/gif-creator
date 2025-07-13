package com.shakster.gifcreator.shared

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import kotlin.js.Promise

suspend fun Blob.arrayBuffer(): ArrayBuffer {
    return asDynamic()
        .arrayBuffer()
        .unsafeCast<Promise<ArrayBuffer>>()
        .await()
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
