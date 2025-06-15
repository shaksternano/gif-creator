package com.shakster.gifcreator.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.accept
import org.jetbrains.compose.web.attributes.multiple
import org.jetbrains.compose.web.dom.Input
import org.w3c.dom.asList
import org.w3c.files.File

@Composable
fun FileInput(
    modifier: Modifier = Modifier,
    multipleFiles: Boolean = true,
    acceptTypes: Collection<String> = emptyList(),
    onFilesSelected: (List<File>) -> Unit,
) {
    Input(
        InputType.File,
        modifier.toAttrs {
            if (multipleFiles) {
                multiple()
            }

            if (acceptTypes.isNotEmpty()) {
                accept(acceptTypes.joinToString(", "))
            }

            onInput { event ->
                val files = event.target.files ?: return@onInput
                if (files.length == 0) return@onInput
                onFilesSelected(files.asList())
            }
        }
    )
}
