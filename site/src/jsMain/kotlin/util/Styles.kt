package com.shakster.gifcreator.util

import com.varabyte.kobweb.compose.css.functions.brightness
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.filter
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.selectors.hover

val HoverHighlightStyle = CssStyle {
    hover {
        Modifier.filter(brightness(1.2))
    }
}
