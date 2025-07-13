@file:JsModule("mp4box")
@file:JsNonModule
@file:Suppress("unused")

package com.shakster.gifcreator.worker

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

sealed external interface Endianness {
    companion object {
        val BIG_ENDIAN: Endianness
        val LITTLE_ENDIAN: Endianness
    }
}

open external class DataStream(
    arrayBuffer: ArrayBuffer? = definedExternally,
    byteOffset: Int = definedExternally,
    endianness: Endianness = definedExternally,
) {

    val buffer: MP4BoxBuffer
}

open external class MultiBufferStream : DataStream

open external class MP4BoxBuffer(
    byteLength: Int,
) : ArrayBuffer {

    val fileStart: Int
    val usedBytes: Int?

    companion object {
        fun fromArrayBuffer(buffer: ArrayBuffer, fileStart: Int): MP4BoxBuffer
    }
}

external interface Track {

    val id: Int
    val codec: String
    @JsName("track_width")
    val width: Int
    @JsName("track_height")
    val height: Int
}

external interface Movie {

    val audioTracks: Array<Track>
    val videoTracks: Array<Track>
}

external interface Sample {

    val cts: Int
    val data: Uint8Array
    val duration: Int
    @JsName("is_sync")
    val isSync: Boolean
    val timescale: Int
}

open external class Box {

    open fun write(stream: DataStream)
}

open external class FullBox : Box

open external class ContainerBox : Box

@JsName("avcCBox")
open external class AvcCBox : Box {

    override fun write(stream: DataStream)
}

@JsName("hvcCBox")
open external class HvcCBox : Box {

    override fun write(stream: DataStream)
}

open external class SampleEntry : ContainerBox {

    val avcC: AvcCBox?

    val hvcC: HvcCBox?
}

@JsName("stsdBox")
open external class StsdBox : FullBox {

    val entries: Array<SampleEntry>
}

@JsName("stblBox")
open external class StblBox : ContainerBox {

    val stsd: StsdBox
}

@JsName("minfBox")
open external class MinfBox : ContainerBox {

    val stbl: StblBox
}

@JsName("mdiaBox")
open external class MdiaBox : ContainerBox {

    val minf: MinfBox
}

@JsName("trakBox")
open external class TrakBox : ContainerBox {

    val mdia: MdiaBox
}

external interface ExtractionOptions {

    var nbSamples: Double?
}

open external class ISOFile {

    var onReady: ((info: Movie) -> Unit)?

    var onError: ((module: String, message: String) -> Unit)?

    var onSamples: ((id: Int, user: Any?, samples: Array<Sample>) -> Unit)?

    fun setExtractionOptions(
        id: Int,
        user: Any? = definedExternally,
        options: ExtractionOptions = definedExternally,
    )

    fun appendBuffer(
        ab: MP4BoxBuffer,
        last: Boolean = definedExternally,
    ): Int

    fun start()

    fun flush()

    fun getTrackById(id: Int): TrakBox
}

external fun createFile(
    keepMdatData: Boolean = definedExternally,
    stream: MultiBufferStream = definedExternally,
): ISOFile
