package com.shakster.gifcreator.shared

import com.varabyte.kobweb.worker.Attachments
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array

fun IntArray.asInt32Array(): Int32Array {
    return unsafeCast<Int32Array>()
}

fun Int32Array.asIntArray(): IntArray {
    return unsafeCast<IntArray>()
}

fun ByteArray.asInt8Array(): Int8Array {
    return unsafeCast<Int8Array>()
}

fun Int8Array.asByteArray(): ByteArray {
    return unsafeCast<ByteArray>()
}

fun Attachments.Builder.add(name: String, value: IntArray) {
    add(name, value.asInt32Array())
}

fun Attachments.Builder.add(name: String, value: ByteArray) {
    add(name, value.asInt8Array())
}

fun Attachments.getIntArray(name: String): IntArray? {
    return getInt32Array(name)?.asIntArray()
}

fun Attachments.getByteArray(name: String): ByteArray? {
    return getInt8Array(name)?.asByteArray()
}
