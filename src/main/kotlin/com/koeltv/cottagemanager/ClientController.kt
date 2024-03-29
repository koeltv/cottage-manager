package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.ClientView
import com.koeltv.cottagemanager.data.toView
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
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*
import com.koeltv.cottagemanager.Client.Companion as DatabaseClient

class ClientController: Initializable {
    @FXML
    lateinit var root: BorderPane

    lateinit var name: TableColumn<ClientView, String>
    lateinit var phoneNumber: TableColumn<ClientView, String?>
    lateinit var nationality: TableColumn<ClientView, String?>
    lateinit var averageNote: TableColumn<ClientView, String>
    lateinit var reservationCount: TableColumn<ClientView, Int>
    lateinit var comments: TableColumn<ClientView, String>
    lateinit var actions: TableColumn<ClientView, Void>

    lateinit var tableView: TableView<ClientView>

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
        transaction {
            DatabaseClient.all()
                .map { it.toView() }
                .forEach { tableView.items.add(it) }
        }
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<ClientView, Void?>, TableCell<ClientView, Void?>> {
            object : TableCell<ClientView, Void?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.add(
                        Button("Éditer").also {
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

                override fun updateItem(item: Void?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty) null else panel
                }
            }
        }
    }
}