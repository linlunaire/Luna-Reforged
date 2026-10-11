package io.github.linlunaire.lunareforged.verification

import io.github.linlunaire.lunareforged.*
import com.mojang.blaze3d.systems.RenderSystem
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
import java.util.concurrent.CompletableFuture

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
    private var tideStack: ItemStack? = null
    private var reload: CompletableFuture<Void>? = null
    private var reloadedTicks = 0
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
        client.gui.toastManager().clear()
        if (clientTicks in listOf(45, 100, 150, 190, 615)) {
            val x = (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0 + 51
            val y = (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0 + ReforgeLayout.INPUT_Y + 7
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.window.handle(), x * client.window.guiScale, y * client.window.guiScale)
            val callback = org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.handle(), null)
            try { callback?.invoke(client.window.handle(), x * client.window.guiScale, y * client.window.guiScale) }
            finally { org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.handle(), callback) }
        }
        if (clientTicks == 50) {
            val device = RenderSystem.getDevice().deviceInfo
            val expectedBackend = System.getProperty("luna_reforged.previewBackend")
            check(expectedBackend == null || device.backendName().equals(expectedBackend, ignoreCase = true)) {
                "Requested $expectedBackend but the game selected ${device.backendName()}"
            }
            println("LUNA_REFORGED_RENDER_BACKEND: ${device.backendName()} / ${device.name()} / ${device.driverInfo()}")
            ClientPresentationChecks.run(client)
            initialRevision = menu.revision
            Screenshot.grab(client.gameDirectory, "luna-before.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
        }
        if (clientTicks == 70) (client.gui.screen() as io.github.linlunaire.lunareforged.client.ReforgeScreen).children().filterIsInstance<net.minecraft.client.gui.components.Button>().single().onPress(net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0))
        if (clientTicks == 76) Screenshot.grab(client.gameDirectory, "luna-success.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
        if (clientTicks == 110) {
            check(menu.revision != initialRevision && ReforgeItems.times(menu.input.getItem(0)) == 1) { "Client/server reforge roundtrip failed" }
            check(menu.input.getItem(1).isEmpty && menu.resultSequence == 1)
            Screenshot.grab(client.gameDirectory, "luna-after.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_CLIENT_ROUNDTRIP_PASSED")
        }
        if (clientTicks == 135) (client.gui.screen() as io.github.linlunaire.lunareforged.client.ReforgeScreen).children().filterIsInstance<net.minecraft.client.gui.components.Button>().single().onPress(net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0))
        if (clientTicks == 160) {
            check(menu.input.getItem(0).get(LunaContent.reforge) == "dynamic" && menu.input.getItem(1).count == 1 && menu.resultSequence == 2)
            check(menu.input.getItem(0).hoverName.string.startsWith("灵动 "))
            Screenshot.grab(client.gameDirectory, "luna-stone.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_STONE_CLIENT_ROUNDTRIP_PASSED")
        }
        if (clientTicks == 195) {
            check(ReforgeItems.name(menu.input.getItem(0)).string == "星铸" && menu.input.getItem(0).get(LunaContent.profile)?.damage == 1.06) { "Custom profile did not sync from server" }
            Screenshot.grab(client.gameDirectory, "luna-custom.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_CUSTOM_CLIENT_SYNC_PASSED")
        }
        if (clientTicks == 210) client.player?.closeContainer()
        if (clientTicks in 215..280) { client.player?.setYRot(180f); client.player?.setXRot(30f) }
        if (clientTicks == 240 && !client.gui.hud.isHidden) client.gui.hud.toggle()
        if (clientTicks == 280 && client.gui.hud.isHidden) client.gui.hud.toggle()
        if (clientTicks == 245) Screenshot.grab(client.gameDirectory, "luna-table.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
        if (clientTicks == 255) Screenshot.grab(client.gameDirectory, "luna-particles.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
        if (clientTicks == 310) {
            val screen = client.gui.screen() as net.minecraft.client.gui.screens.inventory.AnvilScreen
            val field = screen.children().filterIsInstance<net.minecraft.client.gui.components.EditBox>().single()
            check(field.value == "原始名称") { "Anvil field must not include the decorative prefix: ${field.value}" }
            field.value = "星河"
        }
        if (clientTicks == 330) {
            val anvil = client.player!!.containerMenu as net.minecraft.world.inventory.AnvilMenu
            val result = anvil.getSlot(2).item
            check(result.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)?.string == "星河")
            check(result.hoverName.string == "灵动 星河")
            Screenshot.grab(client.gameDirectory, "luna-anvil.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_ANVIL_CLIENT_ROUNDTRIP_PASSED")
        }
        // Keep the cursor away before the first Tide item arrives and until its colors are checked.
        if (clientTicks in listOf(395, 440, 490, 540, 635)) {
            val x = (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0 + 8
            val y = (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0 + 8
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.window.handle(), x * client.window.guiScale, y * client.window.guiScale)
            val callback = org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.handle(), null)
            try { callback?.invoke(client.window.handle(), x * client.window.guiScale, y * client.window.guiScale) }
            finally { org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(client.window.handle(), callback) }
        }
        if (clientTicks in listOf(405, 450, 500, 550, 600)) {
            val expected = when (clientTicks) {
                405 -> "ember"; 450 -> "siphoning"; 500 -> "feathered"
                else -> "nautical"
            }
            check(menu.input.getItem(0).get(LunaContent.reforge) == expected)
            val def = ReforgeItems.definition(menu.input.getItem(0))!!
            check(ReforgeLore.lines(def).first().string.startsWith("+"))
            if (clientTicks == 550 || clientTicks == 600) {
                if (clientTicks == 550) tideStack = menu.input.getItem(0)
                check(menu.input.getItem(0) === tideStack) { "The Tide item was replaced during the hover test" }
                check(client.mouseHandler.getScaledXPos(client.window) < (client.window.guiScaledWidth - ReforgeLayout.WIDTH) / 2.0 + 20)
                check(client.mouseHandler.getScaledYPos(client.window) < (client.window.guiScaledHeight - ReforgeLayout.HEIGHT) / 2.0 + 20)
                checkTideColors(client, listOf(0x68D6CE, 0x6EA8ED))
            }
            val name = if (clientTicks == 550) "nautical-first-display" else expected
            Screenshot.grab(client.gameDirectory, "luna-$name.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_NEW_AFFIX_PREVIEW_PASSED: $expected")
        }
        if (clientTicks == 625 || clientTicks == 645) {
            check(menu.input.getItem(0) === tideStack) { "The Tide item was replaced during the hover test" }
            checkTideColors(client, listOf(0x68D6CE, 0x6EA8ED))
            val state = if (clientTicks == 625) "hovered" else "unhovered"
            Screenshot.grab(client.gameDirectory, "luna-nautical-$state.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
            println("LUNA_REFORGED_TIDE_HOVER_COLORS_PASSED: $state")
        }
        if (clientTicks == 655) reload = client.reloadResourcePacks()
        if (reload?.isDone == true) {
            reload!!.join()
            // The reload future completes before the loading overlay finishes fading out.
            if (client.gui.overlay() == null && ++reloadedTicks == 20) {
                check(menu.input.getItem(0) === tideStack)
                check(menu.input.getItem(0).get(LunaContent.reforge) == "nautical")
                checkTideColors(client, listOf(0x68D6CE, 0x6EA8ED))
                ClientPresentationChecks.run(client)
                Screenshot.grab(client.gameDirectory, "luna-after-reload.png", client.gameRenderer.mainRenderTarget(), 1) { println(it.string) }
                println("LUNA_REFORGED_RESOURCE_RELOAD_PASSED")
            }
        }
        if (reloadedTicks == 30) client.stop()
    }

    private fun checkTideColors(client: Minecraft, expected: List<Int>) {
        val screen = client.gui.screen() as io.github.linlunaire.lunareforged.client.ReforgeScreen
        // Inspect the actual screen's cached text without refreshing it or building a tooltip.
        val field = screen.javaClass.getDeclaredField("presentation").apply { isAccessible = true }
        val presentation = field.get(screen) as io.github.linlunaire.lunareforged.client.ReforgePresentation
        check(presentation.heading.string == "潮汐")
        val colors = mutableListOf<Int?>()
        presentation.heading.visualOrderText.accept { _, style, _ -> colors.add(style.color?.value); true }
        check(colors == expected) { "Rendered Tide colors were $colors, expected $expected at tick $clientTicks" }
        println("LUNA_REFORGED_TIDE_RENDERED_COLORS: tick=$clientTicks colors=${colors.map { it?.toString(16) }}")
    }
}
