package io.github.linlunaire.lunareforged.client

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Shared, renderer-independent presentation state. Only a server receipt starts success feedback. */
class LunarAnimation {
    var ticks = 0; private set
    var flashTicks = 0; private set
    var pendingTicks = 0; private set
    private var receipt = -1
    fun request() { pendingTicks = 20 }
    fun tick(sequence: Int) {
        ticks++
        if (flashTicks > 0) flashTicks--
        if (pendingTicks > 0) pendingTicks--
        if (receipt >= 0 && sequence != receipt) { flashTicks = 32; pendingTicks = 0 }
        receipt = sequence
    }
    companion object {
        // Geometry does not allocate or recompute trigonometry each rendered frame.
        val halo = List(36) { i -> val angle = i * Math.PI / 18; Pair(cos(angle), sin(angle)) }
        val crescent = (-8..8).map { row -> row to sqrt((64 - row * row).coerceAtLeast(0).toDouble()).toInt() }
    }
}
