import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
}
repositories { mavenCentral() }
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
kotlin {
    coreLibrariesVersion = "2.4.0"
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.ow2.asm:asm:9.8")
}

val repoRoot = layout.projectDirectory.dir("..").asFile
fun registerTool(name: String, command: String) = tasks.register<JavaExec>(name) {
    group = "Luna Reforged"
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.linlunaire.lunareforged.tools.BuildToolsKt")
    args(command, repoRoot.absolutePath)
}
registerTool("generateAssets", "assets")
registerTool("verifyArtifacts", "verify").configure {
    providers.gradleProperty("minecraftVersion").orNull?.let { args(it) }
}
registerTool("packageSources", "sources")
registerTool("packageRelease", "release").configure {
    dependsOn("verifyArtifacts", "packageSources")
    doFirst {
        check(!providers.gradleProperty("minecraftVersion").isPresent) {
            "packageRelease requires all four builds; omit -PminecraftVersion."
        }
    }
}
