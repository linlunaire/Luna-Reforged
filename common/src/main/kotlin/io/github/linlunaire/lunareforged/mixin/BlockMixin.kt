package io.github.linlunaire.lunareforged.mixin

import io.github.linlunaire.lunareforged.ReforgeEffects
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Block::class)
abstract class BlockMixin {
    @Inject(method = ["playerDestroy"], at = [At("TAIL")])
    private fun `luna$mined`(level: Level, player: Player, pos: BlockPos, state: BlockState, entity: BlockEntity?, tool: ItemStack, ci: CallbackInfo) {
        ReforgeEffects.mined(player, tool)
    }
}