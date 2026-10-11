package io.github.linlunaire.lunareforged

import io.github.linlunaire.lunareforged.domain.*
import io.github.linlunaire.lunareforged.domain.Target
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.ChatFormatting
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemAttributeModifiers
import java.util.EnumSet

object ReforgeItems {
    private val tags = listOf("swords", "axes", "spears", "pickaxes", "enchantable/bow", "enchantable/crossbow")
        .associateWith { TagKey.create(Registries.ITEM, Id.withDefaultNamespace(it)) }
    private fun tagged(stack: ItemStack, tag: String) = stack.`is`(tags.getValue(tag))
    fun targets(stack: ItemStack): Set<Target> {
        val mask = targetMask(stack)
        if (mask == 0) return emptySet()
        val result = EnumSet.noneOf(Target::class.java)
        for (target in Target.entries) if (mask and (1 shl target.ordinal) != 0) result.add(target)
        return result
    }
    fun canReforge(stack: ItemStack): Boolean = targetMask(stack) != 0

    // Read live components/tags on each call; no item/world cache can become stale on reload.
    private fun targetMask(stack: ItemStack): Int {
        if (stack.isEmpty || stack.count != 1 || !stack.isDamageableItem) return 0
        var mask = 0
        val axe = tagged(stack, "axes")
        if (axe || tagged(stack, "swords") || tagged(stack, "spears") || stack.`is`(Items.MACE)) mask = mask or (1 shl Target.MELEE.ordinal)
        if (axe) mask = mask or (1 shl Target.AXE.ordinal)
        if (stack.`is`(Items.BOW) || stack.`is`(Items.CROSSBOW) || tagged(stack, "enchantable/bow") || tagged(stack, "enchantable/crossbow")) mask = mask or (1 shl Target.BOW.ordinal)
        if (stack.`is`(Items.TRIDENT)) mask = mask or (1 shl Target.TRIDENT.ordinal)
        if (tagged(stack, "pickaxes")) mask = mask or (1 shl Target.PICKAXE.ordinal)
        if (GameVersion.armorSlot(stack) != null) mask = mask or (1 shl Target.ARMOR.ordinal)
        return mask
    }
    fun definition(stack: ItemStack): Reforge? {
        val id = stack.get(LunaContent.reforge) ?: return null
        return ReforgeRules.find(id) ?: stack.get(LunaContent.profile)?.takeIf { it.id == id }
    }
    fun active(stack: ItemStack): Reforge? {
        val def = definition(stack) ?: return null
        var mask = targetMask(stack)
        while (mask != 0) {
            if (Target.entries[Integer.numberOfTrailingZeros(mask)] in def.targets) return def
            mask = mask and (mask - 1)
        }
        return null
    }
    fun name(stack: ItemStack): Component {
        val id = stack.get(LunaContent.reforge) ?: return Component.empty()
        val definition = definition(stack)
        return if (definition != null) name(definition) else Component.literal(id)
    }
    fun name(definition: Reforge): MutableComponent = ReforgeText.parse(if (definition.name.isNotBlank()) definition.name
        else ReforgeText.localized("reforge.luna_reforged.${definition.id}"))

    @JvmStatic fun baseName(stack: ItemStack): Component {
        if (!LunaContent.componentsReady() || !stack.has(LunaContent.reforge)) return stack.hoverName
        return stack.get(DataComponents.CUSTOM_NAME) ?: stack.get(DataComponents.ITEM_NAME) ?: stack.item.getName(stack)
    }

    @JvmStatic fun displayName(stack: ItemStack, original: Component): Component {
        if (!LunaContent.componentsReady()) return original
        val stone = stack.item as? ReforgeStoneItem
        if (stone != null && !stack.has(DataComponents.CUSTOM_NAME)) {
            val def = ReforgeRules.find(stone.reforgeId) ?: return original
            return name(def).append(Component.translatable("item.luna_reforged.stone_suffix").withStyle(ChatFormatting.WHITE))
        }
        if (!stack.has(LunaContent.reforge)) return original
        return Component.empty().append(name(stack)).append(" ").append(original.copy())
    }
    fun times(stack: ItemStack): Int = (stack.get(LunaContent.times) ?: 0).coerceIn(0, 1_000_000)
    fun applyCopy(stack: ItemStack, reforge: Reforge): ItemStack {
        require(reforge.targets.any(targets(stack)::contains))
        val result = stack.copy()
        updateAttributes(result, reforge)
        result.set(LunaContent.reforge, reforge.id)
        if (ReforgeRules.find(reforge.id) == null) result.set(LunaContent.profile, reforge) else result.remove(LunaContent.profile)
        result.set(LunaContent.times, (times(stack) + 1).coerceAtMost(1_000_000))
        return result
    }
    /** Upgrade old saved modifiers when equipped, preserving the affix and paid reforge count. */
    @JvmStatic fun refreshEquipped(stack: ItemStack) {
        if (!LunaContent.componentsReady()) return
        val def = active(stack) ?: return
        updateAttributes(stack, def)
    }
    private fun updateAttributes(stack: ItemStack, reforge: Reforge) {
        val previous = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
        val entries = previous.modifiers().filterNot { it.modifier().id().namespace == LunaContent.MOD_ID }
        var modifiers = GameVersion.copyModifiers(previous, entries)
        val armorSlot = GameVersion.armorSlot(stack)
        fun add(attribute: net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, name: String, value: Double, operation: AttributeModifier.Operation) {
            if (value == 0.0) return
            val slots = armorSlot?.let { listOf(it) } ?: listOf(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.OFFHAND)
            for (slot in slots) modifiers = modifiers.withModifierAdded(attribute,
                AttributeModifier(LunaContent.id("reforge/${slot.serializedName}/$name"), value, operation), EquipmentSlotGroup.bySlot(slot))
        }
        add(Attributes.MOVEMENT_SPEED, "speed", reforge.speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        add(Attributes.ATTACK_SPEED, "attack_speed", reforge.attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        add(Attributes.ATTACK_KNOCKBACK, "knockback", reforge.knockback, AttributeModifier.Operation.ADD_VALUE)
        add(Attributes.ATTACK_KNOCKBACK, "knockback_multiplier", reforge.knockbackMultiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        add(Attributes.BLOCK_BREAK_SPEED, "mining_speed", reforge.miningSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        add(Attributes.KNOCKBACK_RESISTANCE, "knockback_resistance", reforge.knockbackResistance, AttributeModifier.Operation.ADD_VALUE)
        if (modifiers != previous) stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers)
    }
    fun tooltip(stack: ItemStack, lines: MutableList<Component>) {
        val lore = mutableListOf<Component>()
        addLore(stack, lore)
        if (lore.isEmpty()) return
        // Bukkit lore precedes vanilla equipment stats. Preserve existing lore/enchant lines.
        val attributes = lines.indexOfFirst {
            (it.contents as? net.minecraft.network.chat.contents.TranslatableContents)?.key?.startsWith("item.modifiers.") == true
        }
        val index = when {
            attributes > 0 && lines[attributes - 1].string.isEmpty() -> attributes - 1
            attributes >= 0 -> attributes
            else -> lines.size
        }
        lines.addAll(index, lore)
    }
    private fun addLore(stack: ItemStack, lines: MutableList<Component>) {
        val stone = stack.item as? ReforgeStoneItem
        if (stone != null) {
            lines.add(Component.empty())
            lines.add(Component.translatable("tooltip.luna_reforged.stone", name(ReforgeRules.find(stone.reforgeId)!!)).withStyle(ChatFormatting.GRAY))
            return
        }
        val id = stack.get(LunaContent.reforge)
        if (id == null) {
            if (canReforge(stack)) {
                lines.add(Component.empty())
                lines.add(Component.translatable("tooltip.luna_reforged.reforgable").withStyle(ChatFormatting.DARK_GRAY))
            }
            return
        }
        val definition = definition(stack)
        lines.add(Component.empty())
        lines.add(name(stack))
        if (definition == null) lines.add(Component.translatable("tooltip.luna_reforged.unknown").withStyle(ChatFormatting.RED))
        else lines.addAll(ReforgeLore.lines(definition))
        lines.add(Component.translatable("tooltip.luna_reforged.times", times(stack)).withStyle(ChatFormatting.DARK_GRAY))
    }
}
