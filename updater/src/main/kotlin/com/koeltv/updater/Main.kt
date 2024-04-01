package com.koeltv.updater

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream

const val MAX_RETRIES: Int = 5
const val RETRY_DELAY: Long = 1000

val updateLogFile: File by lazy {
    val formattedDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        .replace(":", "")
        .replace("-", "")
    File("update-$formattedDateTime.log").also { it.createNewFile() }
}

val ignoredFileNames = listOf(
    "app/updater",
    "cottage-manager.exe",
    "runtime/bin/java.dll",
    "runtime/bin/java.exe",
    "runtime/bin/jimage.dll",
    "runtime/bin/jli.dll",
    "runtime/bin/msvcp140.dll",
    "runtime/bin/net.dll",
    "runtime/bin/nio.dll",
    "runtime/bin/server/jvm.dll",
    "runtime/bin/vcruntime",
    "runtime/bin/zip.dll",
    "runtime/lib/modules",
).map {
    it
        .replace('/', File.separatorChar)
        .replace('\\', File.separatorChar)
}

fun main() {
    val currentFile = File("").absoluteFile
    val updateZipPath = currentFile.list()?.find { it.contains(".zip") }

    if (updateZipPath == null) {
        println("No update archive, skipping updating process")
    } else {
        val zipFile = File(updateZipPath)
        updateLogFile.appendText("Starting update using ${zipFile.absolutePath}\n")

        updateLogFile.appendText("Unzipping... ")
        val directory = unzipFile(zipFile)
        updateLogFile.appendText("file unzipped to ${directory.absolutePath}\n")
        zipFile.deleteRecursively()

        updateLogFile.appendText("Updating files...\n")
        copyDirectory(directory, currentFile)
        directory.deleteRecursively()
    }
}

fun unzipFile(file: File, outputDirectory: String = file.nameWithoutExtension): File {
    file.inputStream().use { fileInputStream ->
        ZipInputStream(fileInputStream).use { zipInputStream ->
            generateSequence { zipInputStream.nextEntry }.forEach { zipEntry ->
                val outputFile = File(outputDirectory, zipEntry.name)
                if (zipEntry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.outputStream().use { fileOutputStream ->
                        zipInputStream.copyTo(fileOutputStream)
                    }
                }
            }
        }
    }
    return File(outputDirectory)
}

fun copyDirectory(sourceDirectory: File, targetDirectory: File) {
    val sourceDirectoryPath = sourceDirectory.toPath()
    val targetDirectoryPath = targetDirectory.toPath()

    Files.walkFileTree(sourceDirectoryPath, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val targetPath = targetDirectoryPath.resolve(sourceDirectoryPath.relativize(dir))
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath)
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            repeat(MAX_RETRIES) { retries ->
                try {
                    Files.copy(
                        file,
                        targetDirectoryPath.resolve(sourceDirectoryPath.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    return FileVisitResult.CONTINUE
                } catch (_: FileSystemException) {
                    if (file.shouldBeSkipped()) {
                        updateLogFile.appendText("Skipped file: $file\n")
                        return FileVisitResult.CONTINUE
                    }
                    if (retries >= MAX_RETRIES - 1) {
                        updateLogFile.appendText("Skipped file after $MAX_RETRIES attempts: $file\n")
                        return FileVisitResult.CONTINUE
                    }
                    Thread.sleep(RETRY_DELAY)
                }
            }
            return FileVisitResult.CONTINUE
        }
    })
}

private fun Path.shouldBeSkipped(): Boolean = this.matchAny(ignoredFileNames)

private fun Path.matchAny(strings: List<String>) = strings.any { this.toString().contains(it) }
