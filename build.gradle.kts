group = "atomixsoft.dev"
version = "1.0-SNAPSHOT"

val osName = System.getProperty("os.name")

val lwjglNatives = when {
    osName.startsWith("Windows") -> "natives-windows"
    osName.startsWith("Mac") -> "natives-macos"
    osName.startsWith("Linux") -> "natives-linux"
    else -> error("Unsupported operating system: $osName")
}

val imguiNatives = when {
    osName.startsWith("Windows") -> "natives-windows"
    osName.startsWith("Mac") -> "natives-macos"
    osName.startsWith("Linux") -> "natives-linux"
    else -> error("Unsupported operating system: $osName")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

extra["lwjglNatives"] = lwjglNatives
extra["imguiNatives"] = imguiNatives