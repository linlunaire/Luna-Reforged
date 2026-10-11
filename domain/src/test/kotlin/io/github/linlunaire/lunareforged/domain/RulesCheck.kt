package io.github.linlunaire.lunareforged.domain

object RulesCheck {
    @JvmStatic fun main(args: Array<String>) {
        val prices = PricePolicy()
        check(prices.cost(0, null) == 3)
        check(prices.cost(1, null) == 4)
        check(prices.cost(5, null) == 7)
        check(prices.cost(100, "rich") == 3)
        check(!ReforgeRules.find("prospecting")!!.stoneOnly)
        check(prices.cost(1_000_000, null) == 30)
        check(prices.cost(999_999, "dynamic") == 40)
        check(runCatching { PricePolicy(growth = Double.NaN) }.isFailure)
        check(runCatching { PricePolicy(baseLevels = 0) }.isFailure)
        check(runCatching { prices.cost(-1, null) }.isFailure)
        check(ReforgeRules.definitions.size == 44)
        check(ReforgeRules.definitions.map { it.id }.distinct().size == 44)
        check(ReforgeRules.candidates(setOf(Target.PICKAXE), null, null).size == 7)
        check(runCatching { Reforge("bad", setOf(Target.MELEE), damage = Double.NaN) }.isFailure)
        check(runCatching { Reforge("bad", setOf(Target.MELEE), condition = "typo") }.isFailure)
        check(runCatching { Reforge("bad", setOf(Target.MELEE), speed = -1.0) }.isFailure)
        val pool = ReforgeRules.candidates(setOf(Target.MELEE, Target.AXE), "sharp", null)
        check(pool.none { it.id == "sharp" || it.stoneOnly })
        check(pool.any { it.id == "acute" })
        check(ReforgeRules.candidates(setOf(Target.ARMOR), null, "dynamic").isEmpty())
        check(ReforgeRules.candidates(setOf(Target.MELEE), null, "dynamic").single().id == "dynamic")
        check(ReforgeRules.candidates(setOf(Target.MELEE), "dynamic", "dynamic").single().id == "dynamic")
        check(ReforgeRules.select(emptyList()) { error("must not roll without candidates") } == null)
        val counts = pool.associate { it.id to 0 }.toMutableMap()
        repeat(pool.size * 100) { i -> val chosen = ReforgeRules.select(pool) { i % it }!!; counts[chosen.id] = counts.getValue(chosen.id) + 1 }
        check(counts.values.all { it == 100 })
        check(ReforgeRules.find("rich")!!.killXp == 5)
        check(ReforgeRules.find("prospecting")!!.miningXp == 2)
        val animation = io.github.linlunaire.lunareforged.client.LunarAnimation()
        animation.tick(0)
        animation.request()
        repeat(5) { animation.tick(0) }
        check(animation.flashTicks == 0 && animation.pendingTicks > 0) { "A click is not a successful transaction" }
        animation.tick(1)
        check(animation.flashTicks == 32 && animation.pendingTicks == 0)
        repeat(32) { animation.tick(1) }
        check(animation.flashTicks == 0) { "Repeated receipt must not retrigger flash" }
        animation.request()
        repeat(20) { animation.tick(1) }
        check(animation.pendingTicks == 0 && animation.flashTicks == 0) { "A rejected click must time out without success" }
        println("Luna Reforged: rule, bounded-price, target, stone and uniform-selection checks passed")
    }
}
