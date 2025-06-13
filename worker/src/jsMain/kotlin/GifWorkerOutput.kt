package com.shakster.gifcreator.worker

import kotlinx.serialization.Serializable

sealed class GifWorkerOutput {

    @Serializable
    data object Ok : GifWorkerOutput()

    @Serializable
    data class Error(
        val message: String,
    ) : GifWorkerOutput()

    @Serializable
    data class EncodedFrame(
        val framesWritten: Int,
        val writtenDurationMilliseconds: Long,
    ) : GifWorkerOutput()
}
