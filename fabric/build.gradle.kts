import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

evaluationDependsOn(":common")

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.modrinth)
}

val common = project(":common")
val commonSourceSet = common.extensions.getByType<SourceSetContainer>().named("main")
val commonCompileJava = common.tasks.named<JavaCompile>(commonSourceSet.get().compileJavaTaskName)
val minecraftVersion = libs.versions.minecraft.get()
val artifactVersion = project.version.toString()

base {
    archivesName.set("${rootProject.name}-fabric")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

sourceSets["main"].resources.exclude("fabric.mod.json")

loom {
    mods {
        register("gutags") {
            sourceSet(sourceSets["main"])
        }
    }
    runs {
        named("client") {
            client()
            runDirectory = file("../run/fabric")
        }
    }
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${libs.versions.minecraft.get()}")
    add("mappings", loom.officialMojangMappings())
    add("modImplementation", "net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")
    add("modImplementation", "net.fabricmc.fabric-api:fabric-api:${libs.versions.fabric.api.get()}")
}

sourceSets["main"].compileClasspath += commonSourceSet.get().output
sourceSets["main"].runtimeClasspath += commonSourceSet.get().output

tasks.named<ProcessResources>("processResources") {
    from(layout.projectDirectory.dir("../common/src/main/resources"))
    dependsOn(":common:processResources")
    inputs.property("version", artifactVersion)
    from(layout.projectDirectory.dir("src/main/resources")) {
        exclude("fabric.mod.json")
    }
    from(layout.projectDirectory.file("src/main/resources/fabric.mod.json")) {
        expand("version" to artifactVersion)
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
    versionNumber.set("$artifactVersion-${project.name}")
    versionName.set("Gorodurodov Tags $artifactVersion (Fabric/Quilt)")
    versionType.set("release")
    uploadFile.set(tasks.named("remapJar"))
    gameVersions.add(minecraftVersion)
    loaders.addAll("fabric", "quilt")
    dependencies {
        required.project("fabric-api")
    }
}
