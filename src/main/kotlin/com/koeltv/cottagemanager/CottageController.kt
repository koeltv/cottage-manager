package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.CottageView
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
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*


class CottageController : Initializable {
    @FXML
    lateinit var root: BorderPane

    lateinit var name: TableColumn<CottageView, String>
    lateinit var alias: TableColumn<CottageView, String>
    lateinit var actions: TableColumn<CottageView, Void>

    @FXML
    lateinit var tableView: TableView<CottageView>

    @FXML
    private fun onBackButtonClick() {
        (root.parent as Pane).children.remove(root)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        name.cellValueFactory = PropertyValueFactory("name")
        alias.cellValueFactory = PropertyValueFactory("alias")
        setupActionColumn()

        tableView.items.clear()
        transaction {
            Cottage.all()
                .map { it.toView() }
                .forEach { tableView.items.add(it) }
        }

        tableView.sortOrder.add(name)

        EntityHook.subscribe { change ->
            if (change.entityClass == Cottage) {
                when(change.changeType) {
                    EntityChangeType.Created -> {
                        val newView = Cottage.findById(change.entityId.value as String)!!.toView()
                        tableView.items.add(newView)
                    }
                    EntityChangeType.Updated -> {
                        val updatedView = Cottage.findById(change.entityId.value as String)!!.toView()
                        tableView.items.removeIf { it.name == updatedView.name }
                        tableView.items.add(updatedView)
                    }
                    EntityChangeType.Removed -> {
                        tableView.items.removeIf { it.name == change.entityId.value }
                    }
                }
                tableView.sort()
            }
        }
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<CottageView, Void?>, TableCell<CottageView, Void?>> {
            object : TableCell<CottageView, Void?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.add(
                        Button("Éditer").also {
                            it.setOnAction {
                                val data = tableView.items[index]
                                println("toEdit: $data")

                                val fxmlLoader =
                                    FXMLLoader(HelloApplication::class.java.getResource("cottage-edit-view.fxml"))
                                fxmlLoader.setController(CottageUpdateController(data.name))
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

    @FXML
    fun onCreationButtonClick() {
        val fxmlLoader =
            FXMLLoader(HelloApplication::class.java.getResource("cottage-edit-view.fxml"))
        fxmlLoader.setController(CottageCreateController())
        (root.parent as Pane).children.add(fxmlLoader.load())
    }
}