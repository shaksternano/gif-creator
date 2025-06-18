package com.shakster.gifcreator.shared

import com.varabyte.kobweb.worker.Attachments
import com.varabyte.kobweb.worker.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class WorkerMessage<out T>(
    val content: T,
    val attachments: Attachments,
)

suspend fun <I, O : WorkerOutput> Worker<I, O>.submit(
    input: I,
    attachments: Attachments = Attachments.Empty,
): WorkerMessage<O> = suspendCoroutine { continuation ->
    onOutput = { output ->
        if (output.isError) {
            var exceptionMessage = "Worker returned an error."
            if (output.message.isNotBlank()) {
                exceptionMessage += "\n\nMessage: ${output.message}"
            }
            exceptionMessage += "\n\nInput: $input" +
                "\n\nAttachments: ${JSON.stringify(attachments.toJson())}"
            continuation.resumeWithException(Exception(exceptionMessage))
        } else {
            continuation.resume(WorkerMessage(output, this.attachments))
        }
    }
    try {
        postInput(input, attachments)
    } catch (t: Throwable) {
        val exceptionMessage = "Failed to post message to worker." +
            "\n\nReason: $t" +
            "\n\nInput: $input" +
            "\n\nAttachments: ${JSON.stringify(attachments.toJson())}"
        continuation.resumeWithException(Exception(exceptionMessage, t))
    }
}
