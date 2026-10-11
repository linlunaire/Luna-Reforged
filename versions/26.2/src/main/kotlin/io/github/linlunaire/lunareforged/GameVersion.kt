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
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.projectile.arrow.AbstractArrow

typealias Id = net.minecraft.resources.Identifier
object GameVersion {
    fun slowness(ticks: Int) = net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, ticks, 0)
    fun isNight(level: Level): Boolean = level.dimensionType().hasSkyLight() && Math.floorMod(level.defaultClockTime, 24000L) in 13000L until 23000L
    fun isClient(level: Level): Boolean = level.isClientSide()
    fun blockProperties(): BlockBehaviour.Properties = BlockBehaviour.Properties.of().strength(4f, 1200f).requiresCorrectToolForDrops().noOcclusion().lightLevel { 7 }.setId(ResourceKey.create(Registries.BLOCK, LunaContent.id("reforging_table")))
    fun itemProperties(path: String): Item.Properties = Item.Properties().setId(ResourceKey.create(Registries.ITEM, LunaContent.id(path)))
    fun armorSlot(stack: ItemStack): EquipmentSlot? = stack.get(DataComponents.EQUIPPABLE)?.slot()?.takeIf { it == EquipmentSlot.HEAD || it == EquipmentSlot.CHEST || it == EquipmentSlot.LEGS || it == EquipmentSlot.FEET }
    fun copyModifiers(original: ItemAttributeModifiers, entries: List<ItemAttributeModifiers.Entry>) = ItemAttributeModifiers(entries)
    fun projectileWeapon(source: DamageSource): ItemStack? = (source.directEntity as? AbstractArrow)?.weaponItem
    fun testMob(level: net.minecraft.world.level.Level) = net.minecraft.world.entity.monster.zombie.Zombie(level)
    fun testArrow(level: net.minecraft.world.level.Level, player: net.minecraft.world.entity.LivingEntity, bow: ItemStack) = net.minecraft.world.entity.projectile.arrow.Arrow(level, player, ItemStack(net.minecraft.world.item.Items.ARROW), bow)
    fun hurt(target: net.minecraft.world.entity.LivingEntity, level: net.minecraft.server.level.ServerLevel, source: DamageSource, amount: Float): Boolean = target.hurtServer(level, source, amount)
}
