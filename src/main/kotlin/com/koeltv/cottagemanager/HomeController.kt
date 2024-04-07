package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.AirbnbReservation
import com.koeltv.cottagemanager.data.AirbnbReservation.Status
import com.koeltv.cottagemanager.db.ReservationService
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class HomeController : KoinComponent {
    @FXML
    lateinit var stackPane: StackPane

    val reservationService: ReservationService by inject()

    @FXML
    private fun onCottageButtonClick() {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("cottage-view.fxml"))
        stackPane.children.add(fxmlLoader.load())
    }

    @FXML
    private fun onReservationButtonClick() {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("reservation-view.fxml"))
        stackPane.children.add(fxmlLoader.load())
    }

    @FXML
    private fun onClientButtonClick() {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("client-view.fxml"))
        stackPane.children.add(fxmlLoader.load())
    }

    @FXML
    private fun onImportButtonClick(event: ActionEvent) {
        val fileChooser = FileChooser().apply {
            title = "Open Airbnb file"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("CSV", "*.csv")
            )
        }

        val stage = (event.source as Node).scene.window as Stage
        val files: List<File> = fileChooser.showOpenMultipleDialog(stage) ?: return
        println(files)

        for (file in files) {
            AirbnbReservation.fromCsv(file.inputStream())
                .filter { !Status.CANCELED.match(it.status) }
                .forEach { reservationService.importAirbnbReservation(it) }
        }
    }


}