plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val lwjglNatives = rootProject.extra["lwjglNatives"] as String
val lwjglVersion = libs.versions.lwjgl.get()

dependencies {
    api(platform(libs.lwjgl.bom))

    api(libs.lwjgl)
    api(libs.lwjgl.glfw)
    api(libs.lwjgl.opengl)

    api(libs.joml)

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}