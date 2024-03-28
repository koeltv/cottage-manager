package com.koeltv.updater

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream

fun main() {
    val zipPath = File("").absoluteFile.list()?.find { it.contains(".zip") }

    if (zipPath == null) {
        println("No update archive, skipping updating process")
    } else {
        val zipFile = File(zipPath)

        val directory = unzipFile(zipFile)
        zipFile.deleteRecursively()

        copyDirectory(directory, File(""))
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

    val formattedDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        .replace(":", "")
        .replace("-", "")
    val updateLogFile = File("update-$formattedDateTime.log")
    updateLogFile.createNewFile()

    Files.walkFileTree(sourceDirectoryPath, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val targetPath = targetDirectoryPath.resolve(sourceDirectoryPath.relativize(dir))
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath)
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            try {
                Files.copy(
                    file,
                    targetDirectoryPath.resolve(sourceDirectoryPath.relativize(file)),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: FileSystemException) {
                updateLogFile.appendText("Skipped file: $file\n")
                return FileVisitResult.CONTINUE
            }
            return FileVisitResult.CONTINUE
        }
    })
}
