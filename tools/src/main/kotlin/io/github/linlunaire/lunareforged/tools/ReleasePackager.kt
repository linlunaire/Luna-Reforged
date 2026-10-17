package io.github.linlunaire.lunareforged.tools

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal object ReleasePackager {
    fun pack(root: File) {
        val files = linkedMapOf<String, File>()
        for (version in minecraftVersions) {
            for (loader in loaders) {
                val mod = root.resolve("build/release/$version/Luna-Reforged-$loader-$version-$MOD_VERSION.jar")
                files["$version/$loader/mods/${mod.name}"] = mod
            }
        }
        files["Luna-Reforged-$MOD_VERSION-source.zip"] = root.resolve("build/release/Luna-Reforged-$MOD_VERSION-source.zip")
        for (name in listOf(
            "LICENSE", "NOTICE.md", "README.md", "docs/usage.md", "docs/building.md", "docs/compatibility.md",
            "docs/verification.md", "docs/custom-reforges.md", "docs/licensing.md", "docs/changelog.md",
            "docs/reforges.md", "docs/visual-design.md", "examples/reforges/starforged.json", "docs/evidence/validation.txt",
            "docs/images/luna-reforged.svg", "docs/images/reforging-screen.png", "docs/images/reforging-table.png",
            "docs/images/reforging-success.png", "docs/images/reforging-custom.png", "docs/images/reforging-particles.png",
            "docs/images/reforging-stone.png", "docs/images/reforging-anvil.png", "docs/images/reforging-1.21.1.png",
            "docs/images/reforging-ember.png", "docs/images/reforging-siphoning.png", "docs/images/reforging-feathered.png",
            "docs/images/reforging-nautical-before.png", "docs/images/reforging-nautical.png",
            "docs/images/reforging-nautical-hovered.png", "docs/images/reforging-vulkan-neoforge.png"
        )) files[name] = root.resolve(name)
        for ((name, path) in files) check(path.isFile) { "Missing release file: $name" }
        val manifest = files.mapValues { sha256(it.value) }
        val target = root.resolve("build/release/Luna-Reforged-$MOD_VERSION-release.zip")
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            for ((name, path) in files) {
                zip.putNextEntry(ZipEntry(name).apply { time = path.lastModified() })
                path.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("SHA256.json"))
            zip.write((json.toJson(manifest) + "\n").toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("INSTALL.txt"))
            zip.write(installation.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        ZipFile(target).use { zip ->
            val buffer = ByteArray(8192)
            for (entry in zip.entries().asSequence().filterNot { it.isDirectory }) {
                val crc = CRC32()
                zip.getInputStream(entry).use { input ->
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        crc.update(buffer, 0, count)
                    }
                }
                check(crc.value == entry.crc) { "Corrupt release entry: ${entry.name}" }
            }
        }
        println("Packaged $target (${target.length()} bytes, SHA-256 ${sha256(target)})")
    }

    private val installation = """
        辉月重铸 $MOD_VERSION / Luna Reforged

        只选与你的 Minecraft 版本和加载器匹配的一个目录。
        把该目录 mods 内的辉月重铸 JAR 放入客户端和服务端各自的 mods 文件夹。
        另从 https://github.com/linlunaire/Kotlin-LunaCore/releases/tag/v$CORE_VERSION
        下载同一 Minecraft 版本、同一加载器的 LunaCore $CORE_VERSION，放入两端的 mods 文件夹。
        升级时移除旧核心，并一起更新辉月重铸。

        26.2：Java 25；Fabric Loader 0.19.3+ 或 NeoForge 26.2.0.75+。
        1.21.1：Java 21；Fabric Loader 0.19.3+ 或 NeoForge 21.1.248+。
        Fabric 还需另装对应版本的 Fabric API：0.159.0+26.2 或 0.116.15+1.21.1。
        本包只包含辉月重铸；LunaCore、Minecraft、加载器和 Fabric API 需另外安装。

        26.2 可使用原生 Vulkan。NeoForge 26.2.0.75 需将 config/fml.toml 的 earlyWindowControl 设为 false，
        关闭早期加载窗口，并在视频设置中启用 Vulkan 后重启；详情及实测范围见 docs/usage.md。

        合成配方和费用配置见 docs/usage.md，验证范围见 docs/verification.md。
        本次构建和运行验证结果见 docs/verification.md；开发实例检查不代表所有第三方整合包均兼容。
        本项目完整源码在 Luna-Reforged-$MOD_VERSION-source.zip。LunaCore 的源码与构建由它自己的仓库维护。
    """.trimIndent() + "\n"
}
