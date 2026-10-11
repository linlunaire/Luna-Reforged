package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.server.level.ServerLevel
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(LivingEntity::class)
abstract class LivingEntityMixin : io.github.linlunaire.lunareforged.ReforgeCombatState {
    @field:Unique private var `luna$rewarded` = false
    @field:Unique private var `luna$nextProcTick` = Long.MIN_VALUE
    @field:Unique private var `luna$healthBefore` = 0f

    @ModifyVariable(method = ["hurtServer"], at = At("HEAD"), argsOnly = true, ordinal = 0)
    private fun `luna$damage`(amount: Float, level: ServerLevel, source: DamageSource, original: Float): Float =
        ReforgeEffects.damage(this as Any as LivingEntity, source, amount)

    override fun `luna$tryProc`(tick: Long, cooldown: Int): Boolean {
        if (tick < `luna$nextProcTick`) return false
        `luna$nextProcTick` = tick + cooldown
        return true
    }

    @Inject(method = ["hurtServer"], at = [At("HEAD")])
    private fun `luna$beforeHit`(level: ServerLevel, source: DamageSource, amount: Float, cir: CallbackInfoReturnable<Boolean>) {
        val self = this as Any as LivingEntity
        `luna$healthBefore` = self.health + self.absorptionAmount
    }

    @Inject(method = ["hurtServer"], at = [At("RETURN")])
    private fun `luna$afterHit`(level: ServerLevel, source: DamageSource, amount: Float, cir: CallbackInfoReturnable<Boolean>) {
        val self = this as Any as LivingEntity
        if (cir.returnValueZ) ReforgeEffects.afterHit(self, source, `luna$healthBefore` - self.health - self.absorptionAmount)
    }

    @Inject(method = ["die"], at = [At("TAIL")])
    private fun `luna$killed`(source: DamageSource, ci: CallbackInfo) {
        if (!`luna$rewarded`) {
            `luna$rewarded` = true
            ReforgeEffects.killed(this as Any as LivingEntity, source)
        }
    }
}