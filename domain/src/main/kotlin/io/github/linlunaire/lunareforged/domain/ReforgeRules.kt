package io.github.linlunaire.lunareforged.domain

import kotlin.math.ceil
import kotlin.math.pow

enum class Target { MELEE, AXE, BOW, TRIDENT, ARMOR, PICKAXE }

/** Gameplay adapted from Auxilor/Reforges. Monetary rewards become raw XP. */
data class Reforge(
    val id: String,
    val targets: Set<Target>,
    val stoneOnly: Boolean = false,
    val damage: Double = 1.0,
    val critical: Double = 1.0,
    val incoming: Double = 1.0,
    val speed: Double = 0.0,
    val attackSpeed: Double = 0.0,
    val knockback: Double = 0.0,
    val condition: String = "always",
    val chance: Double = 1.0,
    val killXp: Int = 0,
    val miningXp: Int = 0,
    val miningSpeed: Double = 0.0,
    val knockbackResistance: Double = 0.0,
    val name: String = "",
    val description: List<String> = emptyList(),
    val knockbackMultiplier: Double = 1.0,
    val incomingScope: String = "all",
    val burnSeconds: Int = 0,
    val slowTicks: Int = 0,
    val lifeSteal: Double = 0.0,
    val procCooldown: Int = 20
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_]{0,47}"))) { "Invalid reforge ID: $id" }
        require(targets.isNotEmpty()) { "$id: targets must not be empty" }
        require(listOf(damage, critical).all { it.isFinite() && it in 0.1..5.0 }) { "$id: damage and critical must be in 0.1..5" }
        require(incoming.isFinite() && incoming in 0.0..3.0) { "$id: incoming must be in 0..3" }
        require(listOf(speed, attackSpeed, miningSpeed).all { it.isFinite() && it in -0.5..1.0 }) { "$id: speed modifiers must be in -0.5..1" }
        require(knockback.isFinite() && knockback in -1.0..2.0) { "$id: knockback must be in -1..2" }
        require(knockbackMultiplier.isFinite() && knockbackMultiplier in 0.0..3.0) { "$id: knockbackMultiplier must be in 0..3" }
        require(incomingScope in setOf("all", "environment", "fall", "fire", "projectile")) { "$id: unknown incomingScope" }
        require(burnSeconds in 0..5 && slowTicks in 0..100) { "$id: status duration out of range" }
        require(lifeSteal.isFinite() && lifeSteal in 0.0..0.10) { "$id: lifeSteal must be in 0..0.10" }
        require(procCooldown in 20..200) { "$id: procCooldown must be in 20..200 ticks" }
        require(knockbackResistance.isFinite() && knockbackResistance in 0.0..0.5) { "$id: knockbackResistance must be in 0..0.5" }
        require(chance.isFinite() && chance in 0.0..1.0) { "$id: chance must be in 0..1" }
        require(killXp in 0..100 && miningXp in 0..20) { "$id: XP rewards out of range" }
        require(condition in setOf("always", "air", "water", "night", "day", "rain", "sprinting", "distant", "wounded_target", "low_health", "full_health")) { "$id: unknown condition $condition" }
        require(name.length <= 512 && name.none(Char::isISOControl)) { "$id: formatted name must be at most 512 printable characters" }
        require(description.size <= 4 && description.all { it.length <= 512 && it.none(Char::isISOControl) }) { "$id: description must contain at most four formatted lines" }
    }
}

data class PricePolicy(val baseLevels: Int = 3, val growth: Double = 1.15, val maxLevels: Int = 30, val stoneLevels: Int = 40) {
    init {
        require(baseLevels in 1..100 && maxLevels in baseLevels..100 && stoneLevels in 1..100) { "XP levels must be in 1..100, cap >= base" }
        require(growth.isFinite() && growth in 1.0..2.0) { "growth must be finite and in 1..2" }
    }
    fun cost(times: Int, stone: String?): Int {
        require(times in 0..1_000_000)
        if (stone != null) return if (stone == "dynamic") stoneLevels else baseLevels
        return ceil((baseLevels * growth.pow(times)).coerceAtMost(maxLevels.toDouble())).toInt()
    }
}

object ReforgeRules {
    private val melee = setOf(Target.MELEE)
    private val armor = setOf(Target.ARMOR)
    val definitions: List<Reforge> = listOf(
        Reforge("sharp", melee, damage = 1.03),
        Reforge("strong", melee, damage = 1.02, knockbackMultiplier = 1.10),
        Reforge("light", melee, damage = 0.95, attackSpeed = 0.10, knockbackMultiplier = 0.95),
        Reforge("gravitated", melee, critical = 1.08),
        Reforge("acute", setOf(Target.AXE), damage = 3.0, chance = 0.02),
        Reforge("dynamic", melee, stoneOnly = true, damage = 1.05, critical = 1.1),
        Reforge("streamlined", setOf(Target.BOW), damage = 1.04),
        Reforge("aerobic", setOf(Target.BOW), damage = 1.09, condition = "air"),
        Reforge("pointy", setOf(Target.TRIDENT), damage = 1.10),
        Reforge("nautical", setOf(Target.MELEE, Target.BOW, Target.TRIDENT), damage = 1.20, condition = "water"),
        Reforge("rich", setOf(Target.BOW, Target.TRIDENT), stoneOnly = true, killXp = 5),
        Reforge("prospecting", setOf(Target.PICKAXE), chance = 0.05, miningXp = 2),
        Reforge("reinforced", armor, incoming = 0.96, incomingScope = "environment"),
        Reforge("tough", armor, incoming = 0.92, speed = -0.05, incomingScope = "environment"),
        Reforge("thin", armor, incoming = 1.05, speed = 0.025, incomingScope = "environment"),
        Reforge("evasive", armor, incoming = 0.0, chance = 0.02, incomingScope = "environment"),
        Reforge("sundering", melee, damage = 1.08, attackSpeed = -0.06),
        Reforge("duelist", melee, damage = 1.04, attackSpeed = 0.04),
        Reforge("lunar", setOf(Target.MELEE, Target.BOW, Target.TRIDENT), damage = 1.12, condition = "night"),
        Reforge("desperate", melee, damage = 1.16, condition = "low_health"),
        Reforge("precise", setOf(Target.BOW, Target.TRIDENT), damage = 1.07, condition = "full_health"),
        Reforge("steadfast", armor, incoming = 0.95, knockbackResistance = 0.10),
        Reforge("fleet", armor, speed = 0.04, incoming = 1.02),
        Reforge("warded", armor, incoming = 0.90, condition = "low_health"),
        Reforge("resonant", setOf(Target.PICKAXE), miningSpeed = 0.08),
        Reforge("excavating", setOf(Target.PICKAXE), miningSpeed = 0.14, speed = -0.03),
        Reforge("scholar", setOf(Target.PICKAXE), chance = 0.10, miningXp = 1),
        Reforge("balanced", melee, attackSpeed = 0.06, critical = 1.04),
        Reforge("ember", melee, burnSeconds = 3, procCooldown = 40),
        Reforge("frostbound", setOf(Target.BOW, Target.TRIDENT), slowTicks = 40, procCooldown = 60),
        Reforge("siphoning", melee, lifeSteal = 0.05),
        Reforge("executioner", melee, damage = 1.12, condition = "wounded_target"),
        Reforge("dawn", setOf(Target.MELEE, Target.BOW, Target.TRIDENT), damage = 1.10, condition = "day"),
        Reforge("tempest", setOf(Target.BOW, Target.TRIDENT), damage = 1.14, condition = "rain"),
        Reforge("skirmishing", melee, damage = 1.08, condition = "sprinting"),
        Reforge("farshot", setOf(Target.BOW), damage = 1.12, condition = "distant"),
        Reforge("feathered", armor, incoming = 0.86, incomingScope = "fall", speed = 0.01),
        Reforge("cinderward", armor, incoming = 0.88, incomingScope = "fire"),
        Reforge("bulwark", armor, incoming = 0.92, incomingScope = "projectile", speed = -0.02),
        Reforge("moonveil", armor, incoming = 0.94, condition = "night"),
        Reforge("sentinel", armor, incoming = 0.94, condition = "full_health", knockbackResistance = 0.04),
        Reforge("gilded", setOf(Target.PICKAXE), chance = 0.06, miningXp = 3, miningSpeed = -0.04),
        Reforge("quarrying", setOf(Target.PICKAXE), miningSpeed = 0.20, speed = -0.05),
        Reforge("nimble", setOf(Target.PICKAXE), miningSpeed = 0.06, speed = 0.02)
    )
    private val byId = definitions.associateBy { it.id }
    val upstreamIds = definitions.take(16).map { it.id }.toSet()
    fun find(id: String?): Reforge? = byId[id]
    fun candidates(targets: Set<Target>, current: String?, stone: String?): List<Reforge> =
        definitions.filter { eligible(it, targets, current, stone) }

    fun eligible(def: Reforge, targets: Set<Target>, current: String?, stone: String?): Boolean =
        def.targets.any(targets::contains) && if (stone == null) !def.stoneOnly && def.id != current else def.id == stone

    /** Uniform selection, retaining upstream semantics; the caller owns the random source. */
    fun select(candidates: List<Reforge>, index: (Int) -> Int): Reforge? =
        if (candidates.isEmpty()) null else candidates[index(candidates.size)]
}
