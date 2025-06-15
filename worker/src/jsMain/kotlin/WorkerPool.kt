package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.WorkerMessage
import com.shakster.gifcreator.shared.WorkerOutput
import com.shakster.gifcreator.shared.submit
import com.varabyte.kobweb.worker.Transferables
import com.varabyte.kobweb.worker.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.*

class WorkerPool<I, O : WorkerOutput>(
    size: Int,
    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    createWorker: () -> Worker<I, O>,
) {

    private val inputs: Channel<Input<I, O>> = Channel(Channel.UNLIMITED)
    private val workers: List<Worker<I, O>> = List(size) { createWorker() }
    private val availableWorkers: Channel<Worker<I, O>> = Channel(size)

    private val jobRunner: Job = coroutineScope.launch {
        workers.forEach {
            availableWorkers.send(it)
        }
        for ((input, transferables, continuation) in inputs) {
            val worker = availableWorkers.receive()
            try {
                val result = worker.submit(input, transferables)
                continuation.resume(result)
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            } finally {
                availableWorkers.send(worker)
            }
        }
    }

    suspend fun submit(
        input: I,
        transferables: Transferables = Transferables.Empty,
    ): WorkerMessage<O> = suspendCoroutine { continuation ->
        coroutineScope.launch {
            inputs.send(Input(input, transferables, continuation))
        }
    }

    suspend fun shutdown() {
        inputs.close()
        jobRunner.join()
        workers.forEach {
            it.terminate()
        }
    }

    private data class Input<I, O>(
        val data: I,
        val transferables: Transferables,
        val continuation: Continuation<WorkerMessage<O>>
    )
}
