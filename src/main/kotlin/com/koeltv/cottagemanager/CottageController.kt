package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.db.CottageService
import com.koeltv.cottagemanager.db.CottageView
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


class CottageController : Initializable, KoinComponent {
    @FXML
    lateinit var root: BorderPane

    lateinit var name: TableColumn<CottageView, String>
    lateinit var alias: TableColumn<CottageView, String>
    lateinit var actions: TableColumn<CottageView, Unit>

    @FXML
    lateinit var tableView: TableView<CottageView>

    private val cottageService: CottageService by inject()

    @FXML
    private fun onBackButtonClick() {
        (root.parent as Pane).children.remove(root)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        name.cellValueFactory = PropertyValueFactory("name")
        alias.cellValueFactory = PropertyValueFactory("alias")
        setupActionColumn()

        tableView.items.clear()
        cottageService.readAll().forEach { tableView.items.add(it) }

        tableView.sortOrder.add(name)

        cottageService.subscribe { id, eventType ->
            when (eventType) {
                EntityChangeType.Created -> {
                    tableView.items.add(cottageService.read(id))
                }
                EntityChangeType.Updated -> {
                    val updated = cottageService.read(id)!!
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
        actions.cellFactory = Callback<TableColumn<CottageView, Unit?>, TableCell<CottageView, Unit?>> {
            object : TableCell<CottageView, Unit?>() {
                private val panel = HBox(5.0).apply {
                    alignment = Pos.CENTER
                    children.add(
                        Button("", Glyph("FontAwesome", FontAwesome.Glyph.EDIT)).also {
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

                override fun updateItem(item: Unit?, empty: Boolean) {
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