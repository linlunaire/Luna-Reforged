package io.github.linlunaire.lunareforged.verification

import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.client.LunarAnimation
import io.github.linlunaire.lunareforged.client.ReforgePresentation
import io.github.linlunaire.lunareforged.domain.ReforgeRules
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** Invoked once by the opt-in client preview, using a detached menu with real font metrics. */
object ClientPresentationChecks {
    fun run(client: Minecraft) {
        val menu = ReforgeMenu(77, client.player!!.inventory)
        val presentation = ReforgePresentation()
        val animation = LunarAnimation()
        animation.tick(0)
        check(presentation.refresh(client.font, menu, animation))
        val empty = presentation.heading.string
        menu.input.setItem(0, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), ReforgeRules.find("lunar")!!))
        check(presentation.refresh(client.font, menu, animation) && presentation.heading.string != empty)
        val heading = presentation.heading
        val lines = presentation.lines
        val caption = presentation.caption
        repeat(20_000) { check(!presentation.refresh(client.font, menu, animation)) }
        val bean = java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        bean.isThreadAllocatedMemoryEnabled = true
        val thread = Thread.currentThread().threadId()
        val before = bean.getThreadAllocatedBytes(thread)
        repeat(100_000) { check(!presentation.refresh(client.font, menu, animation)) }
        val allocated = bean.getThreadAllocatedBytes(thread) - before
        check(allocated < 16_384) { "Stable presentation refresh introduced per-call allocation: $allocated bytes" }
        check(presentation.heading === heading && presentation.lines === lines && presentation.caption === caption) { "Stable frames rebuilt text/layout" }
        println("LUNA_REFORGED_PRESENTATION_ALLOCATION: $allocated bytes / 100000 unchanged refreshes")
        menu.setData(0, 7)
        check(presentation.refresh(client.font, menu, animation) && "7" in presentation.button.string)
        animation.request()
        check(presentation.refresh(client.font, menu, animation))
        animation.tick(1)
        check(presentation.refresh(client.font, menu, animation))
        menu.input.setItem(1, ItemStack(LunaContent.stones.getValue("dynamic")))
        check(presentation.refresh(client.font, menu, animation) && presentation.heading.string == ReforgeItems.name(ReforgeRules.find("dynamic")!!).string)
        menu.input.setItem(1, ItemStack.EMPTY)
        val custom = ReforgeJson.parse("""{"id":"test_affix","targets":["melee"],"name":"First name","description":["First line"]}""")
        menu.input.setItem(0, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), custom))
        check(presentation.refresh(client.font, menu, animation))
        val customHeading = presentation.heading.string
        menu.input.setItem(0, ReforgeItems.applyCopy(ItemStack(Items.DIAMOND_SWORD), custom.copy(name = "Updated", description = listOf("Updated line"))))
        check(presentation.refresh(client.font, menu, animation) && presentation.heading.string != customHeading) { "Same-ID custom snapshot stayed stale" }
        presentation.invalidate()
        check(presentation.refresh(client.font, menu, animation))
        menu.input.clearContent()
        check(presentation.refresh(client.font, menu, animation) && presentation.heading.string == empty && presentation.previewTooltip.isEmpty())
        println("LUNA_REFORGED_PRESENTATION_CACHE_PASSED")
    }
}
