package io.github.linlunaire.lunareforged.verification

import io.github.linlunaire.lunareforged.*
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

/** Explicit development-only preview in a disposable localhost world. */
object ClientPreview {
    private var serverTicks = 0
    private var totalServerTicks = 0
    private var opened = false
    private var hadPlayer = false
    private var clientTicks = 0
    private var totalClientTicks = 0
    private var initialRevision = -1
    private var shownMenu: ReforgeMenu? = null
    fun serverTick(server: MinecraftServer) {
        if (!java.lang.Boolean.getBoolean("luna_reforged.preview")) return
        totalServerTicks++
        if (totalServerTicks > 2400) { System.err.println("LUNA_REFORGED_PREVIEW_TIMEOUT"); server.halt(false); return }
        val player = server.playerList.players.firstOrNull()
        if (player == null) { if (hadPlayer) server.halt(false); return }
        if (!hadPlayer) totalServerTicks = 0 // Bound connection wait and the active scenario separately.
        hadPlayer = true
        serverTicks++
        if (opened) {
            if (serverTicks == 480) {
                val pos = BlockPos(0, 100, 0)
                player.openMenu(SimpleMenuProvider({ id, inventory, _ -> ReforgeMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), pos)) }, Component.translatable("container.luna_reforged.table")))
                val menu = player.containerMenu as ReforgeMenu
                menu.input.setItem(0, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), io.github.linlunaire.lunareforged.domain.ReforgeRules.find("ember")!!))
                menu.broadcastChanges()
            }
            if (serverTicks in listOf(535, 585, 635)) {
                val menu = player.containerMenu as? ReforgeMenu
                if (menu != null) {
                    val id = when (serverTicks) { 535 -> "siphoning"; 585 -> "feathered"; else -> "nautical" }
                    val item = when (serverTicks) { 535 -> Items.DIAMOND_SWORD; 585 -> Items.DIAMOND_BOOTS; else -> Items.TRIDENT }
                    val definition = io.github.linlunaire.lunareforged.domain.ReforgeRules.find(id)!!
                    menu.input.setItem(0, ReforgeItems.applyCopy(ItemStack(item), definition))
                    menu.broadcastChanges()
                }
            }
            if (serverTicks == 218) {
                val menu = player.containerMenu as? ReforgeMenu
                if (menu != null) {
                    menu.input.setItem(1, ItemStack(LunaContent.stones.getValue("dynamic"), 2))
                    menu.broadcastChanges()
                }
            }
            if (serverTicks == 390) {
                val pos = BlockPos(2, 100, 0)
                server.overworld().setBlock(pos, Blocks.ANVIL.defaultBlockState(), 3)
                player.openMenu(SimpleMenuProvider({ id, inventory, _ -> net.minecraft.world.inventory.AnvilMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), pos)) }, Component.translatable("container.repair")))
                val stack = ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), io.github.linlunaire.lunareforged.domain.ReforgeRules.find("dynamic")!!)
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("原始名称"))
                player.containerMenu.getSlot(0).set(stack)
                player.containerMenu.broadcastChanges()
            }
            if (serverTicks == 278) {
                val menu = player.containerMenu as? ReforgeMenu
                if (menu != null) {
                    val custom = ReforgeJson.parse("""{"id":"starforged","name":"<gradient:#C8B6FF>星铸</gradient:#7DE2FC>","description":["&a+6% &f近战伤害","&a+5% &f下落攻击伤害"],"targets":["melee"],"damage":1.06,"critical":1.05}""")
                    menu.input.setItem(0, ReforgeItems.applyCopy(menu.input.getItem(0), custom))
                    menu.broadcastChanges()
                }
            }
            if (serverTicks == 352) player.level().blockEvent(BlockPos(0, 100, 0), LunaContent.table, ReforgeTableBlock.REFORGE_EVENT, 0)
            return
        }
        if (serverTicks < 100) return
        opened = true
        val level = server.overworld()
        val pos = BlockPos(0, 100, 0)
        for (x in -5..5) for (z in -5..5) level.setBlock(BlockPos(x, 99, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3)
        level.setBlock(pos, LunaContent.table.defaultBlockState(), 3)
        player.connection.teleport(0.5, 100.0, 2.5, 180f, 25f)
        player.inventory.clearContent()
        LunaConfig.reset()
        player.experienceLevel = 100
        player.health = player.maxHealth
        player.openMenu(SimpleMenuProvider({ id, inventory, _ -> ReforgeMenu(id, inventory, ContainerLevelAccess.create(level, pos)) }, Component.translatable("container.luna_reforged.table")))
        val menu = player.containerMenu as ReforgeMenu
        menu.input.setItem(0, ItemStack(Items.DIAMOND_SWORD))
        menu.input.setItem(1, ItemStack.EMPTY)
        menu.broadcastChanges()
    }
    fun clientTick(client: Minecraft) {
        if (!java.lang.Boolean.getBoolean("luna_reforged.preview")) return
        totalClientTicks++
        if (totalClientTicks > 1800) { System.err.println("LUNA_REFORGED_CLIENT_PREVIEW_TIMEOUT"); client.stop(); return }
        val current = client.player?.containerMenu as? ReforgeMenu
        if (current != null) shownMenu = current
        val menu = shownMenu ?: return
        clientTicks++
        client.toasts.clear()
        if (clientTicks in listOf(45, 100, 150, 190)) {
            val x = (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0 + 51
            val y = (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0 + ReforgeLayout.INPUT_Y + 7
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.window.window, x * client.window.guiScale, y * client.window.guiScale)
            // An unfocused preview window may not deliver the OS cursor move. Feed the same
            // coordinates through its existing input callback without taking desktop focus.
            val callback = org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.window, null)
            try { callback?.invoke(client.window.window, x * client.window.guiScale, y * client.window.guiScale) }
            finally { org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.window, callback) }
        }
        if (clientTicks == 155) {
            val mx = client.mouseHandler.xpos() / client.window.guiScale - (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0
            val my = client.mouseHandler.ypos() / client.window.guiScale - (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0
            check(mx in 44.0..60.0 && my in ReforgeLayout.INPUT_Y.toDouble()..(ReforgeLayout.INPUT_Y + 16).toDouble()) { "Preview cursor missed the item: $mx, $my" }
            println("LUNA_REFORGED_TOOLTIP_CURSOR_VERIFIED")
        }
        if (clientTicks == 50) {
            ClientPresentationChecks.run(client)
            initialRevision = menu.revision
            Screenshot.grab(client.gameDirectory, "luna-before.png", client.mainRenderTarget) { println(it.string) }
        }
        if (clientTicks == 70) (client.screen as io.github.linlunaire.lunareforged.client.ReforgeScreen).children().filterIsInstance<net.minecraft.client.gui.components.Button>().single().onPress()
        if (clientTicks == 76) Screenshot.grab(client.gameDirectory, "luna-success.png", client.mainRenderTarget) { println(it.string) }
        if (clientTicks == 110) {
            check(menu.revision != initialRevision && ReforgeItems.times(menu.input.getItem(0)) == 1) { "Client/server reforge roundtrip failed" }
            check(menu.input.getItem(1).isEmpty && menu.resultSequence == 1)
            Screenshot.grab(client.gameDirectory, "luna-after.png", client.mainRenderTarget) { println(it.string) }
            println("LUNA_REFORGED_CLIENT_ROUNDTRIP_PASSED")
        }
        if (clientTicks == 135) (client.screen as io.github.linlunaire.lunareforged.client.ReforgeScreen).children().filterIsInstance<net.minecraft.client.gui.components.Button>().single().onPress()
        if (clientTicks == 160) {
            check(menu.input.getItem(0).get(LunaContent.reforge) == "dynamic" && menu.input.getItem(1).count == 1 && menu.resultSequence == 2)
            check(menu.input.getItem(0).hoverName.string.startsWith("灵动 "))
            Screenshot.grab(client.gameDirectory, "luna-stone.png", client.mainRenderTarget) { println(it.string) }
            println("LUNA_REFORGED_STONE_CLIENT_ROUNDTRIP_PASSED")
        }
        if (clientTicks == 195) {
            check(ReforgeItems.name(menu.input.getItem(0)).string == "星铸" && menu.input.getItem(0).get(LunaContent.profile)?.damage == 1.06) { "Custom profile did not sync from server" }
            Screenshot.grab(client.gameDirectory, "luna-custom.png", client.mainRenderTarget) { println(it.string) }
            println("LUNA_REFORGED_CUSTOM_CLIENT_SYNC_PASSED")
        }
        if (clientTicks == 210) client.player?.closeContainer()
        if (clientTicks in 215..280) { client.player?.setYRot(180f); client.player?.setXRot(30f) }
        if (clientTicks == 240) client.options.hideGui = true
        if (clientTicks == 280) client.options.hideGui = false
        if (clientTicks == 245) Screenshot.grab(client.gameDirectory, "luna-table.png", client.mainRenderTarget) { println(it.string) }
        if (clientTicks == 255) Screenshot.grab(client.gameDirectory, "luna-particles.png", client.mainRenderTarget) { println(it.string) }
        if (clientTicks == 310) {
            val screen = client.screen as net.minecraft.client.gui.screens.inventory.AnvilScreen
            val field = screen.children().filterIsInstance<net.minecraft.client.gui.components.EditBox>().single()
            check(field.value == "原始名称") { "Anvil field must not include the decorative prefix: ${field.value}" }
            field.value = "星河"
        }
        if (clientTicks == 330) {
            val anvil = client.player!!.containerMenu as net.minecraft.world.inventory.AnvilMenu
            val result = anvil.getSlot(2).item
            check(result.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)?.string == "星河")
            check(result.hoverName.string == "灵动 星河")
            Screenshot.grab(client.gameDirectory, "luna-anvil.png", client.mainRenderTarget) { println(it.string) }
            println("LUNA_REFORGED_ANVIL_CLIENT_ROUNDTRIP_PASSED")
        }
        if (clientTicks in listOf(395, 440, 490, 540, 590)) {
            val x = (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0 + 8
            val y = (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0 + 8
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.window.window, x * client.window.guiScale, y * client.window.guiScale)
            val callback = org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.window, null)
            try { callback?.invoke(client.window.window, x * client.window.guiScale, y * client.window.guiScale) }
            finally { org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.window, callback) }
        }
        if (clientTicks in listOf(405, 450, 500, 550, 600)) {
            val expected = when (clientTicks) {
                405 -> "ember"; 450 -> "siphoning"; 500 -> "feathered"
                else -> "nautical"
            }
            check(menu.input.getItem(0).get(LunaContent.reforge) == expected)
            val def = ReforgeItems.definition(menu.input.getItem(0))!!
            check(ReforgeLore.lines(def).first().string.startsWith("+"))
            Screenshot.grab(client.gameDirectory, "luna-$expected.png", client.mainRenderTarget) { println(it.string) }
            println("LUNA_REFORGED_NEW_AFFIX_PREVIEW_PASSED: $expected")
        }
        if (clientTicks == 620) client.stop()
    }
}
