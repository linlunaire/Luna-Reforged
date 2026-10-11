package io.github.linlunaire.lunareforged

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.cos
import kotlin.math.sin

class ReforgeTableBlock(properties: BlockBehaviour.Properties) : Block(properties) {
    override fun codec(): MapCodec<out ReforgeTableBlock> = CODEC
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = SHAPE
    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = BODY

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        // Vanilla random display ticks: no block entity, permanent ticker or world scan.
        if (random.nextInt(3) != 0) return
        val angle = random.nextDouble() * Math.PI * 2
        level.addParticle(ParticleTypes.END_ROD, pos.x + 0.5 + cos(angle) * 0.32, pos.y + 0.95,
            pos.z + 0.5 + sin(angle) * 0.32, 0.0, 0.018, 0.0)
    }

    override fun triggerEvent(state: BlockState, level: Level, pos: BlockPos, id: Int, parameter: Int): Boolean {
        if (id != REFORGE_EVENT) return super.triggerEvent(state, level, pos, id, parameter)
        if (GameVersion.isClient(level)) {
            // One vanilla block-event packet creates the entire local burst.
            val accent = when (parameter) {
                1 -> ParticleTypes.FLAME
                2 -> ParticleTypes.SNOWFLAKE
                3 -> ParticleTypes.HEART
                4 -> ParticleTypes.HAPPY_VILLAGER
                else -> ParticleTypes.REVERSE_PORTAL
            }
            for (i in 0 until 16) {
                val angle = i * Math.PI / 8
                val dx = cos(angle); val dz = sin(angle)
                level.addParticle(ParticleTypes.END_ROD, pos.x + 0.5 + dx * 0.28, pos.y + 1.05,
                    pos.z + 0.5 + dz * 0.28, dx * 0.06, 0.025, dz * 0.06)
                if (i % 2 == 0) level.addParticle(ParticleTypes.ENCHANT, pos.x + 0.5, pos.y + 1.1, pos.z + 0.5, dx * 0.6, 0.5, dz * 0.6)
                if (i % 2 == 0) level.addParticle(accent, pos.x + 0.5 + dx * 0.2, pos.y + 1.0,
                    pos.z + 0.5 + dz * 0.2, dx * 0.025, 0.03, dz * 0.025)
            }
        }
        return true
    }
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!GameVersion.isClient(level)) player.openMenu(SimpleMenuProvider(
            { id, inventory, _ -> ReforgeMenu(id, inventory, ContainerLevelAccess.create(level, pos)) },
            Component.translatable("container.luna_reforged.table")))
        return InteractionResult.SUCCESS
    }
    companion object {
        val CODEC: MapCodec<ReforgeTableBlock> = simpleCodec(::ReforgeTableBlock)
        const val REFORGE_EVENT = 1
        private val BODY: VoxelShape = Shapes.or(box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
            box(4.0, 3.0, 4.0, 12.0, 10.0, 12.0), box(0.0, 10.0, 0.0, 16.0, 12.0, 16.0))
        private val SHAPE: VoxelShape = Shapes.or(BODY, box(2.0, 12.0, 3.0, 14.0, 20.0, 13.0))
    }
}
