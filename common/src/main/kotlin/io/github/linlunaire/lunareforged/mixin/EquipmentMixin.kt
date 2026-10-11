package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeItems
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntity::class)
abstract class EquipmentMixin {
    @Inject(method = ["onEquipItem"], at = [At("HEAD")])
    private fun `luna$upgrade`(slot: EquipmentSlot, oldStack: ItemStack, newStack: ItemStack, ci: CallbackInfo) {
        ReforgeItems.refreshEquipped(newStack)
    }
}