package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.CottageView
import com.koeltv.cottagemanager.data.toView
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.util.Callback
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*


class CottageController : Initializable {
    lateinit var name: TableColumn<CottageView, String>
    lateinit var actions: TableColumn<CottageView, Void>

    @FXML
    lateinit var tableView: TableView<CottageView>

    @FXML
    private fun onBackButtonClick() {
        SceneStack.back()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        name.cellValueFactory = PropertyValueFactory("name")
        setupActionColumn()

        tableView.items.clear()
        transaction {
            Cottage.all()
                .map { it.toView() }
                .forEach { tableView.items.add(it) }
        }
    }

    private fun setupActionColumn() {
        actions.cellFactory = Callback<TableColumn<CottageView, Void?>, TableCell<CottageView, Void?>> {
            object : TableCell<CottageView, Void?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.add(
                        Button("Edit").also {
                            it.setOnAction {
                                val data = tableView.items[index]
                                println("toEdit: $data")
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