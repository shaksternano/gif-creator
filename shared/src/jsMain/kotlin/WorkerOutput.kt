package com.shakster.gifcreator.shared

interface WorkerOutput {

    val isError: Boolean
        get() = false

    val message: String
        get() = ""
}
