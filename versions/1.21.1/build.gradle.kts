import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    base
    id("dev.architectury.loom") version "1.11.458" apply false
    kotlin("jvm") version "2.4.20" apply false
}
val projectRoot = file("../..")
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val modVersion = providers.gradleProperty("mod_version").get()

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    val corePort = name.startsWith("lunacore-")
    val loader = if (name.endsWith("fabric")) "fabric" else "neoforge"
    group = "io.github.linlunaire"
    version = if (corePort) "0.4.0+mc1.21.1" else modVersion
    extensions.configure<BasePluginExtension> {
        archivesName.set((if (corePort) "Kotlin-LunaCore" else "Luna-Reforged") + "-$loader-1.21.1")
    }
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }
    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
        coreLibrariesVersion = "2.4.0"
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        sourceSets.named("main") {
            if (corePort) kotlin.srcDir(projectRoot.resolve("compat/lunacore/src/main/kotlin"))
            else kotlin.srcDirs(
                projectRoot.resolve("domain/src/main/kotlin"),
                projectRoot.resolve("common/src/main/kotlin"),
                rootProject.file("src/main/kotlin")
            )
        }
        if (!corePort) sourceSets.named("test") {
            kotlin.srcDir(projectRoot.resolve("domain/src/test/kotlin"))
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://neoforged.forgecdn.net/releases/")
        maven("https://maven.neoforged.net/releases/")
    }
    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    // Remap Kotlin Mixin annotation targets directly, without Java annotation processing.
    loom.mixin.useLegacyMixinAp.set(false)
    dependencies {
        add("minecraft", "com.mojang:minecraft:$minecraftVersion")
        add("mappings", loom.officialMojangMappings())
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
        if (loader == "fabric") {
            add("modImplementation", "net.fabricmc:fabric-loader:" + rootProject.property("fabric_loader_version"))
            if (!corePort) add("modImplementation", "net.fabricmc.fabric-api:fabric-api:" + rootProject.property("fabric_api_version"))
        } else add("neoForge", "net.neoforged:neoforge:" + rootProject.property("neoforge_version"))
        if (corePort) add("include", "org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    }
    val sources = extensions.getByType<SourceSetContainer>()
    if (!corePort) {
        sources.named("main") {
            resources.srcDirs(projectRoot.resolve("common/src/main/resources"), rootProject.file("src/main/resources"))
        }
        tasks.register<JavaExec>("checkRules") {
            dependsOn("testClasses")
            classpath = sources["test"].runtimeClasspath
            mainClass.set("io.github.linlunaire.lunareforged.domain.RulesCheck")
        }
        tasks.named("check") { dependsOn("checkRules") }
        tasks.named<Test>("test") {
            enabled = false
            dependsOn("checkRules")
        }
    }
    tasks.named<ProcessResources>("processResources") {
        inputs.property("version", project.version)
        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand(mapOf("version" to project.version))
        }
    }
    tasks.named<Jar>("jar") {
        if (corePort) {
            from(files(projectRoot.resolve("compat/lunacore/LICENSE"), projectRoot.resolve("compat/lunacore/NOTICE.md"))) {
                into("META-INF/lunacore")
            }
            from(projectRoot.resolve("compat/lunacore/licenses")) { into("META-INF/lunacore/licenses") }
        } else {
            from(files(projectRoot.resolve("LICENSE"), projectRoot.resolve("NOTICE.md"))) { into("META-INF/luna-reforged") }
        }
    }
    tasks.register<Copy>("releaseJar") {
        dependsOn("remapJar", "check", "sourcesJar")
        from(tasks.named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
        from(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(projectRoot.resolve("build/release/1.21.1"))
    }
    tasks.named("build") { dependsOn("releaseJar") }

    loom.mods.maybeCreate("main").sourceSet(sources["main"])
    loom.runs.configureEach {
        if (loader == "neoforge") {
            environmentVariable("MOD_CLASSES", sources["main"].output.files.joinToString(File.pathSeparator) { "main%%" + it.absolutePath })
        }
    }
    loom.runs.named("server") {
        if (rootProject.hasProperty("verify")) {
            vmArg("-Dluna_reforged.verify=true")
            vmArg("-Xmx2G")
            programArg("nogui")
        }
        if (!corePort && rootProject.hasProperty("preview")) {
            vmArg("-Dluna_reforged.preview=true")
            programArg("nogui")
        }
    }
    if (!corePort) loom.runs.named("client") {
        if (rootProject.hasProperty("preview")) {
            runDir("run/client-preview")
            vmArg("-Dluna_reforged.preview=true")
            vmArg("-Xmx2G")
            programArgs("--username", "LunaPreview", "--quickPlayMultiplayer",
                "127.0.0.1:" + if (loader == "fabric") "25762" else "25763")
        }
    }
    tasks.named<JavaExec>("runServer") {
        doFirst {
            if (rootProject.hasProperty("verify")) systemProperty("luna_reforged.verify", "true")
            if (rootProject.hasProperty("preview")) systemProperty("luna_reforged.preview", "true")
        }
    }
    if (!corePort) tasks.named<JavaExec>("runClient") {
        doFirst { if (rootProject.hasProperty("preview")) systemProperty("luna_reforged.preview", "true") }
    }
}
