package io.github.linlunaire.lunareforged.neoforge
import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.verification.ClientPreview
import io.github.linlunaire.lunareforged.client.ReforgeScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent

@EventBusSubscriber(modid = LunaContent.MOD_ID, value = [Dist.CLIENT])
object LunaNeoForgeScreens {
    @JvmStatic @SubscribeEvent fun screens(event: RegisterMenuScreensEvent) { event.register(LunaContent.menuType, ::ReforgeScreen)
        if (java.lang.Boolean.getBoolean("luna_reforged.preview")) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.client.event.ClientTickEvent.Post> { ClientPreview.clientTick(net.minecraft.client.Minecraft.getInstance()) } }
}
@EventBusSubscriber(modid = LunaContent.MOD_ID, value = [Dist.CLIENT])
object LunaNeoForgeTooltips {
    @JvmStatic @SubscribeEvent fun tooltip(event: ItemTooltipEvent) { ReforgeItems.tooltip(event.itemStack, event.toolTip) }
}
