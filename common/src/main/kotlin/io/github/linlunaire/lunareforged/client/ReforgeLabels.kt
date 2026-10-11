package io.github.linlunaire.lunareforged.client

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import java.util.Optional

object ReforgeLabels {
    fun fit(font: Font, text: Component, width: Int): Component {
        if (font.width(text) <= width) return text
        val result = Component.empty()
        font.substrByWidth(text, width - font.width("…")).visit(FormattedText.StyledContentConsumer<Unit> { style, part ->
            result.append(Component.literal(part).setStyle(style))
            Optional.empty()
        }, Style.EMPTY)
        return result.append(Component.literal("…"))
    }
}
