package io.github.linlunaire.lunareforged

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import io.github.linlunaire.lunareforged.domain.Reforge
import io.github.linlunaire.lunareforged.domain.ReforgeRules
import io.github.linlunaire.lunareforged.domain.Target
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/** Server-owned roll pool; custom item snapshots travel through native data components. */
object ReforgeCatalog {
    private var definitions = ReforgeRules.definitions
    fun candidates(targets: Set<Target>, current: String?, stone: String?): List<Reforge> =
        definitions.filter { ReforgeRules.eligible(it, targets, current, stone) &&
            (LunaConfig.includeNativeReforges || it.id in ReforgeRules.upstreamIds || ReforgeRules.find(it.id) == null) }

    fun load(configDirectory: Path) {
        val directory = configDirectory.resolve("luna-reforged/reforges")
        Files.createDirectories(directory)
        val example = directory.resolve("_example.json")
        if (!Files.exists(example)) Files.writeString(example, ReforgeJson.encode(Reforge("starforged", setOf(Target.MELEE),
            damage = 1.06, critical = 1.05, name = "<gradient:#C8B6FF>星铸</gradient:#7DE2FC>", description = listOf("&a+6% &f近战伤害", "&a+5% &f下落攻击伤害"))) + "\n")
        val paths = Files.list(directory).use { files ->
            files.filter { it.fileName.toString().endsWith(".json") && !it.fileName.toString().startsWith("_") }.sorted().toList()
        }
        require(paths.size <= 256) { "$directory: at most 256 custom reforges" }
        val loaded = paths.map { path ->
            try {
                require(Files.size(path) <= 16_384) { "file is larger than 16 KiB" }
                val definition = ReforgeJson.parse(Files.readString(path))
                require(path.fileName.toString() == definition.id + ".json") { "filename must match id" }
                require(ReforgeRules.find(definition.id) == null) { "built-in ID is reserved" }
                require(definition.name.isNotBlank()) { "custom reforge requires name" }
                require(!definition.stoneOnly) { "custom stone items are not supported; omit stoneOnly" }
                definition
            } catch (error: Exception) { throw IllegalArgumentException("Invalid reforge $path: ${error.message}", error) }
        }
        require(loaded.map { it.id }.distinct().size == loaded.size) { "Duplicate custom reforge ID" }
        // Publish only after the entire directory validates. Never execute a partial config.
        definitions = ReforgeRules.definitions + loaded
    }
    fun reset() { definitions = ReforgeRules.definitions }
}

/** Strict native schema, also used by the saved/networked immutable custom profile. */
object ReforgeJson {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val fields = setOf("id", "targets", "name", "description", "stoneOnly", "damage", "critical", "incoming", "speed", "attackSpeed", "knockback", "condition", "chance", "killXp", "miningXp", "miningSpeed", "knockbackResistance", "knockbackMultiplier", "incomingScope", "burnSeconds", "slowTicks", "lifeSteal", "procCooldown")
    val CODEC: Codec<Reforge> = Codec.STRING.flatXmap({ raw ->
        try { DataResult.success(parse(raw)) } catch (error: Exception) { DataResult.error { error.message ?: "Invalid reforge profile" } }
    }, { profile ->
        try { DataResult.success(encode(profile)) } catch (error: Exception) { DataResult.error { error.message ?: "Invalid reforge profile" } }
    })

    fun parse(raw: String): Reforge {
        require(raw.length <= 4096) { "reforge exceeds 4096 characters" }
        val json = JsonParser.parseString(raw).asJsonObject
        require(json.keySet().all(fields::contains)) { "unknown fields: ${json.keySet() - fields}" }
        fun string(key: String, default: String = ""): String = json[key]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "$key must be a string" }; it.asString
        } ?: default
        fun number(key: String, default: Double): Double = json[key]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "$key must be a number" }; it.asDouble
        } ?: default
        fun integer(key: String, default: Int = 0): Int {
            val value = number(key, default.toDouble())
            require(value.isFinite() && value == value.toInt().toDouble()) { "$key must be an integer" }
            return value.toInt()
        }
        val targets = json.getAsJsonArray("targets")?.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "targets must contain strings" }
            Target.valueOf(it.asString.uppercase(Locale.ROOT))
        }?.toSet() ?: error("targets is required")
        val descriptions = json.getAsJsonArray("description")?.map {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "description must contain strings" }; it.asString
        } ?: emptyList()
        val stoneOnly = json["stoneOnly"]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "stoneOnly must be boolean" }; it.asBoolean
        } ?: false
        val profile = Reforge(string("id"), targets, stoneOnly, number("damage", 1.0), number("critical", 1.0), number("incoming", 1.0),
            number("speed", 0.0), number("attackSpeed", 0.0), number("knockback", 0.0), string("condition", "always"),
            number("chance", 1.0), integer("killXp"), integer("miningXp"), number("miningSpeed", 0.0), number("knockbackResistance", 0.0), string("name"), descriptions,
            number("knockbackMultiplier", 1.0), string("incomingScope", "all"), integer("burnSeconds"), integer("slowTicks"), number("lifeSteal", 0.0), integer("procCooldown", 20))
        // Defaults and escaping can make the wire representation larger than the input file.
        encode(profile)
        return profile
    }
    fun encode(profile: Reforge): String {
        val json: JsonObject = gson.toJsonTree(profile).asJsonObject
        json.add("targets", gson.toJsonTree(profile.targets.map { it.name.lowercase(Locale.ROOT) }))
        // Keep old profiles within their original wire budget when the new effects are unused.
        if (profile.burnSeconds == 0) json.remove("burnSeconds")
        if (profile.slowTicks == 0) json.remove("slowTicks")
        if (profile.lifeSteal == 0.0) json.remove("lifeSteal")
        if (profile.procCooldown == 20) json.remove("procCooldown")
        val encoded = gson.toJson(json)
        require(encoded.length <= 4096) { "serialized reforge exceeds the 4096-character component limit" }
        return encoded
    }
}
