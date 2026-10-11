package io.github.linlunaire.lunareforged.client

import io.github.linlunaire.lunareforged.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** Native slots and keyboard navigation over a lunar artwork, with a live effect preview. */
class ReforgeScreen(menu: ReforgeMenu, inventory: Inventory, title: Component) : AbstractContainerScreen<ReforgeMenu>(menu, inventory, title, ReforgeLayout.WIDTH, ReforgeLayout.HEIGHT) {
    private lateinit var reforgeButton: Button
    private val animation = LunarAnimation()
    private val presentation = ReforgePresentation()
    init {  titleLabelX = 12; titleLabelY = 12; inventoryLabelY = 131 }
    override fun init() {
        super.init()
        reforgeButton = addRenderableWidget(LunarButton(leftPos + 8, topPos + ReforgeLayout.BUTTON_Y, 160, 20, Component.empty()) {
            animation.request()
            refresh()
            minecraft.gameMode?.handleInventoryButtonClick(menu.containerId, menu.revision)
        })
        presentation.invalidate()
        refresh()
    }
    private fun refresh() {
        reforgeButton.active = menu.status == 0 && animation.pendingTicks == 0
        if (presentation.refresh(font, menu, animation)) {
            reforgeButton.message = presentation.button
            reforgeButton.setTooltip(presentation.tooltip)
        }
    }
    override fun containerTick() { super.containerTick(); animation.tick(menu.resultSequence); refresh() }
    override fun extractBackground(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractBackground(g, mouseX, mouseY, delta)
        refresh()
        val x = leftPos; val y = topPos
        g.fill(x - 2, y - 2, x + ReforgeLayout.WIDTH + 2, y + ReforgeLayout.HEIGHT + 2, 0xFF080B13.toInt())
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BACKDROP, x, y, 0f, 0f, ReforgeLayout.WIDTH, ReforgeLayout.HEIGHT, 1448, 1086, 1448, 1086)
        // The artwork remains decorative; text and interactive regions have a stable dark ground.
        g.fill(x + 6, y + 43, x + 170, y + 102, 0xD00C1220.toInt())
        g.fill(x + 179, y + 43, x + 296, y + 218, 0xD80C1220.toInt())
        g.fill(x + 178, y + 45, x + 179, y + 216, 0xFF5B607A.toInt())
        g.fill(x + 12, y + 37, x + 202, y + 38, 0xFF68758D.toInt())
        val accent = presentation.accent
        for ((i, point) in LunarAnimation.halo.withIndex()) {
            val px = x + 88 + (point.first * 16).toInt(); val py = y + 74 + (point.second * 16).toInt()
            val lit = (i + animation.ticks / 3) % 36 < 5
            g.fill(px, py, px + 1, py + 1, if (animation.flashTicks > 0 || lit) accent else 0xFF48506B.toInt())
            if (animation.flashTicks > 0 && i % 3 == 0) {
                val radius = 16 + (32 - animation.flashTicks) / 3.0
                val sx = x + 88 + (point.first * radius).toInt(); val sy = y + 74 + (point.second * radius * 0.6).toInt()
                g.fill(sx, sy, sx + 1, sy + 1, accent)
            }
        }
        for ((row, radius) in LunarAnimation.crescent) {
            g.fill(x + 89 - radius, y + 74 + row, x + 90, y + 75 + row, 0xFFE0DCEB.toInt())
            if (row in -6..6) g.fill(x + 87, y + 74 + row, x + 94, y + 75 + row, 0xFF111827.toInt())
        }
        g.fill(x + 63, y + 74, x + 69, y + 75, 0xFF7C85A0.toInt())
        g.fill(x + 107, y + 74, x + 113, y + 75, 0xFF7C85A0.toInt())
        for ((index, slot) in menu.slots.withIndex()) {
            val sx = x + slot.x; val sy = y + slot.y
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, if (index < 2) 0xFFAB9CBE.toInt() else 0xFF444E64.toInt())
            g.fill(sx, sy, sx + 16, sy + 16, 0xED0C1220.toInt())
            g.fill(sx, sy + 16, sx + 17, sy + 17, 0xFF788296.toInt())
        }
        if (animation.flashTicks > 0) g.fill(x + 8, y + 103, x + 8 + (160 * animation.flashTicks / 32), y + 104, accent)
        g.centeredText(font, presentation.equipment, x + 52, y + 49, 0xFFC4CBDD.toInt())
        g.centeredText(font, presentation.catalyst, x + 124, y + 49, 0xFFC4CBDD.toInt())
        g.centeredText(font, presentation.caption, x + 88, y + 92, if (menu.status == 5) 0xFFFFA7A7.toInt() else 0xFFAEBBCE.toInt())
        g.text(font, presentation.mode, x + 188, y + 51, 0xFF9CAAC1.toInt(), false)
        g.text(font, presentation.heading, x + 188, y + 67, 0xFFE6E3EF.toInt(), false)
        g.fill(x + 188, y + 80, x + 287, y + 81, 0xFF39445B.toInt())
        val lines = presentation.lines
        for (i in 0 until minOf(10, lines.size)) g.text(font, lines[i], x + 188, y + 87 + i * 10, 0xFFB9C2D5.toInt(), false)
        if (lines.size > 10) g.text(font, "…", x + 280, y + 181, 0xFFAEBBCE.toInt(), false)
        g.fill(x + 188, y + 191, x + 287, y + 192, 0xFF39445B.toInt())
        g.text(font, presentation.footer, x + 188, y + 199, 0xFF9CAAC1.toInt(), false)
        if (presentation.previewTooltip.isNotEmpty() && mouseX in x + 179 until x + 296 && mouseY in y + 61 until y + 190) g.setComponentTooltipForNextFrame(font, presentation.previewTooltip, mouseX, mouseY)
    }
    override fun extractLabels(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        g.text(font, title, 12, 12, 0xFFF0EAF6.toInt(), false)
        g.text(font, presentation.subtitle, 12, 26, 0xFFA8B6CC.toInt(), false)
        g.text(font, playerInventoryTitle, 8, 131, 0xFFA6B4CA.toInt(), false)
    }

    companion object { private val BACKDROP = LunaContent.id("textures/gui/lunar_backdrop.png") }
}

private class LunarButton(x: Int, y: Int, width: Int, height: Int, message: Component, onPress: Button.OnPress) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
    override fun extractContents(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val hovered = active && isHoveredOrFocused
        g.fill(x, y, x + width, y + height, if (hovered) 0xFFE5D9F5.toInt() else 0xFF8993AB.toInt())
        g.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1, if (active) 0xFF414864.toInt() else 0xFF272C3B.toInt(), 0xFF242B41.toInt())
        if (hovered) g.fill(x + 3, y + height - 3, x + width - 3, y + height - 2, 0xFFBFB2D7.toInt())
        g.centeredText(net.minecraft.client.Minecraft.getInstance().font, message, x + width / 2, y + (height - 8) / 2, if (active) 0xFFF1EDF8.toInt() else 0xFF9098AB.toInt())
    }
}
