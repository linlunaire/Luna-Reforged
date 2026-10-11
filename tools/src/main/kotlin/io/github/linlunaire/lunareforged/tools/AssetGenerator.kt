package io.github.linlunaire.lunareforged.tools

import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.round

/** Code-native geometry, recipes and bilingual resources; no external scripting runtime. */
internal object AssetGenerator {
    private data class Translation(val key: String, val english: String, val chinese: String)
    // Keep dark-panel names readable even when a translation has only two glyphs.
    private val translations = listOf(
        Translation("block.luna_reforged.reforging_table", "Lunar Reforging Table", "辉月重铸台"),
        Translation("container.luna_reforged.table", "LUNA REFORGED", "辉 月 重 铸"),
        Translation("gui.luna_reforged.success", "Reforged!", "重铸成功"),
        Translation("gui.luna_reforged.pending", "Reforging...", "重铸中…"),
        Translation("gui.luna_reforged.reforge", "Reforge · %s levels", "重铸 · %s 级经验"),
        Translation("gui.luna_reforged.equipment", "Equipment", "装备"),
        Translation("gui.luna_reforged.catalyst", "Stone (optional)", "重铸石（可选）"),
        Translation("gui.luna_reforged.candidates", "%s possible reforges", "可获得 %s 种词缀"),
        Translation("tooltip.luna_reforged.reforge", "Reforge: %s", "重铸：%s"),
        Translation("tooltip.luna_reforged.times", "Reforged %s times", "已重铸 %s 次"),
        Translation("tooltip.luna_reforged.unknown", "Unknown reforge; data has been preserved.", "未知词缀；原有数据已保留。"),
        Translation("tooltip.luna_reforged.reforgable", "This item can be reforged!", "此物品可以重铸！"),
        Translation("tooltip.luna_reforged.stone", "Place on the right to apply %s", "放入重铸台右槽，施加 %s"),
        Translation("item.luna_reforged.stone_suffix", " Reforge Stone", "重铸石"),
        Translation("gui.luna_reforged.mode.random", "Random reforge; no material required", "随机重铸，无需消耗材料"),
        Translation("gui.luna_reforged.mode.stone", "Apply %s; consumes one stone", "施加 %s，消耗一枚重铸石"),
        Translation("gui.luna_reforged.mode.result", "Current: %s", "当前词缀：%s"),
        Translation("gui.luna_reforged.status.0", "Ready. Pay the displayed XP levels. Stones are optional.", "准备就绪，消耗所示经验等级。重铸石可选。"),
        Translation("gui.luna_reforged.status.1", "Insert a weapon, armor piece or pickaxe.", "放入武器、护甲或镐。"),
        Translation("gui.luna_reforged.status.2", "This item cannot be reforged.", "该物品无法重铸。"),
        Translation("gui.luna_reforged.status.3", "Remove this item; use an empty slot or a reforge stone.", "取出该物品；右槽可以留空或放入重铸石。"),
        Translation("gui.luna_reforged.status.4", "No compatible reforge for this equipment and stone.", "没有适用词缀，请取出或更换重铸石。"),
        Translation("gui.luna_reforged.status.5", "Not enough XP levels.", "经验等级不足。"),
        Translation("reforge.luna_reforged.sharp", "<gradient:#f12711>Sharp</gradient:#f5af19>", "<gradient:#f12711>锋锐</gradient:#f5af19>"),
        Translation("reforge.luna_reforged.strong", "<gradient:#F08A84>Strong</gradient:#D46A76>", "<gradient:#F08A84>强韧</gradient:#D46A76>"),
        Translation("reforge.luna_reforged.light", "<gradient:#1c92d2>Light</gradient:#f2fcfe>", "<gradient:#1c92d2>轻盈</gradient:#f2fcfe>"),
        Translation("reforge.luna_reforged.gravitated", "<gradient:#8A9CE2>Gravitated</gradient:#B0BDEF>", "<gradient:#8A9CE2>引力</gradient:#B0BDEF>"),
        Translation("reforge.luna_reforged.acute", "<gradient:#4776E6>Acute</gradient:#8E54E9>", "<gradient:#4776E6>凌厉</gradient:#8E54E9>"),
        Translation("reforge.luna_reforged.dynamic", "<gradient:#AAFFA9>Dynamic</gradient:#11FFBD>", "<gradient:#AAFFA9>灵动</gradient:#11FFBD>"),
        Translation("reforge.luna_reforged.streamlined", "<gradient:#74ebd5>Streamlined</gradient:#ACB6E5>", "<gradient:#74ebd5>流线</gradient:#ACB6E5>"),
        Translation("reforge.luna_reforged.aerobic", "<gradient:#B993D6>Aerobic</gradient:#8CA6DB>", "<gradient:#B993D6>凌空</gradient:#8CA6DB>"),
        Translation("reforge.luna_reforged.pointy", "<gradient:#71C9D8>Pointy</gradient:#ACDFEE>", "<gradient:#71C9D8>穿刺</gradient:#ACDFEE>"),
        Translation("reforge.luna_reforged.nautical", "<gradient:#68D6CE>Nautical</gradient:#6EA8ED>", "<gradient:#68D6CE>潮汐</gradient:#6EA8ED>"),
        Translation("reforge.luna_reforged.rich", "<gradient:#DBDBDB>Rich</gradient:#F2F2F2>", "<gradient:#DBDBDB>丰饶</gradient:#F2F2F2>"),
        Translation("reforge.luna_reforged.prospecting", "<gradient:#00B4DB>Prospecting</gradient:#0083B0>", "<gradient:#00B4DB>勘探</gradient:#0083B0>"),
        Translation("reforge.luna_reforged.reinforced", "<gradient:#FBD786>Reinforced</gradient:#f7797d>", "<gradient:#FBD786>坚固</gradient:#f7797d>"),
        Translation("reforge.luna_reforged.tough", "<gradient:#D99887>Tough</gradient:#BC7C86>", "<gradient:#D99887>坚毅</gradient:#BC7C86>"),
        Translation("reforge.luna_reforged.thin", "<gradient:#D1913C>Thin</gradient:#FFD194>", "<gradient:#D1913C>轻薄</gradient:#FFD194>"),
        Translation("reforge.luna_reforged.evasive", "<gradient:#EFEFBB>Evasive</gradient:#D4D3DD>", "<gradient:#EFEFBB>闪避</gradient:#D4D3DD>"),
        Translation("reforge.luna_reforged.sundering", "<gradient:#FF9966>Sundering</gradient:#FF5E62>", "<gradient:#FF9966>破阵</gradient:#FF5E62>"),
        Translation("reforge.luna_reforged.duelist", "<gradient:#FDC830>Duelist</gradient:#F37335>", "<gradient:#FDC830>决斗</gradient:#F37335>"),
        Translation("reforge.luna_reforged.lunar", "<gradient:#D6BBFF>Lunar</gradient:#74D5FF>", "<gradient:#D6BBFF>月华</gradient:#74D5FF>"),
        Translation("reforge.luna_reforged.desperate", "<gradient:#FF8F91>Desperate</gradient:#E36483>", "<gradient:#FF8F91>背水</gradient:#E36483>"),
        Translation("reforge.luna_reforged.precise", "<gradient:#96FBC4>Precise</gradient:#42A5F5>", "<gradient:#96FBC4>凝神</gradient:#42A5F5>"),
        Translation("reforge.luna_reforged.steadfast", "<gradient:#BCC6CC>Steadfast</gradient:#768896>", "<gradient:#BCC6CC>磐石</gradient:#768896>"),
        Translation("reforge.luna_reforged.fleet", "<gradient:#A8FF78>Fleet</gradient:#78FFD6>", "<gradient:#A8FF78>疾行</gradient:#78FFD6>"),
        Translation("reforge.luna_reforged.warded", "<gradient:#B9A0FF>Warded</gradient:#7864CC>", "<gradient:#B9A0FF>庇护</gradient:#7864CC>"),
        Translation("reforge.luna_reforged.resonant", "<gradient:#D4B8FF>Resonant</gradient:#7D5DC7>", "<gradient:#D4B8FF>共鸣</gradient:#7D5DC7>"),
        Translation("reforge.luna_reforged.excavating", "<gradient:#D5B18B>Excavating</gradient:#A98057>", "<gradient:#D5B18B>掘进</gradient:#A98057>"),
        Translation("reforge.luna_reforged.scholar", "<gradient:#FFF2A1>Scholar</gradient:#D0AA48>", "<gradient:#FFF2A1>博识</gradient:#D0AA48>"),
        Translation("reforge.luna_reforged.balanced", "<gradient:#ADEBDF>Balanced</gradient:#8AABD9>", "<gradient:#ADEBDF>均衡</gradient:#8AABD9>"),
        Translation("reforge.luna_reforged.ember", "<gradient:#FFBE76>Ember</gradient:#FF6348>", "<gradient:#FFBE76>烬火</gradient:#FF6348>"),
        Translation("reforge.luna_reforged.frostbound", "<gradient:#DCF6FF>Frostbound</gradient:#78BEEB>", "<gradient:#DCF6FF>霜凝</gradient:#78BEEB>"),
        Translation("reforge.luna_reforged.siphoning", "<gradient:#EBC5FF>Siphoning</gradient:#B77BEA>", "<gradient:#EBC5FF>汲月</gradient:#B77BEA>"),
        Translation("reforge.luna_reforged.executioner", "<gradient:#F9A7AD>Executioner</gradient:#B9415A>", "<gradient:#F9A7AD>终结</gradient:#B9415A>"),
        Translation("reforge.luna_reforged.dawn", "<gradient:#FFF1AC>Dawn</gradient:#FFA775>", "<gradient:#FFF1AC>曦光</gradient:#FFA775>"),
        Translation("reforge.luna_reforged.tempest", "<gradient:#A5E8EF>Tempest</gradient:#818CDE>", "<gradient:#A5E8EF>雨鸣</gradient:#818CDE>"),
        Translation("reforge.luna_reforged.skirmishing", "<gradient:#D4EDA8>Skirmishing</gradient:#8ACE96>", "<gradient:#D4EDA8>奔袭</gradient:#8ACE96>"),
        Translation("reforge.luna_reforged.farshot", "<gradient:#E0D5FF>Farshot</gradient:#8E9FF2>", "<gradient:#E0D5FF>长星</gradient:#8E9FF2>"),
        Translation("reforge.luna_reforged.feathered", "<gradient:#F0EEFF>Feathered</gradient:#A7CEEE>", "<gradient:#F0EEFF>轻羽</gradient:#A7CEEE>"),
        Translation("reforge.luna_reforged.cinderward", "<gradient:#EDCCAB>Cinderward</gradient:#C68C88>", "<gradient:#EDCCAB>烬佑</gradient:#C68C88>"),
        Translation("reforge.luna_reforged.bulwark", "<gradient:#C5D5E7>Bulwark</gradient:#7D91AF>", "<gradient:#C5D5E7>壁垒</gradient:#7D91AF>"),
        Translation("reforge.luna_reforged.moonveil", "<gradient:#DEE0FF>Moonveil</gradient:#A58EDD>", "<gradient:#DEE0FF>月幕</gradient:#A58EDD>"),
        Translation("reforge.luna_reforged.sentinel", "<gradient:#F0DFB4>Sentinel</gradient:#BABDD4>", "<gradient:#F0DFB4>守望</gradient:#BABDD4>"),
        Translation("reforge.luna_reforged.gilded", "<gradient:#FFE7A3>Gilded</gradient:#D9AD69>", "<gradient:#FFE7A3>鎏金</gradient:#D9AD69>"),
        Translation("reforge.luna_reforged.quarrying", "<gradient:#C7C7BA>Quarrying</gradient:#A49884>", "<gradient:#C7C7BA>岩凿</gradient:#A49884>"),
        Translation("reforge.luna_reforged.nimble", "<gradient:#D2F1E1>Nimble</gradient:#89C7C2>", "<gradient:#D2F1E1>巧工</gradient:#89C7C2>"),
        Translation("item.luna_reforged.dynamic_stone", "Dynamic Reforge Stone", "灵动重铸石"),
        Translation("item.luna_reforged.rich_stone", "Rich Reforge Stone", "丰饶重铸石"),
        Translation("item.luna_reforged.prospecting_stone", "Prospecting Reforge Stone", "勘探重铸石"),
        Translation("stat.luna_reforged.melee_damage", "Damage", "近战伤害"),
        Translation("stat.luna_reforged.projectile_damage", "Projectile Damage", "投射物伤害"),
        Translation("stat.luna_reforged.damage", "Damage", "武器伤害"),
        Translation("stat.luna_reforged.arrow_damage", "Arrow Damage", "箭矢伤害"),
        Translation("stat.luna_reforged.trident_damage", "Thrown Trident Damage", "投掷三叉戟伤害"),
        Translation("stat.luna_reforged.knockback", "Attack Knockback", "攻击击退属性"),
        Translation("stat.luna_reforged.flat_knockback", "Attack Knockback", "攻击击退"),
        Translation("stat.luna_reforged.attack_speed", "Attack Speed", "攻击速度"),
        Translation("stat.luna_reforged.falling_damage", "Falling Attack Damage", "下落攻击伤害"),
        Translation("stat.luna_reforged.triple_chance", "Triple Damage Chance", "三倍近战伤害概率"),
        Translation("stat.luna_reforged.resistance_all", "Damage Resistance", "伤害减免"),
        Translation("stat.luna_reforged.resistance_environment", "Environmental Damage Resistance", "环境伤害减免"),
        Translation("stat.luna_reforged.resistance_fall", "Fall Damage Resistance", "摔落伤害减免"),
        Translation("stat.luna_reforged.resistance_fire", "Fire Damage Resistance", "火焰伤害减免"),
        Translation("stat.luna_reforged.resistance_projectile", "Projectile Damage Resistance", "投射物伤害减免"),
        Translation("stat.luna_reforged.dodge_all", "Chance to Ignore Damage", "抵消伤害的概率"),
        Translation("stat.luna_reforged.dodge_environment", "Chance to Ignore Environmental Damage", "抵消环境伤害的概率"),
        Translation("stat.luna_reforged.dodge_fall", "Chance to Ignore Fall Damage", "抵消摔落伤害的概率"),
        Translation("stat.luna_reforged.dodge_fire", "Chance to Ignore Fire Damage", "抵消火焰伤害的概率"),
        Translation("stat.luna_reforged.dodge_projectile", "Chance to Ignore Projectiles", "抵消投射物伤害的概率"),
        Translation("stat.luna_reforged.movement_speed", "Movement Speed", "移动速度"),
        Translation("stat.luna_reforged.knockback_resistance", "Knockback Resistance", "击退抗性"),
        Translation("stat.luna_reforged.mining_speed", "Mining Speed while Held", "手持时挖掘速度"),
        Translation("stat.luna_reforged.kill_xp", "XP per Weapon Kill", "武器击杀经验点"),
        Translation("stat.luna_reforged.mining_xp", "Chance for %s XP when Mining", "挖掘获得 %s 点经验的概率"),
        Translation("stat.luna_reforged.burn_seconds", "s of Burning on Hit", "秒点燃 · 命中时"),
        Translation("stat.luna_reforged.slow_seconds", "s of Slowness I on Hit", "秒缓慢 I · 命中时"),
        Translation("stat.luna_reforged.life_steal", "Life Steal", "生命汲取"),
        Translation("stat.luna_reforged.life_steal_cap", "Up to 1 HP per hit", "每次最多恢复 1 点生命"),
        Translation("stat.luna_reforged.cooldown", "Effective hits only · %ss shared cooldown", "有效命中触发 · 共用 %s 秒冷却"),
        Translation("stat.luna_reforged.chance", " (%s%% chance)", "（%s%% 概率）"),
        Translation("condition.luna_reforged.air", " while Airborne", "（自身在空中）"),
        Translation("condition.luna_reforged.water", " while in Water", "（自身在水中）"),
        Translation("condition.luna_reforged.night", " at Night", "（有天空的维度 · 夜间）"),
        Translation("condition.luna_reforged.day", " by Day", "（有天空的维度 · 昼间）"),
        Translation("condition.luna_reforged.rain", " in Rain", "（自身淋雨时）"),
        Translation("condition.luna_reforged.sprinting", " while Sprinting", "（疾跑时）"),
        Translation("condition.luna_reforged.distant", " at 12+ Blocks", "（命中距离 ≥12 格）"),
        Translation("condition.luna_reforged.wounded_target", " vs Targets at ≤30% Health", "（目标生命 ≤30%）"),
        Translation("condition.luna_reforged.low_health", " at ≤40% Health", "（自身生命 ≤40%）"),
        Translation("condition.luna_reforged.full_health", " at ≥90% Health", "（自身生命 ≥90%）"),
        Translation("gui.luna_reforged.subtitle", "Let moonlight temper your gear", "以月华淬炼锋芒"),
        Translation("gui.luna_reforged.preview.stone", "STONE PREVIEW", "石头效果"),
        Translation("gui.luna_reforged.preview.current", "CURRENT REFORGE", "当前词缀"),
        Translation("gui.luna_reforged.preview.empty", "Awaiting moonlight", "静候月华"),
        Translation("gui.luna_reforged.preview.hint", "Insert equipment to begin. A stone selects its reforge. Hover an effect for full details.", "放入装备即可重铸。重铸石用于指定词缀；悬停可查看完整效果。"),
        Translation("gui.luna_reforged.preview.fixed", "One stone per reforge", "每次消耗一枚重铸石"),
        Translation("gui.luna_reforged.preview.random", "Random · no materials", "随机重铸 · 无需材料"),
        Translation("gui.luna_reforged.short_status.1", "Place your equipment", "请放入装备"),
        Translation("gui.luna_reforged.short_status.2", "Incompatible equipment", "装备无法重铸"),
        Translation("gui.luna_reforged.short_status.3", "Use a reforge stone or leave empty", "右槽仅限重铸石，也可留空"),
        Translation("gui.luna_reforged.short_status.4", "No compatible reforge", "没有适用词缀"),
        Translation("gui.luna_reforged.short_status.5", "Not enough experience", "经验等级不足"),
    )

    fun generate(root: File) {
        val assets = "common/src/main/resources/assets/luna_reforged"
        fun emit(path: String, value: Any) = writeJson(root.resolve(path), value)
        emit("$assets/lang/en_us.json", translations.associateTo(linkedMapOf()) { it.key to it.english })
        emit("$assets/lang/zh_cn.json", translations.associateTo(linkedMapOf()) { it.key to it.chinese })

        val elements = mutableListOf(
            box(listOf(1, 0, 1), listOf(15, 2, 15), "dark"),
            box(listOf(2, 2, 2), listOf(14, 3, 14), "trim"),
            box(listOf(5, 3, 5), listOf(11, 10, 11), "dark"),
            box(listOf(1, 10, 1), listOf(15, 11, 15), "trim"),
            box(listOf(0, 11, 0), listOf(16, 12, 16), "dark")
        )
        for (x in listOf(2, 12)) for (z in listOf(2, 12)) {
            elements += box(listOf(x, 2, z), listOf(x + 2, 10, z + 2), "trim")
            elements += box(listOf(x + .5, 5, z + .5), listOf(x + 1.5, 9, z + 1.5), "crystal")
        }
        for ((a, b) in listOf(
            listOf(7, 3, 4.8) to listOf(9, 9, 5),
            listOf(7, 3, 11) to listOf(9, 9, 11.2),
            listOf(4.8, 3, 7) to listOf(5, 9, 9),
            listOf(11, 3, 7) to listOf(11.2, 9, 9)
        )) elements += box(a, b, "crystal")
        for ((a, b) in listOf(
            listOf(3, 12, 3) to listOf(13, 12.35, 4),
            listOf(3, 12, 12) to listOf(13, 12.35, 13),
            listOf(3, 12, 4) to listOf(4, 12.35, 12),
            listOf(12, 12, 4) to listOf(13, 12.35, 12)
        )) elements += box(a, b, "gold")
        for ((x, z) in listOf(2 to 7, 13 to 7, 7 to 2, 7 to 13)) {
            elements += box(listOf(x, 12, z), listOf(x + 1, 12.5, z + 2), "glow")
        }
        for ((y, size) in listOf(13 to 1.4, 14 to 2.2, 16 to 1.4, 17 to .7)) {
            val element = box(listOf(8 - size, y, 8 - size), listOf(8 + size, y + 1.5, 8 + size), "crystal")
            element["rotation"] = linkedMapOf("origin" to listOf(8, 15, 8), "axis" to "y", "angle" to 45, "rescale" to false)
            elements += element
        }
        for (i in 0..12) {
            val angle = Math.toRadians(55 + i * 250.0 / 12)
            val x = 8 + cos(angle) * 4.7
            val y = 16 + sin(angle) * 3.4
            elements += box(listOf(round((x - .8) * 100) / 100, round((y - .7) * 100) / 100, 3.8),
                listOf(round((x + .8) * 100) / 100, round((y + .7) * 100) / 100, 4.8), "gold")
        }
        elements += box(listOf(7.5, 19, 7.5), listOf(8.5, 20, 8.5), "glow")
        emit("$assets/blockstates/reforging_table.json", mapOf("variants" to mapOf("" to mapOf("model" to "luna_reforged:block/reforging_table"))))
        emit("$assets/models/block/reforging_table.json", linkedMapOf(
            "parent" to "minecraft:block/block", "ambientocclusion" to true,
            "textures" to linkedMapOf(
                "particle" to "minecraft:block/obsidian", "dark" to "minecraft:block/obsidian",
                "trim" to "minecraft:block/gilded_blackstone", "gold" to "minecraft:block/gold_block",
                "glow" to "minecraft:block/sea_lantern", "crystal" to "minecraft:block/amethyst_block"
            ), "elements" to elements
        ))
        emit("$assets/models/item/reforging_table.json", mapOf("parent" to "luna_reforged:block/reforging_table"))
        for ((id, texture) in listOf("dynamic" to "amethyst_shard", "rich" to "echo_shard", "prospecting" to "prismarine_crystals")) {
            emit("$assets/models/item/${id}_stone.json", mapOf("parent" to "minecraft:item/generated", "textures" to mapOf("layer0" to "minecraft:item/$texture")))
        }
        for (version in minecraftVersions) {
            val resources = "versions/$version/src/main/resources"
            val data = "$resources/data"
            val modern = version == "26.2"
            if (modern) for (id in listOf("reforging_table", "dynamic_stone", "rich_stone", "prospecting_stone")) {
                emit("$resources/assets/luna_reforged/items/$id.json", mapOf("model" to mapOf("type" to "minecraft:model", "model" to "luna_reforged:item/$id")))
            }
            val recipes = linkedMapOf(
                "reforging_table" to (listOf("AIA", "OCO", "OOO") to linkedMapOf("A" to "amethyst_shard", "I" to "iron_ingot", "O" to "obsidian", "C" to "crafting_table")),
                "dynamic_stone" to (listOf("IDI", " P ") to linkedMapOf("I" to "iron_block", "D" to "daylight_detector", "P" to "phantom_membrane")),
                "rich_stone" to (listOf("GTG", " O ", "GBG") to linkedMapOf("G" to "gold_block", "T" to "trident", "O" to "obsidian", "B" to "bow")),
                "prospecting_stone" to (listOf(" C ", "CAC", " I ") to linkedMapOf("C" to "copper_ingot", "A" to "amethyst_shard", "I" to "iron_pickaxe"))
            )
            for ((id, recipe) in recipes) {
                val (pattern, key) = recipe
                emit("$data/luna_reforged/recipe/$id.json", linkedMapOf(
                    "type" to "minecraft:crafting_shaped", "category" to "misc", "pattern" to pattern,
                    "key" to key.mapValues { (_, item) -> if (modern) "minecraft:$item" else mapOf("item" to "minecraft:$item") },
                    "result" to mapOf("id" to "luna_reforged:$id", "count" to 1)
                ))
                emit("$data/luna_reforged/advancement/recipes/$id.json", linkedMapOf(
                    "parent" to "minecraft:recipes/root",
                    "criteria" to mapOf("has_amethyst" to mapOf("trigger" to "minecraft:inventory_changed",
                        "conditions" to mapOf("items" to listOf(mapOf("items" to "minecraft:amethyst_shard"))))),
                    "requirements" to listOf(listOf("has_amethyst")), "rewards" to mapOf("recipes" to listOf("luna_reforged:$id"))
                ))
            }
            emit("$data/luna_reforged/loot_table/blocks/reforging_table.json", mapOf(
                "type" to "minecraft:block", "pools" to listOf(mapOf("rolls" to 1,
                    "entries" to listOf(mapOf("type" to "minecraft:item", "name" to "luna_reforged:reforging_table")),
                    "conditions" to listOf(mapOf("condition" to "minecraft:survives_explosion"))))
            ))
            for (tag in listOf("mineable/pickaxe", "needs_diamond_tool")) {
                emit("$data/minecraft/tags/block/$tag.json", mapOf("replace" to false, "values" to listOf("luna_reforged:reforging_table")))
            }
        }
        println("Generated block geometry, recipes and ${translations.size} bilingual translation keys")
    }

    private fun box(a: List<Number>, b: List<Number>, texture: String): MutableMap<String, Any> = linkedMapOf(
        "from" to a, "to" to b,
        "faces" to listOf("up", "down", "north", "south", "east", "west").associateWith { mapOf("texture" to "#$texture") }
    )
}
