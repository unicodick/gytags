plugins {
    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.neoforge.moddev) apply false
    alias(libs.plugins.modrinth) apply false
}

val modVersion = providers.gradleProperty("mod_version").get()
val minecraftVersion = libs.versions.minecraft.get()

allprojects {
    group = providers.gradleProperty("maven_group").get()
    version = "$modVersion+mc$minecraftVersion"
}
