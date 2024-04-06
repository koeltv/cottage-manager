package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.db.ClientService
import com.koeltv.cottagemanager.db.ClientWithStatsView
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
import javafx.scene.layout.Pane
import javafx.util.Callback
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import org.jetbrains.exposed.dao.EntityChangeType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.util.*

class ClientController: Initializable, KoinComponent {
    @FXML
    lateinit var root: BorderPane

    lateinit var name: TableColumn<ClientWithStatsView, String>
    lateinit var phoneNumber: TableColumn<ClientWithStatsView, String?>
    lateinit var nationality: TableColumn<ClientWithStatsView, String?>
    lateinit var averageNote: TableColumn<ClientWithStatsView, String>
    lateinit var reservationCount: TableColumn<ClientWithStatsView, Int>
    lateinit var comments: TableColumn<ClientWithStatsView, String>
    lateinit var actions: TableColumn<ClientWithStatsView, Unit>

    lateinit var tableView: TableView<ClientWithStatsView>

    private val clientService : ClientService by inject()

    @FXML
    private fun onBackButtonClick() {
        (root.parent as Pane).children.remove(root)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        name.cellValueFactory = PropertyValueFactory("name")
        phoneNumber.cellValueFactory = PropertyValueFactory("phoneNumber")
        nationality.cellValueFactory = PropertyValueFactory("nationality")
        averageNote.cellValueFactory = PropertyValueFactory("averageNote")
        reservationCount.cellValueFactory = PropertyValueFactory("reservationCount")
        comments.cellValueFactory = PropertyValueFactory("comments")
        setupActionColumn()

        tableView.items.clear()
        clientService.readAllWithStats().forEach { tableView.items.add(it)}

        tableView.sortOrder.add(name)

        clientService.subscribe { id, changeType ->
            when(changeType) {
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
        actions.cellFactory = Callback<TableColumn<ClientWithStatsView, Unit?>, TableCell<ClientWithStatsView, Unit?>> {
            object : TableCell<ClientWithStatsView, Unit?>() {
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
                                (root.parent as Pane).children.add(fxmlLoader.load())
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