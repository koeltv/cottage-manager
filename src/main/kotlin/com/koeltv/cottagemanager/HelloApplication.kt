package com.koeltv.cottagemanager

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("home-view.fxml"))

        stage.scene = Scene(fxmlLoader.load(), 650.0, 450.0)
        stage.title = "Cottage Manager"
        stage.icons.add(Image("logo.png"))

        DatabaseManager.init()

        stage.show()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}