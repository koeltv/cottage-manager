package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.db.ClientService
import com.koeltv.cottagemanager.db.CottageService
import com.koeltv.cottagemanager.db.ReservationService
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import kotlin.concurrent.thread

val appModule = module {
    single {
        File("./db").mkdirs()
        Database.connect(
            url = "jdbc:sqlite:./db/testDb.db",
            user = "root",
            password = "pass"
        )
    }
    singleOf(::ClientService)
    singleOf(::CottageService)
    singleOf(::ReservationService)
}

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("home-view.fxml"))

        stage.scene = Scene(fxmlLoader.load(), 650.0, 450.0)
        stage.title = "Cottage Manager ${System.getProperty("jpackage.app-version") ?: "[devMode]"}"
        stage.icons.add(Image("logo.png"))

        stage.show()
    }
}

fun main() {
    thread { Updater.setup() }
    startKoin { modules(appModule) }
    Application.launch(HelloApplication::class.java)
}