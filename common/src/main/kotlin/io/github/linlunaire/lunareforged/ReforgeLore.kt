package io.github.linlunaire.lunareforged

import io.github.linlunaire.lunareforged.domain.Reforge
import io.github.linlunaire.lunareforged.domain.Target
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.math.BigDecimal
import java.math.RoundingMode

/** One presentation of the actual mechanics, shared by the tooltip and table preview. */
object ReforgeLore {
    private fun number(value: Double) = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    fun lines(def: Reforge): List<Component> {
        if (def.description.isNotEmpty()) return def.description.map(ReforgeText::parse)
        val result = mutableListOf<Component>()
        fun stat(value: Double, label: String, percent: Boolean = true, conditional: Boolean = false, vararg args: Any) {
            val prefix = (if (value >= 0) "+" else "") + number(value) + if (percent) "%" else ""
            val line = Component.literal(prefix).withStyle(if (value >= 0) ChatFormatting.GREEN else ChatFormatting.RED)
                .append(" ").append(ReforgeText.component("stat.luna_reforged.$label", *args).withStyle(ChatFormatting.WHITE))
            if (conditional && def.condition != "always") line.append(ReforgeText.component("condition.luna_reforged.${def.condition}").withStyle(ChatFormatting.GRAY))
            val chanceIsValue = label.startsWith("dodge_") || label == "mining_xp" || label == "triple_chance"
            if (conditional && def.chance < 1.0 && !chanceIsValue) line.append(ReforgeText.component("stat.luna_reforged.chance", number(def.chance * 100)).withStyle(ChatFormatting.GRAY))
            result.add(line.withStyle { it.withItalic(false) })
        }
        if (def.id == "acute") stat(def.chance * 100, "triple_chance")
        else if (def.damage != 1.0) {
            val label = when {
                def.targets == setOf(Target.BOW) -> "arrow_damage"
                def.targets == setOf(Target.TRIDENT) -> "trident_damage"
                def.targets.all { it == Target.BOW || it == Target.TRIDENT } -> "projectile_damage"
                def.targets.all { it == Target.MELEE || it == Target.AXE } -> "melee_damage"
                else -> "damage"
            }
            stat((def.damage - 1) * 100, label, conditional = true)
        }
        if (def.knockbackMultiplier != 1.0) stat((def.knockbackMultiplier - 1) * 100, "knockback")
        if (def.attackSpeed != 0.0) stat(def.attackSpeed * 100, "attack_speed")
        if (def.critical != 1.0) stat((def.critical - 1) * 100, "falling_damage", conditional = true)
        if (def.incoming == 0.0) stat(def.chance * 100, "dodge_${def.incomingScope}", conditional = true)
        else if (def.incoming != 1.0) stat((1 - def.incoming) * 100, "resistance_${def.incomingScope}", conditional = true)
        if (def.speed != 0.0) stat(def.speed * 100, "movement_speed")
        if (def.knockback != 0.0) stat(def.knockback, "flat_knockback", percent = false)
        if (def.knockbackResistance != 0.0) stat(def.knockbackResistance * 100, "knockback_resistance")
        if (def.miningSpeed != 0.0) stat(def.miningSpeed * 100, "mining_speed")
        if (def.killXp > 0) stat(def.killXp.toDouble(), "kill_xp", percent = false, conditional = true)
        if (def.miningXp > 0) stat(def.chance * 100, "mining_xp", true, true, def.miningXp)
        if (def.burnSeconds > 0) stat(def.burnSeconds.toDouble(), "burn_seconds", percent = false, conditional = true)
        if (def.slowTicks > 0) stat(def.slowTicks / 20.0, "slow_seconds", percent = false, conditional = true)
        if (def.lifeSteal > 0) {
            stat(def.lifeSteal * 100, "life_steal", conditional = true)
            result.add(ReforgeText.component("stat.luna_reforged.life_steal_cap").withStyle(ChatFormatting.GRAY))
        }
        if (def.burnSeconds > 0 || def.slowTicks > 0 || def.lifeSteal > 0) {
            result.add(ReforgeText.component("stat.luna_reforged.cooldown", number(def.procCooldown / 20.0)).withStyle(ChatFormatting.GRAY))
        }
        return result
    }
}
