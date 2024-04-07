package com.koeltv.cottagemanager.controller

import com.koeltv.cottagemanager.HelloApplication
import com.koeltv.cottagemanager.data.Reservation
import com.koeltv.cottagemanager.db.ClientService
import com.koeltv.cottagemanager.db.CottageService
import com.koeltv.cottagemanager.db.ReservationService
import com.koeltv.cottagemanager.io.PdfExporter
import com.koeltv.cottagemanager.toPlusNote
import com.koeltv.cottagemanager.toPriceString
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import org.jetbrains.exposed.dao.EntityChangeType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull


class ReservationController : Initializable, KoinComponent, Stackable {
    companion object {
        const val ALL_COTTAGES = "Tous les gîtes"
    }

    @FXML
    override lateinit var root: BorderPane

    @FXML
    lateinit var cottageSelectionField: ChoiceBox<String>

    @FXML
    lateinit var arrivalDate: TableColumn<Reservation, LocalDate>

    @FXML
    lateinit var departureDate: TableColumn<Reservation, LocalDate>

    @FXML
    lateinit var client: TableColumn<Reservation, String>

    @FXML
    lateinit var repartition: TableColumn<Reservation, String>

    @FXML
    lateinit var nationality: TableColumn<Reservation, String>

    @FXML
    lateinit var price: TableColumn<Reservation, String>

    @FXML
    lateinit var note: TableColumn<Reservation, String>

    @FXML
    lateinit var code: TableColumn<Reservation, String>

    @FXML
    lateinit var comments: TableColumn<Reservation, String>

    @FXML
    lateinit var actions: TableColumn<Reservation, Unit>

    @FXML
    lateinit var tableView: TableView<Reservation>

    val reservationService: ReservationService by inject()
    val cottageService: CottageService by inject()
    val clientService: ClientService by inject()

    @FXML
    private fun onBackButtonClick() = unstack()

    @FXML
    private fun onCreationButtonClick() {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("reservation-edit-view.fxml"))
        fxmlLoader.setController(ReservationCreateController())
        stack(fxmlLoader.load())
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        arrivalDate.cellValueFactory = PropertyValueFactory("arrivalDate")
        departureDate.cellValueFactory = PropertyValueFactory("departureDate")
        client.setCellValueFactory { SimpleStringProperty(it.value.client.name) }
        repartition.setCellValueFactory { SimpleStringProperty("${it.value.adultCount}A, ${it.value.childCount}E, ${it.value.babyCount}BB") }
        nationality.setCellValueFactory { SimpleStringProperty(it.value.client.nationality) }
        price.setCellValueFactory { SimpleStringProperty(it.value.price.toUInt().toPriceString()) }
        note.setCellValueFactory { SimpleStringProperty(it.value.note?.toUByte()?.toPlusNote() ?: "") }
        code.cellValueFactory = PropertyValueFactory("code")
        comments.cellValueFactory = PropertyValueFactory("comments")
        setupActionColumn()

        cottageSelectionField.items.add(ALL_COTTAGES)
        cottageSelectionField.value = ALL_COTTAGES
        cottageService.readAll().forEach { cottageSelectionField.items.add(it.alias) }

        fetchReservations()
        cottageSelectionField.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            if (oldValue != newValue) {
                if (newValue == ALL_COTTAGES) {
                    fetchReservations()
                } else {
                    fetchReservations(newValue)
                }
            }
        }

        arrivalDate.sortType = TableColumn.SortType.DESCENDING
        tableView.sortOrder.add(arrivalDate)

        reservationService.subscribe { id, changeType ->
            when (changeType) {
                EntityChangeType.Created -> {
                    tableView.items.add(reservationService.read(id))
                }
                EntityChangeType.Updated -> {
                    val updated = reservationService.read(id)!!
                    tableView.items.removeIf { it.code == updated.code }
                    tableView.items.add(updated)
                }
                EntityChangeType.Removed -> {
                    tableView.items.removeIf { it.code == id }
                }
            }
            tableView.sort()
        }
    }

    private fun fetchReservations(cottageAlias: String? = null) {
        tableView.items.clear()
        tableView.items.addAll(
            if (cottageAlias != null) {
                reservationService.readAll().filter { it.cottage.alias == cottageAlias }
            } else {
                reservationService.readAll()
            }
        )
        tableView.sort()
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<Reservation, Unit?>, TableCell<Reservation, Unit?>> {
            object : TableCell<Reservation, Unit?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.addAll(
                        Button("", Glyph("FontAwesome", FontAwesome.Glyph.EDIT)).also {
                            it.setOnAction {
                                val data: Reservation = tableView.items[index]
                                println("toEdit: $data")

                                val fxmlLoader =
                                    FXMLLoader(HelloApplication::class.java.getResource("reservation-edit-view.fxml"))
                                fxmlLoader.setController(ReservationUpdateController(data.code))
                                stack(fxmlLoader.load())
                            }
                        },
                        Button("", Glyph("FontAwesome", FontAwesome.Glyph.TRASH)).also {
                            it.setOnAction {
                                val data: Reservation = tableView.items[index]
                                println("toDelete: $data")

                                val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                                    title = "Suppression de la réservation"
                                    headerText = "Voulez-vous vraiment supprimer cette réservation ?"
                                }

                                val result = alert.showAndWait()
                                if (result.get() == ButtonType.OK) {
                                    val reservation = reservationService.read(data.code)!!
                                    reservationService.delete(reservation.code)
                                    if (clientService.reservationCount(reservation.client.name) <= 0) {
                                        clientService.delete(reservation.client.name)
                                    }
                                }
                            }
                        }
                    )
                }

                override fun updateItem(item: Unit?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else panel
                }
            }
        }
    }

    @FXML
    fun onExportButtonClick(event: ActionEvent) {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Sélection du format de sortie"
            headerText = "Voulez-vous ajouter les informations sensibles ?"
            contentText = "Ceci inclut le numéro de téléphone et le prix de la réservation"
        }

        val censoredButton = ButtonType("Censurer")
        val showButton = ButtonType("Ajouter")
        alert.buttonTypes.setAll(censoredButton, showButton)

        val alertWindow = alert.dialogPane.scene.window
        alertWindow.setOnCloseRequest { _ -> alert.close() }

        val result = alert.showAndWait()
        val answer = result.getOrNull() ?: return

        val fileChooser = FileChooser().apply {
            title = "Save reservation file"
            initialFileName = "reservation-report"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("PDF", "*.pdf")
            )
        }

        val stage = (event.source as Node).scene.window as Stage
        val file: File? = fileChooser.showSaveDialog(stage)
        println(file)

        if (file != null) {
            val reservations = if (cottageSelectionField.value != ALL_COTTAGES) {
                reservationService.readAll().filter { it.cottage.alias == cottageSelectionField.value }
            } else {
                reservationService.readAll()
            }.toSortedSet { res1, res2 -> res1.arrivalDate.compareTo(res2.arrivalDate) }

            PdfExporter.exportFormattedReservations(file, reservations, censored = answer != showButton)
        }
    }
}