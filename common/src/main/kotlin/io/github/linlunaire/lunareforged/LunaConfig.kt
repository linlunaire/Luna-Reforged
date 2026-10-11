package io.github.linlunaire.lunareforged

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.github.linlunaire.lunareforged.domain.PricePolicy
import java.nio.file.Files
import java.nio.file.Path

object LunaConfig {
    var prices = PricePolicy(); private set
    var includeNativeReforges = true; private set
    fun load(directory: Path) {
        val file = directory.resolve("luna-reforged.json")
        Files.createDirectories(directory)
        if (!Files.exists(file)) {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val defaults = gson.toJsonTree(PricePolicy()).asJsonObject
            defaults.addProperty("includeNativeReforges", true)
            Files.writeString(file, gson.toJson(defaults) + "\n")
        }
        val json = JsonParser.parseString(Files.readString(file)).asJsonObject
        val allowed = setOf("baseLevels", "growth", "maxLevels", "stoneLevels", "includeNativeReforges")
        require(json.keySet().all(allowed::contains)) { "Unknown field in $file. Supported: $allowed" }
        fun integer(name: String, default: Int): Int = json[name]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "$file: $name must be a number" }
            val value = it.asDouble
            require(value.isFinite() && value == value.toInt().toDouble()) { "$file: $name must be an integer" }
            value.toInt()
        } ?: default
        val growth = json["growth"]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "$file: growth must be a number" }
            it.asDouble
        } ?: 1.15
        val nextPrices = PricePolicy(integer("baseLevels", 3), growth, integer("maxLevels", 30), integer("stoneLevels", 40))
        val nextIncludeNative = json["includeNativeReforges"]?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "includeNativeReforges must be boolean" }
            it.asBoolean
        } ?: true
        ReforgeCatalog.load(directory)
        // The catalog publishes only after full validation. Commit the scalar policy last,
        // on this same server thread, so a rejected reload leaves all live settings intact.
        prices = nextPrices
        includeNativeReforges = nextIncludeNative
    }
    fun reset() { prices = PricePolicy(); includeNativeReforges = true; ReforgeCatalog.reset() }
}
