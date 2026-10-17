package io.github.linlunaire.lunareforged.tools

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.zip.ZipFile
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal object ArtifactVerifier {
    private const val MOD_PACKAGE = "io/github/linlunaire/lunareforged/"
    private const val CORE_PACKAGE = "io/github/linlunaire/lunacore/"
    private val hooks = mapOf(
        "BlockMixin" to listOf("playerDestroy"),
        "LivingEntityMixin" to listOf("hurt", "die"),
        "ItemStackMixin" to listOf("getHoverName"),
        "AnvilMenuMixin" to listOf("createResult"),
        "AnvilScreenMixin" to listOf("slotChanged", "onNameChanged"),
        "EquipmentMixin" to listOf("onEquipItem")
    )
    // Intermediary method IDs are pinned to the 1.21.1 mappings.
    private val intermediaryHooks = mapOf(
        "BlockMixin" to listOf("method_9556"),
        "LivingEntityMixin" to listOf("method_5643", "method_6078"),
        "ItemStackMixin" to listOf("method_7964"),
        "AnvilMenuMixin" to listOf("method_24928", "method_24923"),
        "AnvilScreenMixin" to listOf("method_7635", "method_2403"),
        "EquipmentMixin" to listOf("method_6116")
    )

    fun verify(root: File, selectedVersion: String?) {
        require(selectedVersion == null || selectedVersion in minecraftVersions) { "Unsupported Minecraft version: $selectedVersion" }
        val rules = root.resolve("domain/src/main/kotlin/io/github/linlunaire/lunareforged/domain/ReforgeRules.kt").readText()
        val affixes = Regex("Reforge\\(\"([a-z_]+)\"").findAll(rules).map { it.groupValues[1] }.toList()
        check(affixes.size == 44 && affixes.toSet().size == 44) { "Expected 44 unique built-in reforges" }
        val rows = mutableListOf<Map<String, Any>>()
        for (version in minecraftVersions.filter { selectedVersion == null || it == selectedVersion }) {
            for (loader in loaders) {
                val path = root.resolve("build/release/$version/Luna-Reforged-$loader-$version-$MOD_VERSION.jar")
                check(path.isFile) { "Missing $path" }
                ZipFile(path).use { zip ->
                    val names = zip.entries().asSequence().map { it.name }.toSet()
                    check(names.none { it.startsWith("kotlin/") || it.startsWith("org/bukkit/") || it.startsWith("com/willfp/") || it.startsWith(CORE_PACKAGE) }) {
                        "Consumer contains a second runtime or plugin implementation: $path"
                    }
                    check(names.none { it.endsWith(".jar") }) { "Consumer must not embed Kotlin or LunaCore: $path" }
                    for (notice in listOf("LICENSE", "NOTICE.md")) {
                        check(zip.text("META-INF/luna-reforged/$notice") == root.resolve(notice).readText()) {
                            "Packaged $notice differs from the current source: $path"
                        }
                    }
                    checkClasses(zip, if (version == "26.2") 69 else 65)
                    val required = listOf(
                        "luna_reforged.mixins.json", "${MOD_PACKAGE}ReforgeMenu.class", "${MOD_PACKAGE}ReforgeText.class",
                        "${MOD_PACKAGE}ReforgeCatalog.class", "${MOD_PACKAGE}ReforgeLore.class",
                        "${MOD_PACKAGE}mixin/ItemStackMixin.class", "${MOD_PACKAGE}client/LunarAnimation.class",
                        "assets/luna_reforged/models/block/reforging_table.json", "assets/luna_reforged/textures/gui/lunar_backdrop.png"
                    )
                    check(names.containsAll(required)) { "Missing code or resource in $path: ${required - names}" }
                    val mixins = zip.json("luna_reforged.mixins.json")
                    val mixinClasses = (mixins.getAsJsonArray("mixins") + mixins.getAsJsonArray("client")).map { it.asString } +
                        if (version == "26.2" && loader == "neoforge") listOf("AnvilMenuNeoForgeMixin") else emptyList()
                    for (mixin in mixinClasses) check(isKotlinClass(zip, "${MOD_PACKAGE}mixin/$mixin.class")) {
                        "Mixin is missing Kotlin metadata: $mixin in $path"
                    }
                    check(isKotlinClass(zip, "${MOD_PACKAGE}ReforgeCombatState.class")) { "Combat state interface must come from Kotlin" }
                    check("${MOD_PACKAGE}mixin/PlayerMixin.class" !in names) { "Obsolete PlayerMixin in $path" }
                    for (item in listOf("reforging_table", "dynamic_stone", "rich_stone", "prospecting_stone")) {
                        check("data/luna_reforged/recipe/$item.json" in names) { "Missing recipe: $item" }
                        if (version == "26.2") check("assets/luna_reforged/items/$item.json" in names) { "Missing item model: $item" }
                    }
                    if (loader == "fabric") {
                        val metadata = zip.json("fabric.mod.json")
                        check(metadata["id"].asString == "luna_reforged" && metadata["version"].asString == MOD_VERSION) { "Invalid Fabric identity in $path" }
                        val depends = metadata.getAsJsonObject("depends")
                        check(depends["minecraft"].asString == version && depends["lunacore"]?.asString == ">=$CORE_VERSION" && !depends.has("transit_core")) { "Invalid Fabric dependencies in $path" }
                        check(metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").map { it.asString } ==
                            listOf("io.github.linlunaire.lunareforged.fabric.LunaFabric")) { "Invalid Fabric entry point" }
                        if (version == "1.21.1") {
                            if (mixins.has("refmap")) {
                                val mappings = zip.json(mixins["refmap"].asString).getAsJsonObject("mappings")
                                for ((mixin, methods) in hooks) {
                                    val mapped = mappings.getAsJsonObject("${MOD_PACKAGE}mixin/$mixin")
                                    check(methods.all { mapped[it]?.asString?.contains("method_") == true }) {
                                        "Legacy Fabric hooks must use intermediary mappings: $mixin"
                                    }
                                }
                            } else {
                                for ((mixin, methods) in intermediaryHooks) {
                                    val targets = injectionTargets(zip, "${MOD_PACKAGE}mixin/$mixin.class")
                                    check(methods.all { method -> targets.any { it == method || "$method(" in it } }) {
                                        "Kotlin Mixin targets were not remapped to 1.21.1 intermediary names: $mixin: $targets"
                                    }
                                    if (mixin in listOf("AnvilMenuMixin", "AnvilScreenMixin")) {
                                        check("Lnet/minecraft/class_1799;method_7964()Lnet/minecraft/class_2561;" in targets) {
                                            "Anvil name redirect was not remapped: $mixin"
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val metadata = zip.text("META-INF/neoforge.mods.toml")
                        check("modId=\"luna_reforged\"" in metadata && "version=\"$MOD_VERSION\"" in metadata) { "Invalid NeoForge identity in $path" }
                        check("modId=\"lunacore\"" in metadata && "versionRange=\"[$CORE_VERSION,)\"" in metadata && "transit_core" !in metadata && "[$version]" in metadata) { "Invalid NeoForge dependencies" }
                        check("${MOD_PACKAGE}neoforge/LunaNeoForge.class" in names) { "Missing NeoForge entry point" }
                        if (version == "26.2") {
                            check("luna_reforged.neoforge.mixins.json" in names && "luna_reforged.neoforge.mixins.json" in metadata &&
                                "${MOD_PACKAGE}mixin/AnvilMenuNeoForgeMixin.class" in names) { "Missing patched NeoForge anvil hook" }
                        }
                    }
                    val en = zip.json("assets/luna_reforged/lang/en_us.json")
                    val zh = zip.json("assets/luna_reforged/lang/zh_cn.json")
                    check(en.keySet() == zh.keySet()) { "Translation keys differ in $path" }
                    check(en.has("stat.luna_reforged.life_steal") && en.has("condition.luna_reforged.rain")) { "Missing effect labels" }
                    check(affixes.all { en["reforge.luna_reforged.$it"].asString.startsWith("<gradient:#") }) { "Missing gradient names" }
                }
                rows += checksum(path)
                println("PASS ${path.name}")
            }
        }
        val checksumName = if (selectedVersion == null) "checksums.json" else "checksums-$selectedVersion.json"
        writeJson(root.resolve("build/release/$checksumName"), rows)
        println("PASS: ${rows.size} artifacts, Java targets, runtime boundary, metadata, mixins, resources and translation parity")
    }

    private fun checkClasses(zip: ZipFile, maximum: Int) {
        for (entry in zip.entries().asSequence().filter { it.name.endsWith(".class") }) {
            zip.getInputStream(entry).buffered().use { input ->
                val header = input.readNBytes(8)
                check(header.size == 8 && header[0].toInt() and 255 == 0xca && header[1].toInt() and 255 == 0xfe &&
                    header[2].toInt() and 255 == 0xba && header[3].toInt() and 255 == 0xbe) { "Invalid class: ${entry.name}" }
                val major = ((header[6].toInt() and 255) shl 8) or (header[7].toInt() and 255)
                check(major <= maximum) { "Java class target $major exceeds $maximum: ${entry.name}" }
            }
        }
    }

    private fun checksum(path: File): Map<String, Any> =
        linkedMapOf("file" to path.name, "size" to path.length(), "sha256" to sha256(path))

    private fun injectionTargets(zip: ZipFile, path: String): Set<String> {
        val entry = zip.getEntry(path) ?: error("Missing Kotlin Mixin: $path")
        val targets = mutableSetOf<String>()
        fun collector(): AnnotationVisitor = object : AnnotationVisitor(Opcodes.ASM9) {
            override fun visit(name: String?, value: Any?) {
                if (value is String) targets += value
            }
            override fun visitArray(name: String?): AnnotationVisitor = collector()
            override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor = collector()
        }
        zip.getInputStream(entry).use { input ->
            ClassReader(input).accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor =
                    object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? =
                            if (descriptor.startsWith("Lorg/spongepowered/asm/mixin/injection/")) collector() else null
                    }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
        return targets
    }

    private fun isKotlinClass(zip: ZipFile, path: String): Boolean {
        var found = false
        val entry = zip.getEntry(path) ?: error("Missing Kotlin class: $path")
        zip.getInputStream(entry).use { input ->
            ClassReader(input).accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                    if (descriptor == "Lkotlin/Metadata;") found = true
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
        return found
    }
}

internal fun ZipFile.text(path: String): String {
    val entry = getEntry(path) ?: error("Missing ZIP entry: $path in $name")
    return getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
internal fun ZipFile.json(path: String): JsonObject = JsonParser.parseString(text(path)).asJsonObject
