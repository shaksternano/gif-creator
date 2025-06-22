package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.WorkerOutput
import kotlinx.serialization.Serializable

@Serializable
sealed class GifWorkerOutput : WorkerOutput {

    @Serializable
    data object Ok : GifWorkerOutput()

    @Serializable
    data class MediaQueryResult(
        val frameCount: Int,
    ) : GifWorkerOutput()

    @Serializable
    data class Error(
        override val message: String,
    ) : GifWorkerOutput() {
        override val isError: Boolean = true
    }
}
