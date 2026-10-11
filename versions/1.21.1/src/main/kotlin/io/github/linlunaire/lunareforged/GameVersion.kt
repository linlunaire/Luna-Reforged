package io.github.linlunaire.lunareforged
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.entity.projectile.AbstractArrow

typealias Id = net.minecraft.resources.ResourceLocation
object GameVersion {
    fun slowness(ticks: Int) = net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 0)
    fun isNight(level: Level): Boolean = level.dimensionType().hasSkyLight() && Math.floorMod(level.dayTime, 24000L) in 13000L until 23000L
    fun isClient(level: Level): Boolean = level.isClientSide
    fun blockProperties(): BlockBehaviour.Properties = BlockBehaviour.Properties.of().strength(4f, 1200f).requiresCorrectToolForDrops().noOcclusion().lightLevel { 7 }
    fun itemProperties(path: String): Item.Properties = Item.Properties()
    fun armorSlot(stack: ItemStack): EquipmentSlot? = (stack.item as? ArmorItem)?.equipmentSlot
    fun copyModifiers(original: ItemAttributeModifiers, entries: List<ItemAttributeModifiers.Entry>) = ItemAttributeModifiers(entries, original.showInTooltip())
    fun projectileWeapon(source: DamageSource): ItemStack? = (source.directEntity as? AbstractArrow)?.weaponItem
    fun testMob(level: net.minecraft.world.level.Level) = net.minecraft.world.entity.monster.Zombie(level)
    fun testArrow(level: net.minecraft.world.level.Level, player: net.minecraft.world.entity.LivingEntity, bow: ItemStack) = net.minecraft.world.entity.projectile.Arrow(level, player, ItemStack(net.minecraft.world.item.Items.ARROW), bow)
    fun hurt(target: net.minecraft.world.entity.LivingEntity, level: net.minecraft.server.level.ServerLevel, source: DamageSource, amount: Float): Boolean = target.hurt(source, amount)
}
