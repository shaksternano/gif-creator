package com.shakster.gifcreator.util

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.functions.brightness
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.display
import com.varabyte.kobweb.compose.ui.modifiers.filter
import com.varabyte.kobweb.compose.ui.modifiers.gridTemplateColumns
import com.varabyte.kobweb.silk.style.CssRule
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.GeneralKind
import com.varabyte.kobweb.silk.style.selectors.hover
import com.varabyte.kobweb.silk.style.toModifier
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.fr

@Composable
fun Modifier.styled(vararg styles: CssStyle<GeneralKind>): Modifier {
    return styles.fold(this) { modifier, style ->
        modifier then style.toModifier()
    }
}

val HighlightModifier = Modifier.filter(brightness(1.2))

val HoverHighlightStyle = CssStyle {
    hover {
        HighlightModifier
    }
}

val DynamicGrid4 = dynamicGrid(4)

private fun dynamicGrid(
    maxColumns: Int,
) = CssStyle {
    base {
        Modifier.display(DisplayStyle.Grid)
    }

    repeat(maxColumns) { i ->
        if (i in 1..maxColumns - 1) {
            val column = i + 1
            CssRule.OfPseudoClass("has(:nth-child($column):nth-last-child(1))").invoke {
                Modifier.gridTemplateColumns {
                    repeat(column) {
                        size(1.fr)
                    }
                }
            }
        } else {
            CssRule.OfPseudoClass("has(:nth-child($maxColumns))").invoke {
                Modifier.gridTemplateColumns {
                    repeat(maxColumns) {
                        size(1.fr)
                    }
                }
            }
        }
    }
}
