package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeItems
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ItemStack::class)
abstract class ItemStackMixin {
    @Inject(method = ["getHoverName"], at = [At("RETURN")], cancellable = true)
    private fun `luna$name`(cir: CallbackInfoReturnable<Component>) {
        cir.returnValue = ReforgeItems.displayName(this as Any as ItemStack, cir.returnValue)
    }
}