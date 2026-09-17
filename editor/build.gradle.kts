plugins {
    id("application")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass= "atomixsoft.dev.cosmos.editor.Editor"

    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED"
    )
}

val osName = System.getProperty("os.name")

val imguiNatives = when {
    osName.startsWith("Windows") -> libs.imgui.natives.windows
    osName.startsWith("Mac") -> libs.imgui.natives.macos
    osName.startsWith("Linux") -> libs.imgui.natives.linux
    else -> error("Unsupported operating system: $osName")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.imgui.binding)
    implementation(libs.imgui.lwjgl3)

    runtimeOnly(imguiNatives)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}