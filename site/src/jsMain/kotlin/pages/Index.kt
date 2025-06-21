package com.shakster.gifcreator.pages

import androidx.compose.runtime.*
import com.shakster.gifcreator.components.FileDropBox
import com.shakster.gifcreator.shared.ColorDistanceCalculatorSettings
import com.shakster.gifcreator.shared.ColorQuantizerSettings
import com.shakster.gifcreator.shared.getByteArray
import com.shakster.gifcreator.shared.submit
import com.shakster.gifcreator.util.DynamicGrid4
import com.shakster.gifcreator.util.HighlightModifier
import com.shakster.gifcreator.util.HoverHighlightStyle
import com.shakster.gifcreator.util.styled
import com.shakster.gifcreator.worker.GifFrameWrittenEvent
import com.shakster.gifcreator.worker.GifWorker
import com.shakster.gifcreator.worker.GifWorkerInput
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
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.MessageChannel
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Page
@Composable
fun HomePage() {
    val coroutineScope = rememberCoroutineScope()

    val worker = createWorker()

    var encodedFrames by remember { mutableStateOf(0) }

    val messageChannel = remember {
        val channel = MessageChannel()
        channel.port1.onmessage = { event ->
            val frameEvent = Json.decodeFromString<GifFrameWrittenEvent>(event.data as String)
            encodedFrames = frameEvent.framesWritten
        }
        channel
    }

    var durationCentiseconds by remember { mutableStateOf(100) }

    val inputFiles = remember { mutableStateListOf<InputFile>() }

    var resultUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            worker.submit(GifWorkerInput.MessagePort, Attachments {
                add("port", messageChannel.port2)
            })
        }
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
                        inputFiles.isEmpty(),
                        Modifier
                            .cursor(Cursor.Pointer)
                            .styled(HoverHighlightStyle),
                    )
                    .thenIf(
                        highlight && inputFiles.isEmpty(),
                        HighlightModifier,
                    ),
                clickable = inputFiles.isEmpty(),
                onDrag = { isEnter ->
                    highlight = isEnter
                },
                onFilesSelected = { files ->
                    files.forEach { file ->
                        inputFiles += InputFile(file, URL.createObjectURL(file))
                    }
                },
            ) {
                if (inputFiles.isEmpty()) {
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
                        inputFiles.forEachIndexed { index, file ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .backgroundColor(Color.lightgray)
                                    .borderRadius(10.px)
                                    .padding(10.px)
                                    .onClick {
                                        val removed = inputFiles.removeAt(index)
                                        URL.revokeObjectURL(removed.url)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    file.url,
                                    Modifier.maxSize(100.percent),
                                )
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
                        if (inputFiles.isEmpty()) return@Button
                        encodedFrames = 0
                        coroutineScope.launch {
                            val bytes = createGif(
                                inputFiles.map(InputFile::file),
                                durationCentiseconds.centiseconds,
                                worker,
                            )
                            val blob = Blob(
                                arrayOf(bytes),
                                BlobPropertyBag("image/gif"),
                            )
                            resultUrl = URL.createObjectURL(blob)
                        }
                    },
                    enabled = inputFiles.isNotEmpty(),
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

                Text("Frames encoded: $encodedFrames")
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

@Composable
private fun createWorker(): GifWorker {
    val worker = remember {
        GifWorker()
    }
    DisposableEffect(worker) {
        onDispose {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    withTimeout(10.seconds) {
                        worker.submit(GifWorkerInput.Shutdown)
                    }
                } finally {
                    worker.terminate()
                }
            }
        }
    }
    return worker
}

private data class InputFile(
    val file: File,
    val url: String,
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
        colorDistanceCalculatorSettings = ColorDistanceCalculatorSettings.CieLab,
        comment = "",
        alphaFill = -1,
        cropTransparent = true,
        minimumFrameDurationCentiseconds = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    )
    worker.submit(input)
    files.forEach { file ->
        val image = window.createImageBitmap(file).await()
        worker.submit(
            GifWorkerInput.Frame(duration),
            Attachments {
                add("image", image)
            },
        )
    }
    val (_, attachments) = worker.submit(GifWorkerInput.EncoderClose)
    return attachments.getByteArray("bytes")
        ?: throw IllegalStateException("Bytes are missing")
}
