package io.github.linlunaire.lunareforged.verification

import com.mojang.authlib.GameProfile
import com.mojang.serialization.JsonOps
import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.domain.ReforgeRules
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.Connection
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.util.UUID
import kotlin.math.abs

/** Opt-in, isolated development-world checks: never runs without -Dluna_reforged.verify=true. */
object ServerChecks {
    @JvmStatic fun onStarted(server: MinecraftServer) {
        if (!java.lang.Boolean.getBoolean("luna_reforged.verify")) return
        server.execute {
            try { run(server); println("LUNA_REFORGED_SERVER_CHECKS_PASSED") }
            catch (error: Throwable) { System.err.println("LUNA_REFORGED_SERVER_CHECKS_FAILED"); error.printStackTrace() }
            finally { server.halt(false) }
        }
    }
    private fun run(server: MinecraftServer) {
        LunaConfig.reset()
        val level = server.overworld()
        val profile = GameProfile(UUID.fromString("c9e2bbee-00fe-40af-b53d-dd8b624d56c1"), "LunaVerifier")
        val player = object : ServerPlayer(server, level, profile, ClientInformation.createDefault()) {
            override fun hasDisconnected() = false
        }
        player.connection = ServerGamePacketListenerImpl(server, Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false))
        val pos = BlockPos(0, 100, 0)
        level.setBlock(pos, LunaContent.table.defaultBlockState(), 3)
        player.setPos(0.5, 101.0, 0.5)
        val sword = ItemStack(Items.DIAMOND_SWORD)
        checkItemEligibility()
        sword.damageValue = 42
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Preserve my name"))
        val originalModifiers = sword.get(DataComponents.ATTRIBUTE_MODIFIERS)
        val light = ReforgeItems.applyCopy(sword, ReforgeRules.find("light")!!)
        val sharp = ReforgeItems.applyCopy(light, ReforgeRules.find("sharp")!!)
        check(sharp.get(DataComponents.ATTRIBUTE_MODIFIERS)!!.modifiers() == originalModifiers!!.modifiers()) { "Stale modifier or lost vanilla damage" }
        check(sharp.damageValue == 42 && sharp.get(DataComponents.CUSTOM_NAME) == sword.get(DataComponents.CUSTOM_NAME))
        check(sharp.hoverName.string == "Sharp Preserve my name") { "Display prefix is missing or duplicated: ${sharp.hoverName.string}" }
        check(sharp.hoverName.string == sharp.hoverName.string && ReforgeItems.baseName(sharp).string == "Preserve my name")
        val nameColors = mutableListOf<Int>()
        val gradient = ReforgeText.parse("<gradient:#FF0000>月🌙光</gradient:#0000FF>")
        gradient.visit(net.minecraft.network.chat.FormattedText.StyledContentConsumer<Unit> { style, text ->
            if (text.isNotEmpty()) nameColors.add(style.color!!.value)
            java.util.Optional.empty()
        }, net.minecraft.network.chat.Style.EMPTY)
        check(gradient.string == "月🌙光" && nameColors == listOf(0xFF0000, 0x800080, 0x0000FF)) { "Gradient endpoints or Unicode interpolation failed" }
        check(ReforgeText.parse("&a+3% &fDamage").string == "+3% Damage")
        for (def in ReforgeRules.definitions) {
            val lore = ReforgeLore.lines(def)
            check(lore.isNotEmpty()) { "Missing mechanics description: ${def.id}" }
            for (line in lore.filter { it.style.color?.value != 0xAAAAAA }) {
                check(line.string.startsWith("+") || line.string.startsWith("-")) { "Value is not first: ${def.id}: ${line.string}" }
                val expected = if (line.string.startsWith("+")) 0x55FF55 else 0xFF5555
                check(line.style.color?.value == expected) { "Incorrect benefit/penalty color: ${def.id}" }
                check(!line.string.contains("stat.luna_reforged") && !line.string.contains("condition.luna_reforged")) { "Untranslated mechanics: ${def.id}" }
            }
            check(ReforgeJson.parse(ReforgeJson.encode(def)) == def) { "New definition fields lost: ${def.id}" }
        }
        val anvil = net.minecraft.world.inventory.AnvilMenu(2, player.inventory)
        anvil.getSlot(0).set(sharp.copy())
        anvil.setItemName("Renamed")
        val renamed = anvil.getSlot(2).item
        check(!renamed.isEmpty && renamed.get(DataComponents.CUSTOM_NAME)?.string == "Renamed") { "Anvil rename failed" }
        check(renamed.hoverName.string == "Sharp Renamed")
        anvil.setItemName("Preserve my name")
        check(anvil.getSlot(2).item.isEmpty) { "Decorative prefix added an unwanted rename cost" }
        val tooltip = mutableListOf<Component>()
        ReforgeItems.tooltip(light, tooltip)
        check(tooltip.map { it.string }.containsAll(listOf("Light", "-5% Damage", "-5% Attack Knockback", "+10% Attack Speed")))
        check(light.get(DataComponents.ATTRIBUTE_MODIFIERS)!!.modifiers().any {
            it.attribute() == Attributes.ATTACK_KNOCKBACK && it.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && abs(it.modifier().amount() + 0.05) < 0.0001
        }) { "Knockback must multiply the attribute instead of adding a flat amount" }
        val legacy = sword.copy().also {
            it.set(LunaContent.reforge, "strong")
            it.set(LunaContent.times, 7)
            it.set(DataComponents.ATTRIBUTE_MODIFIERS, originalModifiers!!.withModifierAdded(Attributes.ATTACK_KNOCKBACK,
                net.minecraft.world.entity.ai.attributes.AttributeModifier(LunaContent.id("reforge/mainhand/knockback"), 0.1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND))
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, legacy)
        check(legacy.get(DataComponents.ATTRIBUTE_MODIFIERS)!!.modifiers().none { it.modifier().id() == LunaContent.id("reforge/mainhand/knockback") }) { "Legacy flat knockback survived equipment migration" }
        check(ReforgeItems.times(legacy) == 7 && legacy.damageValue == 42)
        check(legacy.get(DataComponents.CUSTOM_NAME) == sword.get(DataComponents.CUSTOM_NAME))
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        check(ReforgeItems.times(sharp) == 2 && sword.get(LunaContent.reforge) == null)
        val ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE)
        val encoded = ItemStack.CODEC.encodeStart(ops, sharp).getOrThrow()
        val restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow()
        check(ItemStack.matches(sharp, restored)) { "Save/load component roundtrip failed" }
        val buffer = net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess())
        try {
            ItemStack.STREAM_CODEC.encode(buffer, sharp)
            check(ItemStack.matches(sharp, ItemStack.STREAM_CODEC.decode(buffer))) { "Network component roundtrip failed" }
        } finally { buffer.release() }
        val unknown = restored.copy().also { it.set(LunaContent.reforge, "future_affix") }
        check(ReforgeItems.definition(unknown) == null && unknown.get(LunaContent.reforge) == "future_affix")

        val custom = ReforgeJson.parse("""{"id":"starforged","name":"星铸","description":["伤害 +6%"],"targets":["melee"],"damage":1.06}""")
        check(ReforgeJson.parse(ReforgeJson.encode(custom)) == custom)
        val overlong = custom.copy(name = "\\".repeat(512), description = List(4) { "\\".repeat(512) })
        check(runCatching { ReforgeJson.encode(overlong) }.isFailure) { "A profile must fit its native network component" }
        for (bad in listOf("""{"id":"bad","targets":["melee"],"damage":-1}""",
            """{"id":"bad","targets":["melee"],"killXp":1.2}""",
            """{"id":"bad","targets":["melee"],"effects":[]}""",
            """{"id":"bad","targets":["melee"],"damage":"1.2"}""")) {
            check(runCatching { ReforgeJson.parse(bad) }.isFailure) { "Invalid schema accepted: $bad" }
        }
        val customItem = ReforgeItems.applyCopy(sword, custom)
        val customRestored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, customItem).getOrThrow()).getOrThrow()
        check(ItemStack.matches(customItem, customRestored))
        val customBuffer = net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess())
        try {
            ItemStack.STREAM_CODEC.encode(customBuffer, customItem)
            val synced = ItemStack.STREAM_CODEC.decode(customBuffer)
            check(ItemStack.matches(customItem, synced) && ReforgeItems.name(synced).string == "星铸")
        } finally { customBuffer.release() }
        check(ReforgeItems.applyCopy(customItem, ReforgeRules.find("sharp")!!).get(LunaContent.profile) == null)
        check(ReforgeItems.definition(customItem.copy().also { it.set(LunaContent.reforge, "other_custom") }) == null)
        val temporary = java.nio.file.Files.createTempDirectory("luna-reforged-verify-")
        val definitions = temporary.resolve("luna-reforged/reforges")
        val customPath = definitions.resolve("starforged.json")
        val invalidPath = definitions.resolve("bad.json")
        val configPath = temporary.resolve("luna-reforged.json")
        try {
            ReforgeCatalog.load(temporary)
            check(ReforgeCatalog.candidates(setOf(io.github.linlunaire.lunareforged.domain.Target.MELEE), null, null).none { it.id == "starforged" }) { "Example loaded unexpectedly" }
            java.nio.file.Files.writeString(customPath, ReforgeJson.encode(custom))
            ReforgeCatalog.load(temporary)
            check(ReforgeCatalog.candidates(custom.targets, null, null).any { it == custom })
            java.nio.file.Files.writeString(invalidPath, """{"id":"bad","targets":["invalid"]}""")
            check(runCatching { ReforgeCatalog.load(temporary) }.isFailure)
            check(ReforgeCatalog.candidates(custom.targets, null, null).any { it == custom }) { "Failed load partially replaced catalog" }
            java.nio.file.Files.delete(invalidPath)
            java.nio.file.Files.writeString(configPath, """{"includeNativeReforges":false}""")
            LunaConfig.load(temporary)
            val originalPool = ReforgeCatalog.candidates(custom.targets, null, null)
            check(originalPool.any { it.id == "sharp" } && originalPool.any { it.id == "starforged" } && originalPool.none { it.id == "duelist" })
            check(ReforgeCatalog.candidates(setOf(io.github.linlunaire.lunareforged.domain.Target.PICKAXE), null, null).single().id == "prospecting")
            val oldPrices = LunaConfig.prices
            for (invalidConfig in listOf(
                """{"baseLevels":7,"includeNativeReforges":"bad"}""",
                """{"baseLevels":"7"}""",
                """{"growth":"1.5"}"""
            )) {
                java.nio.file.Files.writeString(configPath, invalidConfig)
                check(runCatching { LunaConfig.load(temporary) }.isFailure)
                check(LunaConfig.prices === oldPrices && !LunaConfig.includeNativeReforges) { "Rejected scalar settings partially replaced live config" }
                check(ReforgeCatalog.candidates(custom.targets, null, null) == originalPool)
            }
            java.nio.file.Files.writeString(configPath, """{"baseLevels":7,"includeNativeReforges":true}""")
            java.nio.file.Files.writeString(invalidPath, """{"id":"bad","targets":["invalid"]}""")
            check(runCatching { LunaConfig.load(temporary) }.isFailure)
            check(LunaConfig.prices === oldPrices && !LunaConfig.includeNativeReforges) { "Rejected catalog partially replaced prices or native-affix policy" }
            check(ReforgeCatalog.candidates(custom.targets, null, null) == originalPool)
            java.nio.file.Files.delete(invalidPath)
            LunaConfig.load(temporary)
            check(LunaConfig.prices.baseLevels == 7 && LunaConfig.includeNativeReforges)
            println("LUNA_REFORGED_CONFIG_TRANSACTION_PASSED")
            ReforgeCatalog.reset()
            check(ReforgeItems.active(customRestored) == custom) { "Saved custom profile must survive a catalog removal" }
        } finally {
            LunaConfig.reset()
            for (path in listOf(configPath, invalidPath, customPath, definitions.resolve("_example.json"), definitions, temporary.resolve("luna-reforged"), temporary)) java.nio.file.Files.deleteIfExists(path)
        }

        fun menu(): ReforgeMenu = ReforgeMenu(1, player.inventory, ContainerLevelAccess.create(level, pos)).also { player.containerMenu = it }
        val menu = menu()
        menu.input.setItem(0, sword.copy())
        player.experienceLevel = 0
        check(!menu.clickMenuButton(player, menu.revision))
        check(menu.input.getItem(1).isEmpty && menu.input.getItem(0).get(LunaContent.reforge) == null)
        player.experienceLevel = 100
        val revision = menu.revision
        check(!menu.clickMenuButton(player, revision + 1))
        check(menu.clickMenuButton(player, revision)) { "Valid reforge was rejected" }
        check(menu.resultSequence == 1)
        check(player.experienceLevel == 97 && menu.input.getItem(1).isEmpty && ReforgeItems.times(menu.input.getItem(0)) == 1)
        check(!menu.clickMenuButton(player, revision)) { "Duplicate packet charged twice" }
        check(menu.resultSequence == 1) { "Rejected replay emitted a success receipt" }
        check(player.experienceLevel == 97)
        check(!menu.quickMoveStack(player, 0).isEmpty && menu.input.getItem(0).isEmpty)
        menu.removed(player)
        check(menu.input.isEmpty)
        check((0 until player.inventory.containerSize).sumOf { player.inventory.getItem(it).count } == 1) { "Close/shift movement lost or duplicated items" }
        player.inventory.clearContent()
        val wrong = menu()
        wrong.input.setItem(0, ItemStack(Items.DIAMOND_HELMET))
        wrong.input.setItem(1, ItemStack(LunaContent.stones.getValue("dynamic")))
        check(!wrong.clickMenuButton(player, wrong.revision))
        check(wrong.input.getItem(1).count == 1 && player.experienceLevel == 97)
        wrong.removed(player)
        player.inventory.clearContent()
        val stone = menu()
        stone.input.setItem(0, ItemStack(Items.DIAMOND_SWORD))
        stone.input.setItem(1, ItemStack(LunaContent.stones.getValue("dynamic"), 2))
        check(stone.clickMenuButton(player, stone.revision))
        check(stone.input.getItem(0).get(LunaContent.reforge) == "dynamic" && player.experienceLevel == 57)
        check(stone.candidateCount == 1 && stone.cost == 40) { "Stones must allow the same reforge and retain a fixed price" }
        check(!stone.clickMenuButton(player, stone.revision))
        stone.removed(player)
        player.inventory.clearContent()
        val repeatedStone = menu()
        repeatedStone.input.setItem(0, ReforgeItems.applyCopy(sword, ReforgeRules.find("dynamic")!!))
        repeatedStone.input.setItem(1, ItemStack(LunaContent.stones.getValue("dynamic")))
        check(repeatedStone.clickMenuButton(player, repeatedStone.revision))
        check(player.experienceLevel == 17 && ReforgeItems.times(repeatedStone.input.getItem(0)) == 2 && repeatedStone.input.getItem(1).isEmpty)
        repeatedStone.removed(player)
        player.inventory.clearContent()
        val richStone = menu()
        richStone.input.setItem(0, ItemStack(Items.BOW).also { it.set(LunaContent.times, 100) })
        richStone.input.setItem(1, ItemStack(LunaContent.stones.getValue("rich")))
        check(richStone.cost == 3 && richStone.clickMenuButton(player, richStone.revision) && player.experienceLevel == 14)
        richStone.removed(player)
        player.inventory.clearContent()
        val broken = menu()
        broken.input.setItem(0, sword.copy())
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
        check(!broken.stillValid(player) && !broken.clickMenuButton(player, broken.revision))
        broken.removed(player)

        val target = GameVersion.testMob(level)
        target.getAttribute(Attributes.ARMOR)!!.baseValue = 0.0
        player.setItemSlot(EquipmentSlot.MAINHAND, sharp)
        val source = player.damageSources().playerAttack(player)
        check(abs(ReforgeEffects.damage(target, source, 10f) - 10.3f) < 0.001)
        check(GameVersion.hurt(target, level, source, 10f))
        check(abs(target.health - 9.7f) < 0.01) { "Damage Mixin did not apply exactly once: health=${target.health}" }
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        target.setItemSlot(EquipmentSlot.HEAD, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_HELMET), ReforgeRules.find("reinforced")!!))
        check(ReforgeEffects.damage(target, source, 10f) == 10f) { "Upstream take_damage must not apply to entity attacks" }
        check(abs(ReforgeEffects.damage(target, target.damageSources().fall(), 10f) - 9.6f) < 0.001) { "Environmental damage must include damage that bypasses armor" }
        check(ReforgeEffects.damage(target, target.damageSources().fellOutOfWorld(), 10f) == 10f)
        target.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
        check(ReforgeEffects.damage(target, source, 10f) == 10f)
        player.setItemSlot(EquipmentSlot.MAINHAND, customRestored)
        check(abs(ReforgeEffects.damage(target, source, 10f) - 10.6f) < 0.001)
        player.setItemSlot(EquipmentSlot.MAINHAND, ReforgeItems.applyCopy(sword, ReforgeRules.find("gravitated")!!))
        player.setDeltaMovement(0.0, -0.11, 0.0)
        check(abs(ReforgeEffects.damage(target, source, 10f) - 10.8f) < 0.001)
        player.setDeltaMovement(0.0, -0.1, 0.0)
        check(ReforgeEffects.damage(target, source, 10f) == 10f)
        player.setDeltaMovement(0.0, 0.0, 0.0)
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        player.setItemSlot(EquipmentSlot.OFFHAND, sharp)
        check(abs(ReforgeEffects.damage(target, source, 10f) - 10.3f) < 0.001) { "Melee reforge in offhand did not activate" }
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY)
        player.setItemSlot(EquipmentSlot.MAINHAND, ReforgeItems.applyCopy(sword, ReforgeRules.find("desperate")!!))
        player.health = player.maxHealth
        check(ReforgeEffects.damage(target, source, 10f) == 10f)
        player.health = player.maxHealth * 0.4f
        check(abs(ReforgeEffects.damage(target, source, 10f) - 11.6f) < 0.001)
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        target.setItemSlot(EquipmentSlot.HEAD, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_HELMET), ReforgeRules.find("warded")!!))
        target.health = target.maxHealth
        check(ReforgeEffects.damage(target, source, 10f) == 10f)
        target.health = target.maxHealth * 0.4f
        check(abs(ReforgeEffects.damage(target, source, 10f) - 9f) < 0.001)
        target.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)
        val mining = ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_PICKAXE), ReforgeRules.find("resonant")!!)
        check(mining.get(DataComponents.ATTRIBUTE_MODIFIERS)!!.modifiers().any { it.attribute() == Attributes.BLOCK_BREAK_SPEED && abs(it.modifier().amount() - 0.08) < 0.00001 })
        val scholar = ReforgeItems.applyCopy(mining, ReforgeRules.find("scholar")!!)
        check(scholar.get(DataComponents.ATTRIBUTE_MODIFIERS)!!.modifiers().none { it.modifier().id().path.endsWith("mining_speed") })
        val bow = ReforgeItems.applyCopy(ItemStack(Items.BOW), ReforgeRules.find("streamlined")!!)
        player.setItemSlot(EquipmentSlot.MAINHAND, bow)
        check(ReforgeEffects.damage(target, source, 10f) == 10f) { "Arrow bonus must not apply to hitting with a bow" }
        val arrow = GameVersion.testArrow(level, player, bow)
        bow.set(LunaContent.reforge, "rich")
        val arrowSource = player.damageSources().arrow(arrow, player)
        check(abs(ReforgeEffects.damage(target, arrowSource, 10f) - 10.4f) < 0.001) { "Arrow must retain its firing weapon snapshot" }
        val richArrow = GameVersion.testArrow(level, player, bow)
        val killSource = player.damageSources().arrow(richArrow, player)
        val victim = GameVersion.testMob(level)
        val xpBefore = player.totalExperience
        victim.health = 0f
        victim.die(killSource)
        victim.die(killSource)
        check(player.totalExperience == xpBefore + 5) { "Kill XP missing or duplicated" }
        checkNewMechanics(level)
        println("Verified: gradient RGB and Unicode, display prefix and anvil rename, legacy equipment migration, original-pool config, empty-slot random reforging, fixed-price and repeat stones, save/network profiles, receipts and replay rejection, falling-attack threshold, offhand, environmental defense and void exclusion, actual damage Mixin, conditions, attributes, projectile snapshot and exactly-once kill XP")
    }

    private fun checkNewMechanics(level: net.minecraft.server.level.ServerLevel) {
        fun mob() = GameVersion.testMob(level).also { it.getAttribute(Attributes.ARMOR)!!.baseValue = 0.0 }
        fun attacker(id: String, item: net.minecraft.world.item.Item = Items.DIAMOND_SWORD) = mob().also {
            it.setItemSlot(EquipmentSlot.MAINHAND, ReforgeItems.applyCopy(ItemStack(item), ReforgeRules.find(id)!!))
        }
        val burner = attacker("ember")
        val victim = mob()
        victim.setInvulnerable(true)
        check(!GameVersion.hurt(victim, level, burner.damageSources().mobAttack(burner), 4f))
        check(!victim.isOnFire) { "Rejected hit produced a combat effect" }
        victim.setInvulnerable(false)
        check(GameVersion.hurt(victim, level, burner.damageSources().mobAttack(burner), 4f))
        check(victim.remainingFireTicks >= 60) { "Accepted hit did not ignite the target" }
        val second = mob()
        check(GameVersion.hurt(second, level, burner.damageSources().mobAttack(burner), 4f))
        check(!second.isOnFire) { "Shared cooldown was bypassed by hitting another target" }

        val archer = attacker("frostbound", Items.BOW)
        val frozen = mob()
        val arrow = GameVersion.testArrow(level, archer, archer.mainHandItem)
        archer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        check(GameVersion.hurt(frozen, level, archer.damageSources().arrow(arrow, archer), 4f))
        check(frozen.hasEffect(GameVersion.slowness(40).effect)) { "Projectile snapshot lost its frost effect" }
        check(frozen.getEffect(GameVersion.slowness(40).effect)!!.duration == 40)

        val siphon = attacker("siphoning").also { it.health = 10f }
        val prey = mob()
        check(GameVersion.hurt(prey, level, siphon.damageSources().mobAttack(siphon), 10f))
        check(abs(siphon.health - 10.5f) < 0.001) { "Life steal must use actual accepted damage" }
        val secondPrey = mob()
        check(GameVersion.hurt(secondPrey, level, siphon.damageSources().mobAttack(siphon), 10f))
        check(abs(siphon.health - 10.5f) < 0.001) { "Life steal ignored cooldown" }
        check((siphon as ReforgeCombatState).`luna$tryProc`(level.gameTime + 20, 20)) { "Cooldown did not expire" }

        val finisher = attacker("executioner")
        val weak = mob()
        val strike = finisher.damageSources().mobAttack(finisher)
        check(ReforgeEffects.damage(weak, strike, 10f) == 10f)
        weak.health = weak.maxHealth * 0.3f
        check(abs(ReforgeEffects.damage(weak, strike, 10f) - 11.2f) < 0.001)

        val distant = attacker("farshot", Items.BOW)
        val farTarget = mob()
        distant.setPos(0.0, 100.0, 0.0); farTarget.setPos(12.0, 100.0, 0.0)
        val farArrow = GameVersion.testArrow(level, distant, distant.mainHandItem)
        val ranged = distant.damageSources().arrow(farArrow, distant)
        check(abs(ReforgeEffects.damage(farTarget, ranged, 10f) - 11.2f) < 0.001)
        farTarget.setPos(11.9, 100.0, 0.0)
        check(ReforgeEffects.damage(farTarget, ranged, 10f) == 10f)

        val protected = mob()
        fun armor(id: String) { protected.setItemSlot(EquipmentSlot.CHEST, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_CHESTPLATE), ReforgeRules.find(id)!!)) }
        armor("feathered")
        check(abs(ReforgeEffects.damage(protected, protected.damageSources().fall(), 10f) - 8.6f) < 0.001)
        check(ReforgeEffects.damage(protected, strike, 10f) == 10f)
        armor("cinderward")
        check(abs(ReforgeEffects.damage(protected, protected.damageSources().onFire(), 10f) - 8.8f) < 0.001)
        check(ReforgeEffects.damage(protected, protected.damageSources().fall(), 10f) == 10f)
        armor("bulwark")
        val plainArrow = GameVersion.testArrow(level, distant, ItemStack(Items.BOW))
        val plainRanged = distant.damageSources().arrow(plainArrow, distant)
        check(abs(ReforgeEffects.damage(protected, plainRanged, 10f) - 9.2f) < 0.001)
        check(ReforgeEffects.damage(protected, strike, 10f) == 10f)
        check(ReforgeEffects.damage(protected, protected.damageSources().fellOutOfWorld(), 10f) == 10f)
        check(ReforgeRules.definitions.map(ReforgeVisuals::tone).all { it in 0..4 })
        println("Verified: 44 numeric-first colored descriptions, accepted-hit ignite/frost/life-steal, projectile proc snapshot, shared cooldown and expiry, distance/health thresholds and fall/fire/projectile defense scopes")
    }

    private fun checkItemEligibility() {
        val melee = io.github.linlunaire.lunareforged.domain.Target.MELEE
        val cases = listOf(
            Items.DIAMOND_SWORD to setOf(melee),
            Items.DIAMOND_AXE to setOf(melee, io.github.linlunaire.lunareforged.domain.Target.AXE),
            Items.BOW to setOf(io.github.linlunaire.lunareforged.domain.Target.BOW),
            Items.CROSSBOW to setOf(io.github.linlunaire.lunareforged.domain.Target.BOW),
            Items.TRIDENT to setOf(io.github.linlunaire.lunareforged.domain.Target.TRIDENT),
            Items.DIAMOND_PICKAXE to setOf(io.github.linlunaire.lunareforged.domain.Target.PICKAXE),
            Items.DIAMOND_BOOTS to setOf(io.github.linlunaire.lunareforged.domain.Target.ARMOR),
            Items.DIAMOND_CHESTPLATE to setOf(io.github.linlunaire.lunareforged.domain.Target.ARMOR),
            Items.BOOK to emptySet()
        )
        for ((item, expected) in cases) {
            val stack = ItemStack(item)
            check(ReforgeItems.targets(stack) == expected && ReforgeItems.canReforge(stack) == expected.isNotEmpty()) { "Changed target classification: $item" }
            for (definition in ReforgeRules.definitions) {
                stack.set(LunaContent.reforge, definition.id)
                val applies = expected.any(definition.targets::contains)
                check(ReforgeItems.active(stack) == definition.takeIf { applies }) { "Changed active affix: $item / ${definition.id}" }
            }
            stack.count = 2
            check(!ReforgeItems.canReforge(stack) && ReforgeItems.active(stack) == null) { "Stacked equipment was accepted" }
        }
        check(!ReforgeItems.canReforge(ItemStack.EMPTY))
        val active = ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), ReforgeRules.find("sharp")!!)
        val bean = java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        bean.isThreadAllocatedMemoryEnabled = true
        repeat(100_000) { check(ReforgeItems.active(active)?.id == "sharp") }
        val thread = Thread.currentThread().threadId()
        val before = bean.getThreadAllocatedBytes(thread)
        repeat(200_000) { check(ReforgeItems.active(active)?.id == "sharp") }
        val allocated = bean.getThreadAllocatedBytes(thread) - before
        check(allocated < 16_384) { "Active-affix lookup introduced per-call allocation: $allocated bytes" }
        println("LUNA_REFORGED_ACTIVE_ALLOCATION: $allocated bytes / 200000 lookups")
        println("LUNA_REFORGED_ITEM_ELIGIBILITY_PASSED")
    }
}
