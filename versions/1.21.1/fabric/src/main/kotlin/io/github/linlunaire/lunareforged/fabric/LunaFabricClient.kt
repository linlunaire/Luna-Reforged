package io.github.linlunaire.lunareforged.fabric
import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.verification.ClientPreview
import io.github.linlunaire.lunareforged.client.ReforgeScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.screens.MenuScreens
class LunaFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        if (java.lang.Boolean.getBoolean("luna_reforged.preview")) net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register { ClientPreview.clientTick(it) }
        MenuScreens.register(LunaContent.menuType, ::ReforgeScreen)
        ItemTooltipCallback.EVENT.register { stack, _, _, lines -> ReforgeItems.tooltip(stack, lines) }
    }
}
