package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeItems
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/** NeoForge 26.2 moves vanilla's rename calculation into this patched method. */
@Mixin(AnvilMenu::class)
abstract class AnvilMenuNeoForgeMixin {
    @Redirect(method = ["createResultInternal"], at = At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private fun `luna$originalName`(stack: ItemStack): Component = ReforgeItems.baseName(stack)
}