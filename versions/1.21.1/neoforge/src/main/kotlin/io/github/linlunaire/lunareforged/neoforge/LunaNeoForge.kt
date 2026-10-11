package io.github.linlunaire.lunareforged.neoforge
import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.verification.ClientPreview
import net.neoforged.fml.common.Mod
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.common.NeoForge
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.flag.FeatureFlags
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.fml.loading.FMLPaths
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem

@Mod(LunaContent.MOD_ID)
class LunaNeoForge(bus: IEventBus) {
    init {
        bus.addListener<RegisterEvent> { event ->
            event.register(Registries.DATA_COMPONENT_TYPE, LunaContent.id("reforge")) { LunaContent.reforgeComponent().also { LunaContent.reforge = it } }
            event.register(Registries.DATA_COMPONENT_TYPE, LunaContent.id("custom_profile")) { LunaContent.profileComponent().also { LunaContent.profile = it } }
            event.register(Registries.DATA_COMPONENT_TYPE, LunaContent.id("times_reforged")) { LunaContent.timesComponent().also { LunaContent.times = it } }
            event.register(Registries.BLOCK, LunaContent.id("reforging_table")) { ReforgeTableBlock(GameVersion.blockProperties()).also { LunaContent.table = it } }
            event.register(Registries.ITEM, LunaContent.id("reforging_table")) { BlockItem(LunaContent.table, GameVersion.itemProperties("reforging_table")).also { LunaContent.tableItem = it } }
            for (id in listOf("dynamic", "rich", "prospecting")) event.register(Registries.ITEM, LunaContent.id(id + "_stone")) {
                ReforgeStoneItem(GameVersion.itemProperties(id + "_stone"), id).also { LunaContent.stones[id] = it }
            }
            event.register(Registries.MENU, LunaContent.id("reforging")) { MenuType({ id, inventory -> ReforgeMenu(id, inventory) }, FeatureFlags.VANILLA_SET).also { LunaContent.menuType = it } }
        }
        if (java.lang.Boolean.getBoolean("luna_reforged.preview")) NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.tick.ServerTickEvent.Post> { ClientPreview.serverTick(it.server) }
        NeoForge.EVENT_BUS.addListener<ServerStartingEvent> { LunaConfig.load(FMLPaths.CONFIGDIR.get()) }
        bus.addListener<net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent> { event ->
            if (event.tabKey == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(LunaContent.tableItem)
            if (event.tabKey == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) LunaContent.stones.values.forEach { event.accept(it) }
        }
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStartedEvent> { io.github.linlunaire.lunareforged.verification.ServerChecks.onStarted(it.server) }
        NeoForge.EVENT_BUS.addListener<ServerStoppedEvent> { LunaConfig.reset() }
    }
}
