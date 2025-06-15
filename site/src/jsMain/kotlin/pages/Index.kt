package com.shakster.gifcreator.pages

import androidx.compose.runtime.*
import com.shakster.gifcreator.components.FileInput
import com.shakster.gifcreator.shared.ColorDistanceCalculatorSettings
import com.shakster.gifcreator.shared.ColorQuantizerSettings
import com.shakster.gifcreator.shared.getByteArray
import com.shakster.gifcreator.shared.submit
import com.shakster.gifcreator.util.HoverHighlightStyle
import com.shakster.gifcreator.worker.GifFrameWrittenEvent
import com.shakster.gifcreator.worker.GifWorker
import com.shakster.gifcreator.worker.GifWorkerInput
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import com.shakster.gifkt.internal.centiseconds
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.Input
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.worker.Transferables
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.MessageChannel
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import kotlin.math.max
import kotlin.time.Duration

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

    val inputFiles = remember { mutableStateListOf<File>() }
    val inputUrls = remember { mutableStateListOf<String>() }

    var resultUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        document.title = "GIF Creator"

        coroutineScope.launch {
            worker.submit(GifWorkerInput.MessagePort, Transferables {
                add("port", messageChannel.port2)
            })
        }
    }

    Row(
        Modifier
            .fillMaxHeight()
            .padding(2.cssRem)
            .gap(1.cssRem),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier
                .width(30.cssRem)
                .fillMaxHeight()
                .gap(1.cssRem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FileInput(
                Modifier
                    .fillMaxWidth()
                    .height(15.cssRem)
                    .flexShrink(0)
                    .borderRadius(10.px)
                    .padding(30.px)
                    .backgroundColor(Color.gray)
                    .alignContent(AlignContent.Center)
                    .outline(width = 3.px, style = LineStyle.Dashed, color = Color.black)
                    .outlineOffset((-10).px)
                    .cursor(Cursor.Pointer)
                    .transition(Transition.of("ease", 0.1.s))
                    .then(HoverHighlightStyle.toModifier()),
                acceptTypes = listOf("image/*"),
            ) { files ->
                inputFiles.addAll(files)
                files.forEach { file ->
                    inputUrls += URL.createObjectURL(file)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(10.px),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (inputFiles.isEmpty()) return@Button
                        encodedFrames = 0
                        coroutineScope.launch {
                            val bytes = createGif(
                                inputFiles,
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
                        durationCentiseconds = max(value.toInt(), GIF_MINIMUM_FRAME_DURATION_CENTISECONDS)
                    },
                )

                Text("Frames encoded: $encodedFrames")
            }

            Image(
                resultUrl,
                Modifier.maxSize(100.percent),
            )
        }

        Div(
            Modifier
                .width(30.cssRem)
                .fillMaxHeight()
                .borderRadius(10.px)
                .padding(20.px)
                .backgroundColor(Color.gray)
                .overflow(Overflow.Auto)
                .toAttrs(),
        ) {
            Div(
                Modifier
                    .display(DisplayStyle.Grid)
                    .gridTemplateColumns {
                        repeat(3) {
                            size(1.fr)
                        }
                    }
                    .gap(1.cssRem)
                    .toAttrs(),
            ) {
                inputUrls.forEachIndexed { index, url ->
                    Box(
                        Modifier
                            .height(10.cssRem)
                            .backgroundColor(Color.lightgray)
                            .borderRadius(10.px)
                            .padding(10.px)
                            .onClick {
                                inputFiles.removeAt(index)
                                inputUrls.removeAt(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            url,
                            Modifier.maxSize(100.percent),
                        )
                    }
                }
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
                worker.submit(GifWorkerInput.Shutdown)
                worker.terminate()
            }
        }
    }
    return worker
}

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
            Transferables {
                add("image", image)
            },
        )
    }
    val (_, transferables) = worker.submit(GifWorkerInput.EncoderClose)
    return transferables.getByteArray("bytes")
        ?: throw IllegalStateException("Bytes are missing")
}
