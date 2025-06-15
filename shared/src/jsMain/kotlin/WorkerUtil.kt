package com.shakster.gifcreator.shared

import com.varabyte.kobweb.worker.Transferables
import com.varabyte.kobweb.worker.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class WorkerResult<O>(
    val output: O,
    val transferables: Transferables,
)

suspend fun <I, O : WorkerOutput> Worker<I, O>.submit(
    input: I,
    transferables: Transferables = Transferables.Empty,
): WorkerResult<O> = suspendCoroutine { continuation ->
    onOutput = { output ->
        if (output.isError) {
            var exceptionMessage = "Worker returned an error."
            if (output.message.isNotBlank()) {
                exceptionMessage += "\n\nMessage: ${output.message}"
            }
            exceptionMessage += "\n\nInput: $input" +
                "\n\nTransferables: ${JSON.stringify(transferables.toJson())}"
            continuation.resumeWithException(Exception(exceptionMessage))
        } else {
            continuation.resume(WorkerResult(output, this.transferables))
        }
    }
    try {
        postInput(input, transferables)
    } catch (t: Throwable) {
        val exceptionMessage = "Failed to post message to worker." +
            "\n\nReason: $t" +
            "\n\nInput: $input" +
            "\n\nTransferables: ${JSON.stringify(transferables.toJson())}"
        continuation.resumeWithException(Exception(exceptionMessage, t))
    }
}
