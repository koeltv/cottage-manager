import org.gradle.internal.os.OperatingSystem
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    kotlin("jvm") version "1.9.22"

    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.runtime") version "1.13.1"
}

group = "com.koeltv"
version = "2.4"

val author = "koeltv"
val vendor = "Valentin Koeltgen"
val projectUrl = "https://github.com/koeltv/cottage-manager"

val exposedVersion: String by project
val sqliteDriverVersion: String by project
val controlFxVersion: String by project
val logbackVersion: String by project
val openpdfVersion: String by project

val currentOs: OperatingSystem = OperatingSystem.current()

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

    implementation("org.controlsfx:controlsfx:$controlFxVersion")

    implementation("org.xerial:sqlite-jdbc:${sqliteDriverVersion}")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("com.github.librepdf:openpdf:$openpdfVersion")

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
    modules = listOf(
        "java.desktop",
        "java.xml",
        "jdk.unsupported",
        "java.scripting",
        "java.net.http",
        "java.sql",
        "java.logging",
        "java.naming",
        // Needed for update mechanism
        "jdk.crypto.ec",
        "java.datatransfer",
        "jdk.zipfs",
    )

    launcher {
        noConsole = true
    }

    jpackage {
        imageName = rootProject.name
        val imgType = if (currentOs.isWindows) "ico" else "png"
        imageOptions = listOf(
            "--vendor", vendor,
            "--icon", "src/main/resources/logo.$imgType",
            "--copyright", vendor,
            "--description", "facilite la gestion de locations sur plusieurs gites simultanement"
        )

        installerName = "${rootProject.name}-installer"
        installerOptions = mutableListOf(
            "--license-file", "LICENSE",
            "--about-url", projectUrl
        ).also {
            if (currentOs.isWindows) {
                it += listOf(
                    "--win-per-user-install",
                    "--win-menu",
                    "--win-menu-group", author,
                    "--win-shortcut",
                    "--win-help-url", projectUrl
                )
            } else if (currentOs.isLinux) {
                it += listOf(
                    "--linux-package-name", project.name,
                    "--linux-shortcut"
                )
            } else if (currentOs.isMacOsX) {
                it += listOf(
                    "--mac-package-name", project.name
                )
            }
        }
    }
}

tasks.register("bundleUpdater") {
    description = "Bundle the updater jar into the jpackage image"
    group = JavaBasePlugin.BUILD_TASK_NAME

    dependsOn(tasks.jpackageImage)
    dependsOn(":updater:shadowJar")

    doLast {
        val updaterLibDirectory = project.file("${projectDir}/updater/build/libs")
        updaterLibDirectory.absoluteFile.list()
            ?.first { "all.jar" in it }
            ?.let {
                val updaterJarFile = File("$updaterLibDirectory${FileSystems.getDefault().separator}$it")
                updaterJarFile.copyTo(
                    project.file("${buildDir}/jpackage/${project.name}/app/${updaterJarFile.name}"),
                    overwrite = true
                )
            }
    }
}

tasks.jpackage {
    dependsOn("bundleUpdater")
}

tasks.register("jpackageZip") {
    description = "Create a .zip archive from the jpackage image"
    group = JavaBasePlugin.BUILD_TASK_NAME

    dependsOn(tasks.jpackage)

    doLast {
        val osExtension = if (currentOs.isWindows) "win"
        else if (currentOs.isMacOsX) "mac"
        else "linux"

        zipAll(
            sourceFile = project.file("${buildDir}/jpackage/${project.name}"),
            outputZipFile = project.file("${buildDir}/jpackage/${project.name}-$version-${osExtension}.zip")
        )
    }
}

fun zipAll(sourceFile: File, outputZipFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zipOutputStream ->
        sourceFile.walkTopDown().forEach { file ->
            val zipFileName =
                file.absolutePath.removePrefix(sourceFile.absolutePath).removePrefix("/").removePrefix("\\")
            val entry = ZipEntry("$zipFileName${if (file.isDirectory) "/" else ""}")
            zipOutputStream.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().copyTo(zipOutputStream)
            }
        }
    }
}

tasks.register("version") {
    description = "Return the current version of the project"
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    doLast { println("v$version") }
}