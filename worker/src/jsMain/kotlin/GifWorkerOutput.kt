package com.shakster.gifcreator.worker

import kotlinx.serialization.Serializable

@Serializable
sealed class GifWorkerOutput {

    @Serializable
    data object Ok : GifWorkerOutput()

    @Serializable
    data class Error(
        val message: String,
    ) : GifWorkerOutput()
}
