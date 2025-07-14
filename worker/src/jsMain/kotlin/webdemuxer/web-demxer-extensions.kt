@file:Suppress("unused")

package com.shakster.gifcreator.worker.webdemuxer

import js.reflect.unsafeCast
import web.codecs.AudioDecoderConfig
import web.codecs.EncodedAudioChunk
import web.codecs.EncodedVideoChunk
import web.codecs.VideoDecoderConfig

inline val MediaType.Companion.VIDEO: WebCodecsSupportedMediaType<VideoDecoderConfig, EncodedVideoChunk>
    get() = unsafeCast("video")

inline val MediaType.Companion.AUDIO: WebCodecsSupportedMediaType<AudioDecoderConfig, EncodedAudioChunk>
    get() = unsafeCast("audio")

inline val MediaType.Companion.SUBTITLE: MediaType
    get() = unsafeCast("subtitle")

fun WebDemuxer(wasmFilePath: String): WebDemuxer {
    val options = js("({wasmFilePath: wasmFilePath})")
    return WebDemuxer(options.unsafeCast<WebDemuxerOptions>())
}
