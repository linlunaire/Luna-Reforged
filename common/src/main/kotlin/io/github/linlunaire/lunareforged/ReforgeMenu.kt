package io.github.linlunaire.lunareforged

import io.github.linlunaire.lunareforged.domain.ReforgeRules
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/** Vanilla slot packets own inventory movement. Only this server-side method commits a reforge. */
class ReforgeMenu(id: Int, inventory: Inventory, private val access: ContainerLevelAccess = ContainerLevelAccess.NULL) : AbstractContainerMenu(LunaContent.menuType, id) {
    private val owner = inventory.player
    private val data = SimpleContainerData(5)
    private var rollPool = emptyList<io.github.linlunaire.lunareforged.domain.Reforge>()
    private var ready = false
    private var lastClick = Long.MIN_VALUE
    private var committing = false
    val input = object : SimpleContainer(2) { override fun setChanged() { super.setChanged(); if (ready && !committing) slotsChanged(this) } }
    val cost get() = data.get(0)
    val status get() = data.get(1)
    val revision get() = data.get(2)
    val candidateCount get() = data.get(3)
    val resultSequence get() = data.get(4)
    init {
        addSlot(object : Slot(input, 0, 44, ReforgeLayout.INPUT_Y) {
            override fun mayPlace(stack: ItemStack) = ReforgeItems.canReforge(stack)
            override fun getMaxStackSize() = 1
        })
        addSlot(object : Slot(input, 1, 116, ReforgeLayout.INPUT_Y) {
            override fun mayPlace(stack: ItemStack) = catalyst(stack)
        })
        for (row in 0..2) for (column in 0..8) addSlot(Slot(inventory, column + row * 9 + 9, 8 + column * 18, ReforgeLayout.INVENTORY_Y + row * 18))
        for (column in 0..8) addSlot(Slot(inventory, column, 8 + column * 18, ReforgeLayout.HOTBAR_Y))
        addDataSlots(data)
        ready = true
        refresh(true)
    }
    private fun catalyst(stack: ItemStack) = stack.item is ReforgeStoneItem
    private fun refresh(rebuild: Boolean = false) {
        if (GameVersion.isClient(owner.level())) return
        val item = input.getItem(0)
        val stone = input.getItem(1)
        if (rebuild) {
            rollPool = ReforgeCatalog.candidates(ReforgeItems.targets(item), item.get(LunaContent.reforge), (stone.item as? ReforgeStoneItem)?.reforgeId)
            data.set(0, LunaConfig.prices.cost(ReforgeItems.times(item), (stone.item as? ReforgeStoneItem)?.reforgeId))
            data.set(3, rollPool.size)
        }
        data.set(1, when {
            item.isEmpty -> 1
            !ReforgeItems.canReforge(item) -> 2
            !stone.isEmpty && !catalyst(stone) -> 3
            rollPool.isEmpty() -> 4
            !owner.abilities.instabuild && owner.experienceLevel < cost -> 5
            else -> 0
        })
    }
    override fun slotsChanged(container: Container) {
        super.slotsChanged(container)
        if (!GameVersion.isClient(owner.level())) {
            // Container button IDs are a signed byte in 1.21.1. Reserve 1..100.
            data.set(2, revision % 100 + 1)
            refresh(true)
        }
    }
    override fun broadcastChanges() { refresh(); super.broadcastChanges() }
    override fun clickMenuButton(player: Player, buttonId: Int): Boolean {
        if (GameVersion.isClient(player.level()) || player !== owner || player.containerMenu !== this || !stillValid(player) || buttonId != revision) return false
        val tick = player.level().gameTime
        if (lastClick != Long.MIN_VALUE && tick - lastClick < 5) return false
        refresh(true)
        if (status != 0) return false
        val chosen = ReforgeRules.select(rollPool) { player.random.nextInt(it) } ?: return false
        // Prepare before payment; foreign names, enchantments, damage and components survive the copy.
        val replacement = ReforgeItems.applyCopy(input.getItem(0), chosen)
        lastClick = tick
        committing = true
        try {
            if (!player.abilities.instabuild) { player.giveExperienceLevels(-cost); input.getItem(1).shrink(1) }
            input.setItem(0, replacement)
        } finally { committing = false }
        slotsChanged(input)
        data.set(4, resultSequence % 30000 + 1)
        access.execute { level, pos ->
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 0.9f + player.random.nextFloat() * 0.3f)
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.45f, 1.2f)
            level.blockEvent(pos, LunaContent.table, ReforgeTableBlock.REFORGE_EVENT, ReforgeVisuals.tone(chosen))
        }
        broadcastChanges()
        return true
    }
    override fun stillValid(player: Player) = stillValid(access, player, LunaContent.table)
    override fun removed(player: Player) { super.removed(player); if (!GameVersion.isClient(player.level())) clearContainer(player, input) }
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = when {
            index < 2 -> moveItemStackTo(stack, 2, slots.size, true)
            catalyst(stack) -> moveItemStackTo(stack, 1, 2, false)
            ReforgeItems.canReforge(stack) -> moveItemStackTo(stack, 0, 1, false)
            index < 29 -> moveItemStackTo(stack, 29, 38, false)
            else -> moveItemStackTo(stack, 2, 29, false)
        }
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stack)
        return original
    }
}
