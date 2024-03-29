package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.ReservationView
import com.koeltv.cottagemanager.data.toView
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
import javafx.scene.layout.Pane
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.alertSubscribers
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.util.*


class ReservationController : Initializable {
    companion object {
        const val ALL_COTTAGES = "Tous les gîtes"
    }

    @FXML
    lateinit var root: BorderPane

    @FXML
    lateinit var cottageSelectionField: ChoiceBox<String>

    lateinit var arrivalDate: TableColumn<ReservationView, LocalDate>
    lateinit var departureDate: TableColumn<ReservationView, LocalDate>
    lateinit var client: TableColumn<ReservationView, String>
    lateinit var repartition: TableColumn<ReservationView, String>
    lateinit var nationality: TableColumn<ReservationView, String>
    lateinit var price: TableColumn<ReservationView, String>
    lateinit var note: TableColumn<ReservationView, String>
    lateinit var code: TableColumn<ReservationView, String>
    lateinit var comments: TableColumn<ReservationView, String>
    lateinit var actions: TableColumn<ReservationView, Void>

    @FXML
    lateinit var tableView: TableView<ReservationView>

    @FXML
    private fun onBackButtonClick() {
        (root.parent as Pane).children.remove(root)
    }

    @FXML
    private fun onCreationButtonClick() {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("reservation-edit-view.fxml"))
        fxmlLoader.setController(ReservationCreateController())
        (root.parent as Pane).children.add(fxmlLoader.load())
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        arrivalDate.cellValueFactory = PropertyValueFactory("arrivalDate")
        departureDate.cellValueFactory = PropertyValueFactory("departureDate")
        client.cellValueFactory = PropertyValueFactory("client")
        repartition.cellValueFactory = PropertyValueFactory("repartition")
        nationality.cellValueFactory = PropertyValueFactory("nationality")
        price.cellValueFactory = PropertyValueFactory("price")
        note.cellValueFactory = PropertyValueFactory("note")
        code.cellValueFactory = PropertyValueFactory("code")
        comments.cellValueFactory = PropertyValueFactory("comments")
        setupActionColumn()

        cottageSelectionField.items.add(ALL_COTTAGES)
        cottageSelectionField.value = ALL_COTTAGES
        transaction {
            Cottage.all()
                .map { it.toView() }
                .forEach { cottageSelectionField.items.add(it.alias) }
        }
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

        tableView.sortOrder.add(arrivalDate)

        EntityHook.subscribe { change ->
            if (change.entityClass == Reservation) {
                when(change.changeType) {
                    EntityChangeType.Created -> {
                        val newView = Reservation.findById(change.entityId.value as String)!!.toView()
                        tableView.items.add(newView)
                    }
                    EntityChangeType.Updated -> {
                        val updatedView = Reservation.findById(change.entityId.value as String)!!.toView()
                        tableView.items.removeIf { it.code == updatedView.code }
                        tableView.items.add(updatedView)
                    }
                    EntityChangeType.Removed -> {
                        tableView.items.removeIf { it.code == change.entityId.value }
                    }
                }
                tableView.sort()
            }
        }
    }

    private fun fetchReservations(cottageAlias: String? = null) {
        tableView.items.clear()
        transaction {
            val reservations = if (cottageAlias != null) {
                Reservation.all().filter { it.cottage.alias == cottageAlias }
            } else {
                Reservation.all()
            }
            reservations
                .map { it.toView() }
                .sortedBy { it.arrivalDate }
                .forEach { tableView.items.add(it) }
        }
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<ReservationView, Void?>, TableCell<ReservationView, Void?>> {
            object : TableCell<ReservationView, Void?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.addAll(
                        Button("Éditer").also {
                            it.setOnAction {
                                val data: ReservationView = tableView.items[index]
                                println("toEdit: $data")

                                val fxmlLoader =
                                    FXMLLoader(HelloApplication::class.java.getResource("reservation-edit-view.fxml"))
                                fxmlLoader.setController(ReservationUpdateController(data.code))
                                (root.parent as Pane).children.add(fxmlLoader.load())

                            }
                        },
                        Button("Supprimer").also {
                            it.setOnAction {
                                val data: ReservationView = tableView.items[index]
                                println("toDelete: $data")

                                val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                                    title = "Suppression de la réservation"
                                    headerText = "Voulez-vous vraiment supprimer cette réservation ?"
                                }

                                val result = alert.showAndWait()
                                if (result.get() == ButtonType.OK) {
                                    transaction {
                                        val reservation = Reservation.findById(data.code)!!
                                        val client = reservation.client

                                        reservation.delete()

                                        if (Reservation.find { Reservations.client eq client.id }.count() <= 0) {
                                            client.delete()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                override fun updateItem(item: Void?, empty: Boolean) {
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

        val result = alert.showAndWait()
        val censorReservations = result.get() != showButton

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
            transaction {
                PdfExporter.exportFormattedReservations(
                    file,
                    if (cottageSelectionField.value != ALL_COTTAGES) {
                        Reservation.all().filter { it.cottage.alias == cottageSelectionField.value }
                    } else {
                        Reservation.all()
                    }.toSortedSet { res1, res2 -> res1.arrivalDate.compareTo(res2.arrivalDate) },
                    censorReservations
                )
            }
        }
    }
}