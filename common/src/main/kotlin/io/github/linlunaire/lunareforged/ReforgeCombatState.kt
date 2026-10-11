package io.github.linlunaire.lunareforged

/** Entity-owned, transient cooldown; no global entity map or per-tick task. */
interface ReforgeCombatState {
    fun `luna$tryProc`(tick: Long, cooldown: Int): Boolean
}
