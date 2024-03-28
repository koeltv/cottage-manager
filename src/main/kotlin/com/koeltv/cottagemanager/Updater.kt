package com.koeltv.cottagemanager

import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.channels.Channels
import java.nio.file.FileSystems

object Updater {
    fun setup() {
        val version: String? = System.getProperty("jpackage.app-version")

        if (version == null) {
            println("Dev mode, skipping updating process")
            return
        }

        val latestVersion = getLatestVersion() ?: return
        if (latestVersion.newerThan("v$version")) {
            val appDirectory = File("app")
            val updaterFile = appDirectory.list()?.find { "updater" in it }

            if (updaterFile != null) {
                val osExtension = getOsExtension()
                val fileName = "cottage-manager-${latestVersion.removePrefix("v")}-$osExtension.zip"
                val zipUrl = "https://github.com/koeltv/cottage-manager/releases/download/$latestVersion/$fileName"
                downloadFromURL(zipUrl, fileName)

                val javaHome = System.getProperty("java.home")
                val separator = FileSystems.getDefault().separator

                Runtime.getRuntime().addShutdownHook(Thread {
                    ProcessBuilder(
                        "$javaHome${separator}bin${separator}java",
                        "-jar",
                        "${appDirectory.absolutePath}$separator$updaterFile"
                    ).start()
                })
            }
        }
    }

    private infix fun String.newerThan(version: String): Boolean {
        for (i in 1..<length) {
            if (this[i].isDigit() && version[i].isDigit()) {
                val versionDigit = this[i].digitToInt()
                val otherVersionDigit = version[i].digitToInt()
                if (versionDigit > otherVersionDigit) return true
                else if (versionDigit < otherVersionDigit) return false
            }
        }
        return length < version.length
    }

    private fun getOsExtension(): String {
        return System.getProperty("os.name").let {
            when {
                it.contains("win", ignoreCase = true) -> "win"
                it.contains("mac", ignoreCase = true) -> "mac"
                else -> "linux"
            }
        }
    }

    private fun getLatestVersion(): String? {
        return runCatching {
            HttpClient.newHttpClient()
                .send(
                    HttpRequest
                        .newBuilder(URI.create("https://api.github.com/repos/koeltv/cottage-manager/releases/latest"))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                )
        }
            .getOrNull()
            ?.let { response ->
                Regex("\"tag_name\":\"(v[.\\d]+)\"").find(response.body())!!.groupValues[1]
            }
    }

    /**
     * Downloads a file from a given URL and saves it to a specified output file.
     *
     * @param url The URL of the file to download.
     * @param outputFileName The name of the output file where the downloaded file will be saved.
     */
    private fun downloadFromURL(url: String, outputFileName: String): File {
        URL(url).openStream().use {
            Channels.newChannel(it).use { byteChannel ->
                FileOutputStream(outputFileName).use { fileOutputStream ->
                    fileOutputStream.channel.transferFrom(byteChannel, 0, Long.MAX_VALUE)
                }
            }
        }
        return File(outputFileName)
    }
}