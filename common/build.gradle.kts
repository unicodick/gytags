plugins {
    `java-library`
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName.set("${rootProject.name}-common")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${libs.versions.minecraft.get()}")
    add("mappings", loom.officialMojangMappings())
    add("compileOnly", "net.fabricmc:sponge-mixin:${libs.versions.mixin.get()}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.jar {
    enabled = true
}

tasks.named("remapJar") {
    enabled = false
}
