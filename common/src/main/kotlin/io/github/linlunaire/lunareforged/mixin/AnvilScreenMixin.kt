package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeItems
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(AnvilScreen::class)
abstract class AnvilScreenMixin {
    @Redirect(method = ["slotChanged", "onNameChanged"], at = At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private fun `luna$originalName`(stack: ItemStack): Component = ReforgeItems.baseName(stack)
}