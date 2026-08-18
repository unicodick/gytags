import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

evaluationDependsOn(":common")

plugins {
    `java-library`
    alias(libs.plugins.neoforge.moddev)
    id("com.modrinth.minotaur")
}

val common = project(":common")
val commonSourceSet = common.extensions.getByType<SourceSetContainer>().named("main")
val commonCompileJava = common.tasks.named<JavaCompile>(commonSourceSet.get().compileJavaTaskName)
val modVersion = providers.gradleProperty("mod_version").get()
val gameMinecraftVersion = libs.versions.minecraft.get()
val neoForgeVersion = libs.versions.neoforge.get()

base {
    archivesName.set("${rootProject.name}-neoforge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

sourceSets["main"].resources.exclude("META-INF/neoforge.mods.toml")

neoForge {
    version = neoForgeVersion
    parchment {
        mappingsVersion = "2025.12.20"
        minecraftVersion = gameMinecraftVersion
    }
    runs {
        create("client") {
            client()
            gameDirectory = layout.projectDirectory.dir("../run/neoforge").asFile
        }
    }
    mods {
        create("gutags") {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
}

sourceSets["main"].compileClasspath += commonSourceSet.get().output
sourceSets["main"].runtimeClasspath += commonSourceSet.get().output

tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("../common/src/main/resources"))
    dependsOn(":common:processResources")
    inputs.properties(
        mapOf(
            "mod_version" to modVersion,
            "neo_version" to neoForgeVersion,
            "minecraft_version" to gameMinecraftVersion
        )
    )
    from(layout.projectDirectory.dir("src/main/resources")) {
        exclude("META-INF/neoforge.mods.toml")
    }
    from(layout.projectDirectory.dir("src/main/resources/META-INF")) {
        include("neoforge.mods.toml")
        into("META-INF")
        expand(
            "mod_version" to modVersion,
            "neo_version" to neoForgeVersion,
            "minecraft_version" to gameMinecraftVersion,
            "minecraft_version_range" to "[$gameMinecraftVersion]"
        )
    }
}

tasks.named<Jar>("jar") {
    from(commonCompileJava.map { it.destinationDirectory })
    from(layout.projectDirectory.file("../LICENSE")) {
        rename { "${it}_gutags" }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("kOWoPaqf")
    versionNumber.set(modVersion)
    versionName.set("Gorodurodov Tags $modVersion (NeoForge)")
    versionType.set("release")
    uploadFile.set(tasks.named("jar"))
    gameVersions.add(gameMinecraftVersion)
    loaders.add("neoforge")
}
