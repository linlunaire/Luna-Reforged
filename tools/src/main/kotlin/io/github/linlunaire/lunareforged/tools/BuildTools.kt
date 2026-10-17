package io.github.linlunaire.lunareforged.tools

import com.google.gson.GsonBuilder
import java.io.File
import java.security.MessageDigest

internal val json = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
internal const val MOD_VERSION = "0.5.1"
internal const val CORE_VERSION = "0.4.1"
internal val loaders = listOf("fabric", "neoforge")
internal val minecraftVersions = listOf("26.2", "1.21.1")

fun main(args: Array<String>) {
    require(args.size >= 2) { "Usage: <assets|verify|sources|release> <repository> [arguments]" }
    val root = File(args[1]).canonicalFile
    when (args[0]) {
        "assets" -> AssetGenerator.generate(root)
        "verify" -> ArtifactVerifier.verify(root, args.getOrNull(2))
        "sources" -> SourceArchives.create(root, root.resolve("build/release/Luna-Reforged-$MOD_VERSION-source.zip"), "Luna-Reforged")
        "release" -> {
            require(args.size == 2) { "release only requires this repository" }
            ReleasePackager.pack(root)
        }
        else -> error("Unknown command: ${args[0]}")
    }
}

internal fun writeJson(path: File, value: Any) {
    path.parentFile.mkdirs()
    path.writeText(json.toJson(value) + "\n", Charsets.UTF_8)
}

internal fun sha256(path: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
}
