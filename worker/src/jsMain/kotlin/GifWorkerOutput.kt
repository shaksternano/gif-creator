package com.shakster.gifcreator.worker

import com.shakster.gifcreator.util.InputTypeSerializer
import com.shakster.gifcreator.util.Typed
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

@Serializable(GifWorkerOutputSerializer::class)
sealed interface GifWorkerOutput : Typed

@Serializable
object OkOutput : GifWorkerOutput {
    override val type: String = "ok"
}

@Serializable
data class ErrorOutput(
    val message: String,
) : GifWorkerOutput {
    override val type: String = "error"
}

@Serializable
data class EncodedFrameOutput(
    val framesWritten: Int,
    val writtenDurationMilliseconds: Long,
) : GifWorkerOutput {
    override val type: String = "encodedFrame"
}

private object GifWorkerOutputSerializer : InputTypeSerializer<GifWorkerOutput>(
    GifWorkerOutput::class,
) {
    override val serializers: Map<String, DeserializationStrategy<GifWorkerOutput>> = mapOf(
        "ok" to OkOutput.serializer(),
        "error" to ErrorOutput.serializer(),
        "encodedFrame" to EncodedFrameOutput.serializer(),
    )
}
