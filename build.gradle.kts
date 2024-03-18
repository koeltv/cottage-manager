plugins {
    kotlin("jvm") version "1.9.22"

    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.runtime") version "1.13.1"
}

group = "com.koeltv"
version = "1.0"

val author = "koeltv"
val vendor = "Valentin Koeltgen"

val exposedVersion: String by project
val sqliteDriverVersion: String by project

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
//    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:${sqliteDriverVersion}")

    implementation("ch.qos.logback:logback-classic:1.5.3")

    implementation("com.github.librepdf:openpdf:2.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.koeltv.cottagemanager.HelloApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

runtime {
    imageZip = project.file("${buildDir}/distributions/app-${javafx.platform.classifier}.zip")
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
//    modules = listOf() TODO Add modules manually

    launcher {
        noConsole = true
    }

    jpackage {
        val currentOs = org.gradle.internal.os.OperatingSystem.current()

        imageName = rootProject.name
        val imgType = if(currentOs.isWindows) "ico" else "png"
        imageOptions = listOf(
            "--vendor", vendor,
            "--icon", "src/main/resources/logo.$imgType",
//            "copyright", "",
//            "description", ""
            )

        installerName = rootProject.name
        val myInstallerOptions = mutableListOf<String>(
//            "--license-file", "path/to/file"
        )
        if (currentOs.isWindows) {
            myInstallerOptions += listOf("--win-per-user-install", "--win-menu", "--win-menu-group", author, "--win-shortcut")
        } else if (currentOs.isLinux) {
            myInstallerOptions += listOf("--linux-package-name", author, "--linux-shortcut")
        } else if (currentOs.isMacOsX) {
            myInstallerOptions += listOf("--mac-package-name", author)
        }

        installerOptions = myInstallerOptions
    }
}

tasks.register("version") {
    doLast { println("v$version") }
}