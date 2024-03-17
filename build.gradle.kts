plugins {
    kotlin("jvm") version "1.9.22"

    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.koeltv"
version = "1.0-SNAPSHOT"

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
    mainModule.set("com.koeltv.cottagemanager")
    mainClass.set("com.koeltv.cottagemanager.HelloApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

jlink {
    imageZip = project.file("${buildDir}/distributions/app-${javafx.platform.classifier}.zip")
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    launcher {
        name = "app"
    }
}

tasks.jlinkZip.configure {
    group = "distribution"
}