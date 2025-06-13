package com.shakster.gifcreator.worker

import com.shakster.gifcreator.shared.WorkerResult
import com.shakster.gifcreator.shared.submit
import com.varabyte.kobweb.worker.Transferables
import com.varabyte.kobweb.worker.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkerPool<I, O>(
    size: Int,
    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    createWorker: () -> Worker<I, O>,
) {

    private val inputs: Channel<Input<I, O>> = Channel(UNLIMITED)
    private val workers: List<Worker<I, O>> = List(size) { createWorker() }
    private val availableWorkers: Channel<Worker<I, O>> = Channel(size)

    private val jobRunner: Job = coroutineScope.launch {
        workers.forEach {
            availableWorkers.send(it)
        }
        for ((input, inputTransferables, continuation) in inputs) {
            val worker = availableWorkers.receive()
            val output = worker.submit(input, inputTransferables)
            availableWorkers.send(worker)
            continuation.resume(output)
        }
    }

    suspend fun submit(input: I, transferables: Transferables): WorkerResult<O> {
        return suspendCoroutine { continuation ->
            coroutineScope.launch {
                inputs.send(Input(input, transferables, continuation))
            }
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
        val continuation: Continuation<WorkerResult<O>>
    )
}
