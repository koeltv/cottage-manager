package com.koeltv.cottagemanager.controller

import com.koeltv.cottagemanager.HelloApplication
import com.koeltv.cottagemanager.data.ClientWithStats
import com.koeltv.cottagemanager.db.ClientService
import com.koeltv.cottagemanager.toPlusNote
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.util.Callback
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import org.jetbrains.exposed.dao.EntityChangeType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.util.*

class ClientController : Initializable, KoinComponent, Stackable {
    @FXML
    override lateinit var root: BorderPane

    @FXML
    lateinit var name: TableColumn<ClientWithStats, String>

    @FXML
    lateinit var phoneNumber: TableColumn<ClientWithStats, String?>

    @FXML
    lateinit var nationality: TableColumn<ClientWithStats, String?>

    @FXML
    lateinit var averageNote: TableColumn<ClientWithStats, String>

    @FXML
    lateinit var reservationCount: TableColumn<ClientWithStats, Int>

    @FXML
    lateinit var comments: TableColumn<ClientWithStats, String>

    @FXML
    lateinit var actions: TableColumn<ClientWithStats, Unit>

    @FXML
    lateinit var tableView: TableView<ClientWithStats>

    private val clientService: ClientService by inject()

    @FXML
    private fun onBackButtonClick() = unstack()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        name.cellValueFactory = PropertyValueFactory("name")
        phoneNumber.cellValueFactory = PropertyValueFactory("phoneNumber")
        nationality.cellValueFactory = PropertyValueFactory("nationality")
        averageNote.setCellValueFactory { SimpleStringProperty(it.value.averageNote?.toUByte()?.toPlusNote() ?: "") }
        reservationCount.cellValueFactory = PropertyValueFactory("reservationCount")
//        comments.cellValueFactory = PropertyValueFactory("comments")
        setupActionColumn()

        tableView.items.clear()
        clientService.readAllWithStats().forEach { tableView.items.add(it) }

        tableView.sortOrder.add(name)

        clientService.subscribe { id, changeType ->
            when (changeType) {
                EntityChangeType.Created -> {
                    tableView.items.add(clientService.readWithStats(id))
                }
                EntityChangeType.Updated -> {
                    val updated = clientService.readWithStats(id)!!
                    tableView.items.removeIf { it.name == updated.name }
                    tableView.items.add(updated)
                }
                EntityChangeType.Removed -> {
                    tableView.items.removeIf { it.name == id }
                }
            }
            tableView.sort()
        }
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<ClientWithStats, Unit?>, TableCell<ClientWithStats, Unit?>> {
            object : TableCell<ClientWithStats, Unit?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.add(
                        Button("", Glyph("FontAwesome", FontAwesome.Glyph.EDIT)).also {
                            it.setOnAction {
                                val data = tableView.items[index]
                                println("toEdit: $data")

                                val fxmlLoader =
                                    FXMLLoader(HelloApplication::class.java.getResource("client-edit-view.fxml"))
                                fxmlLoader.setController(ClientUpdateController(data.name))
                                stack(fxmlLoader.load())
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
}