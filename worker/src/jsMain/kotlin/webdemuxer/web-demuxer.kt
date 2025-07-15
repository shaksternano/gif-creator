@file:JsModule("web-demuxer")
@file:JsNonModule
@file:Suppress("unused")

package com.shakster.gifcreator.worker.webdemuxer

import js.objects.Record
import org.khronos.webgl.Uint8Array
import org.w3c.files.File
import web.streams.ReadableStream
import kotlin.js.Promise

sealed external interface AVLogLevel {

    companion object {

        /**
         * Print no output.
         */
        val AV_LOG_QUIET: AVLogLevel

        /**
         * Something went really wrong and we will crash now.
         */
        val AV_LOG_PANIC: AVLogLevel

        /**
         * Something went wrong and recovery is not possible.
         * For example, no header was found for a format which depends
         * on headers or an illegal combination of parameters is used.
         */
        val AV_LOG_FATAL: AVLogLevel

        /**
         * Something went wrong and cannot losslessly be recovered.
         * However, not all future data is affected.
         */
        val AV_LOG_ERROR: AVLogLevel

        /**
         * Something somehow does not look correct. This may or may not
         * lead to problems. An example would be the use of '-vstrict -2'.
         */
        val AV_LOG_WARNING: AVLogLevel

        /**
         * Standard information.
         */
        val AV_LOG_INFO: AVLogLevel

        /**
         * Detailed information.
         */
        val AV_LOG_VERBOSE: AVLogLevel

        /**
         * Stuff which is only useful for libav* developers.
         */
        val AV_LOG_DEBUG: AVLogLevel

        /**
         * Extremely verbose debugging, useful for libav* development.
         */
        val AV_LOG_TRACE: AVLogLevel
    }
}

/**
 * sync with ffmpeg libavutil/avutil.h
 */
sealed external interface AVMediaType {

    companion object {

        val AVMEDIA_TYPE_UNKNOWN: AVMediaType

        val AVMEDIA_TYPE_VIDEO: AVMediaType

        val AVMEDIA_TYPE_AUDIO: AVMediaType

        val AVMEDIA_TYPE_DATA: AVMediaType

        val AVMEDIA_TYPE_SUBTITLE: AVMediaType

        val AVMEDIA_TYPE_ATTACHMENT: AVMediaType

        val AVMEDIA_TYPE_NB: AVMediaType
    }
}

sealed external interface AVSeekFlag {

    companion object {

        /**
         * seek backward
         */
        val AVSEEK_FLAG_BACKWARD: AVSeekFlag

        /**
         * seeking based on position in bytes
         */
        val AVSEEK_FLAG_BYTE: AVSeekFlag

        /**
         * seek to any frame, even non-keyframes
         */
        val AVSEEK_FLAG_ANY: AVSeekFlag

        /**
         * seeking based on frame number
         */
        val AVSEEK_FLAG_FRAME: AVSeekFlag
    }
}

external interface WebAVPacket {

    val keyframe: Int
    val timestamp: Int
    val duration: Int
    val size: Int
    val data: Uint8Array
}

external interface WebAVStream {

    val index: Int
    val id: Int

    @JsName("codec_type")
    val codecType: AVMediaType

    @JsName("codec_type_string")
    val codecTypeString: String

    @JsName("codec_name")
    val codecName: String

    @JsName("codec_string")
    val codecString: String

    @JsName("color_primaries")
    val colorPrimaries: String

    @JsName("color_range")
    val colorRange: String

    @JsName("color_space")
    val colorSpace: String

    @JsName("color_transfer")
    val colorTransfer: String
    val profile: String

    @JsName("pix_fmt")
    val pixFmt: String
    val level: Int
    val width: Int
    val height: Int
    val channels: Int

    @JsName("sample_rate")
    val sampleRate: Int

    @JsName("sample_fmt")
    val sampleFmt: String

    @JsName("bit_rate")
    val bitRate: String

    @JsName("extradata_size")
    val extraDataSize: Int

    @JsName("extradata")
    val extraData: Uint8Array

    @JsName("r_frame_rate")
    val rFrameRate: String

    @JsName("avg_frame_rate")
    val avgFrameRate: String

    @JsName("sample_aspect_ratio")
    val sampleAspectRatio: String

    @JsName("display_aspect_ratio")
    val displayAspectRatio: String

    @JsName("start_time")
    val startTime: Int
    val duration: Int
    val rotation: Int

    @JsName("nb_frames")
    val nbFrames: String
    val tags: Record<String, String>
}

external interface WebMediaInfo {

    @JsName("format_name")
    val formatName: String

    @JsName("start_time")
    val startTime: Int
    val duration: Int

    @JsName("bit_rate")
    val bitRate: String

    @JsName("nb_streams")
    val nbStreams: Int

    @JsName("nb_chapters")
    val nbChapters: Int
    val flags: Int
    val streams: Array<WebAVStream>
}

sealed external interface MediaType {

    companion object
}

sealed external interface WebCodecsSupportedMediaType<Config, Chunk> : MediaType

external interface WebDemuxerOptions {

    val wasmFilePath: String?
}

/**
 * WebDemuxer
 *
 * A class to demux media files in the browser using WebAssembly.
 *
 * ```kotlin
 * val demuxer = WebDemuxer()
 * demuxer.load(file).await()
 * val encodedChunk = demuxer.seek(WebCodecsSupportedMediaType.VIDEO, 10).await()
 * ```
 */
external class WebDemuxer(
    options: WebDemuxerOptions = definedExternally,
) {

    /**
     * A [File] or [String]
     */
    val source: Any?

    /**
     * Load a file for demuxing
     *
     * @param source source to load
     */
    fun load(source: File): Promise<Unit>

    /**
     * Load a file for demuxing
     *
     * @param source source to load
     */
    fun load(source: String): Promise<Unit>

    /**
     * Destroy the demuxer instance
     * terminate the worker
     */
    fun destroy()

    /**
     * Get file media info
     */
    fun getMediaInfo(): Promise<WebMediaInfo>

    /**
     * Gets information about a specified stream in the media file.
     *
     * @param streamType The type of media stream
     * @param streamIndex The index of the media stream
     */
    fun getAVStream(
        streamType: AVMediaType = definedExternally,
        streamIndex: Int = definedExternally,
    ): Promise<WebAVStream>

    /**
     * Get all streams
     */
    fun getAVStreams(): Promise<Array<WebAVStream>>

    /**
     * Gets the data at a specified time point in the media file.
     *
     * @param time time in seconds
     * @param streamType The type of media stream
     * @param streamIndex The index of the media stream
     * @param seekFlag The seek flag
     */
    fun getAVPacket(
        time: Int,
        streamType: AVMediaType = definedExternally,
        streamIndex: Int = definedExternally,
        seekFlag: AVSeekFlag = definedExternally,
    ): Promise<WebAVPacket>

    /**
     * Get all packets at a time point from all streams
     *
     * @param time time in seconds
     * @param seekFlag The seek flag
     */
    fun getAVPackets(
        time: Int,
        seekFlag: AVSeekFlag = definedExternally,
    ): Promise<Array<WebAVPacket>>

    /**
     * Returns a [ReadableStream] for streaming packet data.
     *
     * @param start start time in seconds
     * @param end end time in seconds
     * @param streamType The type of media stream
     * @param streamIndex The index of the media stream
     * @param seekFlag The seek flag
     */
    fun readAVPacket(
        start: Int = definedExternally,
        end: Int = definedExternally,
        streamType: AVMediaType = definedExternally,
        streamIndex: Int = definedExternally,
        seekFlag: AVSeekFlag = definedExternally,
    ): ReadableStream<WebAVPacket>

    /**
     * Set log level
     *
     * @param level log level
     */
    fun setLogLevel(level: AVLogLevel): Promise<Unit>

    /**
     * Get media stream (video, audio or subtitle)
     *
     * @param type The type of media stream
     * @param streamIndex The index of the media stream
     */
    fun getMediaStream(
        type: MediaType,
        streamIndex: Int = definedExternally,
    ): Promise<WebAVStream>

    /**
     * Seek media packet at a time point
     *
     * @param type The type of media
     * @param time seek time in seconds
     * @param seekFlag The seek flag
     */
    fun seekMediaPacket(
        type: MediaType,
        time: Int,
        seekFlag: AVSeekFlag = definedExternally,
    ): Promise<WebAVPacket>

    /**
     * Read media packet as a stream
     *
     * @param type The type of media
     * @param start start time in seconds
     * @param end end time in seconds
     * @param seekFlag The seek flag
     */
    fun readMediaPacket(
        type: MediaType,
        start: Int = definedExternally,
        end: Int = definedExternally,
        seekFlag: AVSeekFlag = definedExternally,
    ): ReadableStream<WebAVPacket>

    /**
     * Generate decoder config for video or audio
     *
     * @param type The type of media
     * @param avStream WebAVStream
     */
    fun <Config, T : WebCodecsSupportedMediaType<Config, *>> genDecoderConfig(
        type: T,
        avStream: WebAVStream,
    ): Config

    /**
     * Generate encoded chunk for video or audio
     *
     * @param type The type of media
     * @param avPacket WebAVPacket
     */
    fun <Chunk, T : WebCodecsSupportedMediaType<*, Chunk>> genEncodedChunk(
        type: T,
        avPacket: WebAVPacket,
    ): Chunk

    /**
     * Get decoder config for WebCodecs
     *
     * @param type The type of media ('video' or 'audio')
     */
    fun <Config, T : WebCodecsSupportedMediaType<Config, *>> getDecoderConfig(
        type: T,
    ): Promise<Config>

    /**
     * Seek and return encoded chunk for WebCodecs
     *
     * @param type The type of media
     * @param time time in seconds
     * @param seekFlag The seek flag
     */
    fun <Chunk, T : WebCodecsSupportedMediaType<*, Chunk>> seek(
        type: T,
        time: Int,
        seekFlag: AVSeekFlag = definedExternally,
    ): Promise<Chunk>

    /**
     * Read encoded chunks as a stream for WebCodecs
     *
     * @param type The type of media
     * @param start start time in seconds
     * @param end end time in seconds
     * @param seekFlag The seek flag
     */
    fun <Chunk, T : WebCodecsSupportedMediaType<*, Chunk>> read(
        type: T,
        start: Int = definedExternally,
        end: Int = definedExternally,
        seekFlag: AVSeekFlag = definedExternally,
    ): ReadableStream<Chunk>
}
