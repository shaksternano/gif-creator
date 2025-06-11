package com.shakster.gifcreator.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.core.Page
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Text

@Page
@Composable
fun HomePage() {
    LaunchedEffect(Unit) {
        document.title = "GIF Creator"
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("GIF Creator")
    }
}
