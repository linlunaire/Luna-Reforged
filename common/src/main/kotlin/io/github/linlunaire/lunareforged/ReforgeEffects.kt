package io.github.linlunaire.lunareforged

import io.github.linlunaire.lunareforged.domain.Reforge
import io.github.linlunaire.lunareforged.domain.Target
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.tags.DamageTypeTags
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel

/** Shared hooks run once on both loaders, on the owning server thread. */
object ReforgeEffects {
    private val armor = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
    private inline fun forEachWeapon(source: DamageSource, action: (Reforge) -> Unit) {
        val projectile = GameVersion.projectileWeapon(source)
        if (projectile != null) {
            ReforgeItems.active(projectile)?.let(action)
            return
        }
        val attacker = source.entity as? LivingEntity ?: return
        if (source.directEntity !== attacker || !(source.`is`(DamageTypes.PLAYER_ATTACK) || source.`is`(DamageTypes.MOB_ATTACK))) return
        for (hand in 0..1) {
            val def = ReforgeItems.active(if (hand == 0) attacker.mainHandItem else attacker.offhandItem) ?: continue
            if (Target.MELEE in def.targets || Target.AXE in def.targets) action(def)
        }
    }
    private fun condition(def: Reforge, attacker: LivingEntity, target: LivingEntity? = null) = when (def.condition) {
        "air" -> !attacker.onGround()
        "water" -> attacker.isInWater
        "night" -> GameVersion.isNight(attacker.level())
        "day" -> attacker.level().dimensionType().hasSkyLight() && !GameVersion.isNight(attacker.level())
        "rain" -> attacker.level().isRainingAt(attacker.blockPosition())
        "sprinting" -> attacker.isSprinting
        "distant" -> target != null && attacker.distanceToSqr(target) >= 144.0
        "wounded_target" -> target != null && target.health <= target.maxHealth * 0.3f
        "low_health" -> attacker.health <= attacker.maxHealth * 0.4f
        "full_health" -> attacker.health >= attacker.maxHealth * 0.9f
        else -> true
    }
    @JvmStatic fun damage(target: LivingEntity, source: DamageSource, amount: Float): Float {
        if (GameVersion.isClient(target.level()) || amount <= 0 || !amount.isFinite()) return amount
        var result = amount.toDouble()
        val attacker = source.entity as? LivingEntity
        if (attacker != null) forEachWeapon(source) { def ->
            if (condition(def, attacker, target) && (def.chance == 1.0 || attacker.random.nextDouble() < def.chance)) {
                result *= def.damage
                // Upstream's crit effect tests falling velocity, independently of vanilla's crit flag.
                if (source.directEntity === attacker && attacker is Player && attacker.deltaMovement.y < -0.1) result *= def.critical
            }
        }
        if (!source.`is`(DamageTypes.FELL_OUT_OF_WORLD) && !source.`is`(DamageTypes.GENERIC_KILL)) for (slot in armor) {
            val item = target.getItemBySlot(slot)
            val defense = ReforgeItems.active(item) ?: continue
            val allowed = when (defense.incomingScope) {
                "environment" -> source.entity == null && source.directEntity == null
                "fall" -> source.`is`(DamageTypeTags.IS_FALL)
                "fire" -> source.`is`(DamageTypeTags.IS_FIRE)
                "projectile" -> source.`is`(DamageTypeTags.IS_PROJECTILE)
                else -> true
            }
            if (allowed && GameVersion.armorSlot(item) == slot && condition(defense, target) && (defense.chance == 1.0 || target.random.nextDouble() < defense.chance)) result *= defense.incoming
        }
        return result.toFloat()
    }
    /** Called after accepted damage, using the actual health/absorption loss, never on a rejected hit. */
    @JvmStatic fun afterHit(target: LivingEntity, source: DamageSource, healthLost: Float) {
        val level = target.level() as? ServerLevel ?: return
        if (healthLost <= 0 || !healthLost.isFinite()) return
        val attacker = source.entity as? LivingEntity ?: return
        if (attacker === target || !attacker.isAlive) return
        forEachWeapon(source) { def ->
            if (def.burnSeconds == 0 && def.slowTicks == 0 && def.lifeSteal == 0.0) return@forEachWeapon
            if (!condition(def, attacker, target) || (def.chance < 1.0 && attacker.random.nextDouble() >= def.chance)) return@forEachWeapon
            if (!(attacker as ReforgeCombatState).`luna$tryProc`(level.gameTime, def.procCooldown)) return@forEachWeapon
            if (def.burnSeconds > 0 && target.isAlive && !target.fireImmune()) {
                target.igniteForSeconds(def.burnSeconds.toFloat())
                level.sendParticles(ParticleTypes.FLAME, target.x, target.y + target.bbHeight * 0.5, target.z, 6, 0.2, 0.2, 0.2, 0.01)
            }
            if (def.slowTicks > 0 && target.isAlive) {
                target.addEffect(GameVersion.slowness(def.slowTicks), attacker)
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.x, target.y + target.bbHeight * 0.5, target.z, 8, 0.25, 0.2, 0.25, 0.015)
            }
            if (def.lifeSteal > 0 && attacker.health < attacker.maxHealth) {
                attacker.heal((healthLost * def.lifeSteal).coerceAtMost(1.0).toFloat())
                level.sendParticles(ParticleTypes.HEART, attacker.x, attacker.y + attacker.bbHeight * 0.8, attacker.z, 2, 0.2, 0.1, 0.2, 0.0)
            }
        }
    }
    @JvmStatic fun killed(target: LivingEntity, source: DamageSource) {
        if (GameVersion.isClient(target.level()) || target !is Mob) return
        val player = source.entity as? Player ?: return
        forEachWeapon(source) { reforge ->
            if (reforge.killXp > 0 && condition(reforge, player) && (reforge.chance == 1.0 || player.random.nextDouble() < reforge.chance)) player.giveExperiencePoints(reforge.killXp)
        }
    }
    @JvmStatic fun mined(player: Player, tool: ItemStack) {
        if (GameVersion.isClient(player.level()) || player.abilities.instabuild) return
        for (hand in 0..1) {
            val held = if (hand == 0) tool else player.offhandItem
            val reforge = ReforgeItems.active(held) ?: continue
            if (reforge.miningXp > 0 && condition(reforge, player) && player.random.nextDouble() < reforge.chance) player.giveExperiencePoints(reforge.miningXp)
        }
    }
}
