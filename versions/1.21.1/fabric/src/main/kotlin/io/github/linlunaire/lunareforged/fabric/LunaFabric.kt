package io.github.linlunaire.lunareforged.fabric
import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.verification.ClientPreview
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.BlockItem

class LunaFabric : ModInitializer {
    override fun onInitialize() {
        LunaContent.reforge = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LunaContent.id("reforge"), LunaContent.reforgeComponent())
        LunaContent.profile = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LunaContent.id("custom_profile"), LunaContent.profileComponent())
        LunaContent.times = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, LunaContent.id("times_reforged"), LunaContent.timesComponent())
        LunaContent.table = Registry.register(BuiltInRegistries.BLOCK, LunaContent.id("reforging_table"), ReforgeTableBlock(GameVersion.blockProperties()))
        LunaContent.tableItem = Registry.register(BuiltInRegistries.ITEM, LunaContent.id("reforging_table"), BlockItem(LunaContent.table, GameVersion.itemProperties("reforging_table")))
        for (id in listOf("dynamic", "rich", "prospecting")) {
            LunaContent.stones[id] = Registry.register(BuiltInRegistries.ITEM, LunaContent.id(id + "_stone"), ReforgeStoneItem(GameVersion.itemProperties(id + "_stone"), id))
        }
        LunaContent.menuType = Registry.register(BuiltInRegistries.MENU, LunaContent.id("reforging"), MenuType({ id, inventory -> ReforgeMenu(id, inventory) }, FeatureFlags.VANILLA_SET))
        if (java.lang.Boolean.getBoolean("luna_reforged.preview")) net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register { ClientPreview.serverTick(it) }
        ServerLifecycleEvents.SERVER_STARTING.register { LunaConfig.load(FabricLoader.getInstance().configDir) }
        ServerLifecycleEvents.SERVER_STARTED.register { io.github.linlunaire.lunareforged.verification.ServerChecks.onStarted(it) }
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register { it.accept(LunaContent.tableItem) }
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS).register { entries -> LunaContent.stones.values.forEach { entries.accept(it) } }
        ServerLifecycleEvents.SERVER_STOPPED.register { LunaConfig.reset() }
    }
}
