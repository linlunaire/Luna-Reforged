package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeItems
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/** The decorative prefix must never become an anvil rename or a saved custom name. */
@Mixin(AnvilMenu::class)
abstract class AnvilMenuMixin {
    @Redirect(method = ["createResult", "onTake"], at = At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private fun `luna$originalName`(stack: ItemStack): Component = ReforgeItems.baseName(stack)
}