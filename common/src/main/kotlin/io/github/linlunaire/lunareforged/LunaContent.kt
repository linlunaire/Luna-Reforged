package io.github.linlunaire.lunareforged

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

object LunaContent {
    const val MOD_ID = "luna_reforged"
    lateinit var table: Block
    lateinit var tableItem: Item
    lateinit var menuType: MenuType<ReforgeMenu>
    lateinit var reforge: DataComponentType<String>
    lateinit var times: DataComponentType<Int>
    lateinit var profile: DataComponentType<io.github.linlunaire.lunareforged.domain.Reforge>
    fun componentsReady() = ::reforge.isInitialized
    val stones = linkedMapOf<String, Item>()
    fun id(path: String): Id = Id.fromNamespaceAndPath(MOD_ID, path)
    fun reforgeComponent(): DataComponentType<String> = DataComponentType.builder<String>()
        .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(64)).build()
    fun timesComponent(): DataComponentType<Int> = DataComponentType.builder<Int>()
        .persistent(Codec.intRange(0, 1_000_000)).networkSynchronized(ByteBufCodecs.VAR_INT).build()
    fun profileComponent(): DataComponentType<io.github.linlunaire.lunareforged.domain.Reforge> =
        DataComponentType.builder<io.github.linlunaire.lunareforged.domain.Reforge>()
            .persistent(ReforgeJson.CODEC)
            .networkSynchronized(ByteBufCodecs.stringUtf8(4096).map(ReforgeJson::parse, ReforgeJson::encode)).build()
}

class ReforgeStoneItem(properties: Item.Properties, val reforgeId: String) : Item(properties)
