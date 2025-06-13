package com.shakster.gifcreator.shared

import com.varabyte.kobweb.worker.Transferables
import com.varabyte.kobweb.worker.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class WorkerResult<O>(
    val output: O,
    val transferables: Transferables,
)

suspend fun <I, O> Worker<I, O>.submit(
    input: I,
    transferables: Transferables = Transferables.Empty,
): WorkerResult<O> = suspendCoroutine { continuation ->
    onOutput = { output ->
        continuation.resume(WorkerResult(output, this.transferables))
    }
    postInput(input, transferables)
}
