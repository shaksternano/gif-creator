package com.shakster.gifcreator.pages

import androidx.compose.runtime.*
import com.shakster.gifcreator.components.FileDropBox
import com.shakster.gifcreator.shared.*
import com.shakster.gifcreator.util.DynamicGrid4
import com.shakster.gifcreator.util.HighlightModifier
import com.shakster.gifcreator.util.HoverHighlightStyle
import com.shakster.gifcreator.util.styled
import com.shakster.gifcreator.worker.GifFrameWrittenEvent
import com.shakster.gifcreator.worker.GifWorker
import com.shakster.gifcreator.worker.GifWorkerInput
import com.shakster.gifcreator.worker.GifWorkerOutput
import com.shakster.gifkt.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import com.shakster.gifkt.centiseconds
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.css.autoLength
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.thenIf
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.Input
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.worker.Attachments
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.MessageChannel
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Page
@Composable
fun HomePage() {
    val coroutineScope = rememberCoroutineScope()

    val mutex = remember { Mutex() }
    val worker = createWorker(mutex)

    var encoding by remember { mutableStateOf(false) }

    var startTime by remember { mutableStateOf(Clock.System.now()) }
    var elapsedTime by remember { mutableStateOf(0.seconds) }
    var encodedFrames by remember { mutableStateOf(0) }
    var averageFps by remember { mutableStateOf(0.0) }
    var maxFps by remember { mutableStateOf(0.0) }

    val messageChannel = remember {
        val channel = MessageChannel()
        channel.port1.onmessage = { event ->
            val frameEvent = Json.decodeFromString<GifFrameWrittenEvent>(event.data as String)
            encodedFrames = frameEvent.framesWritten
            averageFps = frameEvent.framesWritten / (elapsedTime.inWholeMilliseconds / 1000.0)
            maxFps = max(maxFps, averageFps)
        }
        channel
    }

    var durationCentiseconds by remember { mutableStateOf(100) }

    val gifInputs = remember { mutableStateListOf<GifInput>() }
    val totalFrames by remember { derivedStateOf { gifInputs.sumOf { it.frameCount } } }

    var resultUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            mutex.withLock {
                worker.submit(GifWorkerInput.MessagePort, Attachments {
                    add("port", messageChannel.port2)
                })
            }
        }

        window.setInterval({
            if (encoding) {
                elapsedTime = Clock.System.now() - startTime
            }
        })
    }

    Row(
        Modifier
            .fillMaxHeight()
            .padding(1.cssRem),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .gap(1.cssRem),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var highlight by remember { mutableStateOf(false) }

            FileDropBox(
                Modifier
                    .width(80.percent)
                    .minWidth(300.px)
                    .maxWidth(1200.px)
                    .height(80.percent)
                    .minHeight(300.px)
                    .borderRadius(10.px)
                    .padding(30.px)
                    .backgroundColor(Color.gray)
                    .outline(width = 3.px, style = LineStyle.Dashed, color = Color.black)
                    .outlineOffset((-10).px)
                    .display(DisplayStyle.Flex)
                    .overflow(Overflow.Auto)
                    .transition(Transition.of("ease", 0.1.s))
                    .thenIf(
                        gifInputs.isEmpty(),
                        Modifier
                            .cursor(Cursor.Pointer)
                            .styled(HoverHighlightStyle),
                    )
                    .thenIf(
                        highlight && gifInputs.isEmpty(),
                        HighlightModifier,
                    ),
                acceptTypes = listOf("image/*", "video/*"),
                clickable = gifInputs.isEmpty(),
                onDrag = { isEnter ->
                    highlight = isEnter
                },
                onFilesSelected = { files ->
                    coroutineScope.launch {
                        files.forEach { file ->
                            try {
                                val mediaInfo = mutex.withLock {
                                    worker.submit(
                                        GifWorkerInput.MediaQuery,
                                        Attachments {
                                            add("file", file)
                                        },
                                    )
                                }.content
                                if (mediaInfo !is GifWorkerOutput.MediaQueryResult) {
                                    throw createWrongOutputTypeException(mediaInfo)
                                }
                                gifInputs += GifInput(
                                    file,
                                    URL.createObjectURL(file),
                                    mediaInfo.frameCount,
                                    mediaInfo.isVideo,
                                )
                            } catch (t: Throwable) {
                                console.error("Error processing file: ${file.name}", t.message ?: t)
                                window.alert("Error processing file: ${file.name}")
                            }
                        }
                    }
                },
            ) {
                if (gifInputs.isEmpty()) {
                    H1(
                        Modifier
                            .margin(autoLength)
                            .fontSize(2.cssRem)
                            .toAttrs(),
                    ) {
                        Text("Drop images here")
                    }
                } else {
                    Div(
                        Modifier
                            .margin(autoLength)
                            .gap(1.cssRem)
                            .styled(DynamicGrid4)
                            .toAttrs(),
                    ) {
                        gifInputs.forEachIndexed { index, file ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .backgroundColor(Color.lightgray)
                                    .borderRadius(10.px)
                                    .padding(10.px)
                                    .onClick {
                                        val removed = gifInputs.removeAt(index)
                                        URL.revokeObjectURL(removed.url)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (file.isVideo) {
                                    Video(
                                        Modifier
                                            .maxSize(100.percent)
                                            .toAttrs {
                                                attr("autoplay", "")
                                                attr("muted", "")
                                                attr("playsinline", "")
                                                attr("loop", "")
                                                ref { element ->
                                                    /*
                                                     * "muted" attribute doesn't work with
                                                     * dynamically added video elements
                                                     */
                                                    element.muted = true
                                                    onDispose {}
                                                }
                                            }
                                    ) {
                                        Source(attrs = {
                                            attr("src", file.url)
                                            attr("type", "video/mp4")
                                        })
                                    }
                                } else {
                                    Image(
                                        file.url,
                                        Modifier.maxSize(100.percent),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(10.px),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (gifInputs.isEmpty()) return@Button
                        startTime = Clock.System.now()
                        encoding = true
                        encodedFrames = 0
                        coroutineScope.launch {
                            val bytes = mutex.withLock {
                                createGif(
                                    gifInputs.map(GifInput::file),
                                    durationCentiseconds.centiseconds,
                                    worker,
                                )
                            }
                            val blob = Blob(
                                arrayOf(bytes),
                                BlobPropertyBag("image/gif"),
                            )
                            resultUrl = URL.createObjectURL(blob)
                            encoding = false
                            averageFps = totalFrames / (elapsedTime.inWholeMilliseconds / 1000.0)
                            maxFps = max(maxFps, averageFps)
                        }
                    },
                    enabled = gifInputs.isNotEmpty(),
                ) {
                    Text("Create GIF")
                }

                Input(
                    InputType.Number,
                    value = durationCentiseconds,
                    onValueChange = { value ->
                        if (value == null) {
                            return@Input
                        }
                        durationCentiseconds = max(
                            value.toInt(),
                            GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
                        )
                    },
                )

                Text(
                    "Frames encoded: $encodedFrames/$totalFrames, " +
                        "Average FPS: ${averageFps.format(2)}, " +
                        "Max FPS: ${maxFps.format(2)}, " +
                        "Elapsed time: $elapsedTime",
                )
            }

            if (resultUrl.isNotEmpty()) {
                Image(
                    resultUrl,
                    Modifier.maxHeight(500.px),
                )
            }
        }
    }
}

private fun Number.format(decimals: Int): Double {
    return toFixed(decimals).toDouble()
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun createWorker(mutex: Mutex): GifWorker {
    val worker = remember { GifWorker() }
    DisposableEffect(worker) {
        onDispose {
            GlobalScope.launch {
                try {
                    withTimeout(10.seconds) {
                        mutex.withLock {
                            worker.submit(GifWorkerInput.Shutdown)
                        }
                    }
                } finally {
                    worker.terminate()
                }
            }
        }
    }
    return worker
}

private data class GifInput(
    val file: File,
    val url: String,
    val frameCount: Int,
    val isVideo: Boolean,
)

private suspend fun createGif(
    files: List<File>,
    duration: Duration,
    worker: GifWorker,
): ByteArray {
    val input = GifWorkerInput.EncoderInit(
        transparencyColorTolerance = 0.01,
        quantizedTransparencyColorTolerance = 0.02,
        loopCount = 0,
        maxColors = 256,
        colorQuantizerSettings = ColorQuantizerSettings.NeuQuant(10),
        colorSimilarityCheckerSettings = ColorSimilarityCheckerSettings.Euclidean(2.99, 5.87, 1.14),
        comment = "",
        alphaFill = -1,
        cropTransparent = true,
        minimumFrameDurationCentiseconds = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    )
    worker.submit(input)
    files.forEach { file ->
        worker.submit(
            GifWorkerInput.Frames(duration),
            Attachments {
                add("file", file)
            },
        )
    }
    val (_, attachments) = worker.submit(GifWorkerInput.EncoderClose)
    return attachments.getByteArray("bytes")
        ?: throw IllegalStateException("Bytes are missing")
}
