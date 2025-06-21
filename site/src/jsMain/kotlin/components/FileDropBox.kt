package com.shakster.gifcreator.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.display
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File

@Composable
fun FileDropBox(
    modifier: Modifier = Modifier,
    multipleFiles: Boolean = true,
    acceptTypes: Collection<String> = emptyList(),
    clickable: Boolean = true,
    onDrag: ((isEnter: Boolean) -> Unit)? = null,
    onFilesSelected: (List<File>) -> Unit,
    content: ContentBuilder<HTMLDivElement>? = null,
) {
    var inputElement by remember { mutableStateOf<HTMLInputElement?>(null) }

    Div(
        modifier.toAttrs {
            if (clickable) {
                onClick {
                    inputElement?.click()
                }
            }

            onDragOver { event ->
                event.preventDefault()
                onDrag?.invoke(true)
            }

            onDragLeave { event ->
                event.nativeEvent
                onDrag?.invoke(false)
            }

            onDrop { event ->
                event.preventDefault()
                onDrag?.invoke(false)
                val files = event.dataTransfer?.files ?: return@onDrop
                if (files.length == 0) return@onDrop
                onFilesSelected(files.asList())
            }
        },
    ) {
        content?.invoke(this)

        FileInput(
            modifier = Modifier.display(DisplayStyle.None),
            multipleFiles = multipleFiles,
            acceptTypes = acceptTypes,
            ref = { element ->
                inputElement = element
            },
            onFilesSelected = onFilesSelected,
        )
    }
}
