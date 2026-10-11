package io.github.linlunaire.lunareforged

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import kotlin.math.roundToInt

/** Native rendering of the eco gradient syntax and legacy colors used by Reforges. */
object ReforgeText {
    private val gradient = Regex("<gradient:#([0-9a-fA-F]{6})>(.*?)</gradient:#([0-9a-fA-F]{6})>")
    private val fallback by lazy {
        val stream = ReforgeText::class.java.getResourceAsStream("/assets/luna_reforged/lang/en_us.json")!!
        stream.bufferedReader(Charsets.UTF_8).use { com.google.gson.JsonParser.parseReader(it).asJsonObject }
    }
    private data class Glyph(val text: String, val style: Style)

    // Dedicated servers don't load client language packs. Keep their displayed names readable too.
    fun component(key: String, vararg args: Any): MutableComponent = Component.translatableWithFallback(key, fallback[key]?.asString ?: key, *args)
    fun localized(key: String): String = component(key).string

    fun parse(text: String): MutableComponent {
        val output = Component.empty()
        var offset = 0
        for (match in gradient.findAll(text)) {
            append(output, glyphs(text.substring(offset, match.range.first)))
            val chars = glyphs(match.groupValues[2])
            val start = match.groupValues[1].toInt(16)
            val end = match.groupValues[3].toInt(16)
            for ((index, glyph) in chars.withIndex()) {
                val fraction = if (chars.size == 1) 0.0 else index.toDouble() / (chars.size - 1)
                var rgb = 0
                for (shift in listOf(16, 8, 0)) {
                    val a = (start shr shift) and 255
                    val b = (end shr shift) and 255
                    rgb = rgb or ((a + (b - a) * fraction).roundToInt() shl shift)
                }
                output.append(Component.literal(glyph.text).setStyle(glyph.style.withColor(TextColor.fromRgb(rgb))))
            }
            offset = match.range.last + 1
        }
        append(output, glyphs(text.substring(offset)))
        return output
    }

    private fun append(output: MutableComponent, glyphs: List<Glyph>) {
        glyphs.forEach { output.append(Component.literal(it.text).setStyle(it.style)) }
    }

    private fun glyphs(text: String): List<Glyph> {
        val result = ArrayList<Glyph>()
        var style = Style.EMPTY.withItalic(false)
        var index = 0
        while (index < text.length) {
            if ((text[index] == '&' || text[index] == '§') && index + 1 < text.length) {
                if (text[index + 1] == '#' && index + 8 <= text.length) {
                    val rgb = text.substring(index + 2, index + 8).toIntOrNull(16)
                    if (rgb != null) { style = Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(rgb)); index += 8; continue }
                }
                val format = ChatFormatting.getByCode(text[index + 1].lowercaseChar())
                if (format != null) {
                    style = if (format == ChatFormatting.RESET) Style.EMPTY.withItalic(false) else style.applyLegacyFormat(format)
                    index += 2
                    continue
                }
            }
            val point = text.codePointAt(index)
            result.add(Glyph(String(Character.toChars(point)), style))
            index += Character.charCount(point)
        }
        return result
    }
}
