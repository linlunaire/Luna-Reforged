package io.github.linlunaire.lunareforged.client

import io.github.linlunaire.lunareforged.*
import io.github.linlunaire.lunareforged.domain.Reforge
import io.github.linlunaire.lunareforged.domain.ReforgeRules
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/** Screen-owned text layout shared by both renderers. Unchanged frames only compare state. */
class ReforgePresentation {
    val equipment: Component = ReforgeText.component("gui.luna_reforged.equipment")
    val catalyst: Component = ReforgeText.component("gui.luna_reforged.catalyst")
    val subtitle: Component = ReforgeText.component("gui.luna_reforged.subtitle")
    var button: Component = Component.empty(); private set
    var tooltip: Tooltip? = null; private set
    var caption: Component = Component.empty(); private set
    var heading: Component = Component.empty(); private set
    var mode: Component = Component.empty(); private set
    var footer: Component = Component.empty(); private set
    var lines: List<FormattedCharSequence> = emptyList(); private set
    var previewTooltip: List<Component> = emptyList(); private set
    var accent = 0; private set

    private var language: Language? = null
    private var layoutFont: Font? = null
    private var definition: Reforge? = null
    private var itemId: String? = null
    private var stoneId: String? = null
    private var status = -1
    private var cost = -1
    private var candidates = -1
    private var pending = false
    private var success = false

    fun invalidate() { language = null }

    fun refresh(font: Font, menu: ReforgeMenu, animation: LunarAnimation): Boolean {
        val nextLanguage = Language.getInstance()
        val item = menu.input.getItem(0)
        val nextDefinition = ReforgeItems.definition(item)
        val nextItemId = item.get(LunaContent.reforge)
        val nextStoneId = (menu.input.getItem(1).item as? ReforgeStoneItem)?.reforgeId
        val nextPending = animation.pendingTicks > 0
        val nextSuccess = animation.flashTicks > 16
        if (language === nextLanguage && layoutFont === font && definition == nextDefinition &&
            itemId == nextItemId && stoneId == nextStoneId && status == menu.status &&
            cost == menu.cost && candidates == menu.candidateCount && pending == nextPending && success == nextSuccess) return false

        language = nextLanguage
        layoutFont = font
        definition = nextDefinition
        itemId = nextItemId
        stoneId = nextStoneId
        status = menu.status
        cost = menu.cost
        candidates = menu.candidateCount
        pending = nextPending
        success = nextSuccess

        val stone = nextStoneId?.let(ReforgeRules::find)
        val preview = stone ?: nextDefinition
        val previewName = preview?.let(ReforgeItems::name)
        val lore = preview?.let(ReforgeLore::lines)
        previewTooltip = if (previewName == null) emptyList() else listOf(previewName) + lore!!
        lines = (lore ?: listOf(ReforgeText.component("gui.luna_reforged.preview.hint"))).flatMap { font.split(it, 98) }
        heading = ReforgeLabels.fit(font, previewName ?: ReforgeText.component("gui.luna_reforged.preview.empty"), 99)
        mode = ReforgeText.component(if (stoneId != null) "gui.luna_reforged.preview.stone" else "gui.luna_reforged.preview.current")
        footer = ReforgeLabels.fit(font, ReforgeText.component(if (stoneId != null) "gui.luna_reforged.preview.fixed" else "gui.luna_reforged.preview.random"), 99)
        button = if (pending) ReforgeText.component("gui.luna_reforged.pending") else ReforgeText.component("gui.luna_reforged.reforge", cost)
        tooltip = Tooltip.create(when {
            status != 0 -> ReforgeText.component("gui.luna_reforged.status.$status")
            stone != null -> ReforgeText.component("gui.luna_reforged.mode.stone", previewName!!)
            else -> ReforgeText.component("gui.luna_reforged.mode.random")
        })
        caption = ReforgeLabels.fit(font, when {
            success -> ReforgeText.component("gui.luna_reforged.success")
            status != 0 -> ReforgeText.component("gui.luna_reforged.short_status.$status")
            itemId != null -> ReforgeText.component("gui.luna_reforged.mode.result", ReforgeItems.name(item))
            else -> ReforgeText.component("gui.luna_reforged.candidates", candidates)
        }, 156)
        accent = ReforgeVisuals.accent(nextDefinition)
        return true
    }
}
