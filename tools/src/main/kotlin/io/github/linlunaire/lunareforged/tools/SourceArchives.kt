package io.github.linlunaire.lunareforged.tools

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object SourceArchives {
    /** Include tracked and unignored sources, preserving the wrapper and license texts. */
    fun create(root: File, target: File, prefix: String) {
        val process = ProcessBuilder("git", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
            .directory(root)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val names = process.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        check(process.waitFor() == 0) { "Cannot enumerate source files in $root" }
        target.parentFile.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            for (name in names.split('\u0000').filter(String::isNotEmpty).distinct().sorted()) {
                val source = root.resolve(name).canonicalFile
                check(source.toPath().startsWith(root.canonicalFile.toPath())) { "Source path escapes repository: $name" }
                if (!source.isFile) continue
                check(source != target.canonicalFile) { "Archive must be ignored by Git: $target" }
                zip.putNextEntry(ZipEntry("$prefix/$name").apply { time = source.lastModified() })
                source.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        println("Packaged $target")
    }
}
