package com.shakster.gifcreator.worker

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class GifFrameWrittenEvent(
    val framesWritten: Int,
    val writtenDuration: Duration,
)
