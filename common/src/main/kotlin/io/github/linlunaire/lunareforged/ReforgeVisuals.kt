package io.github.linlunaire.lunareforged

import io.github.linlunaire.lunareforged.domain.Reforge

/** A block event carries a single byte, so transmit a small palette index, never raw RGB. */
object ReforgeVisuals {
    fun tone(def: Reforge): Int = when {
        def.burnSeconds > 0 || def.incomingScope == "fire" -> 1
        def.slowTicks > 0 || def.condition == "water" || def.condition == "rain" -> 2
        def.lifeSteal > 0 -> 3
        def.miningXp > 0 || def.killXp > 0 -> 4
        else -> 0
    }
    fun accent(def: Reforge?): Int = 0xFF000000.toInt() or
        (def?.let { ReforgeItems.name(it).siblings.firstOrNull()?.style?.color?.value } ?: 0xC4B7F1)
}
