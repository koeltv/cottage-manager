package com.koeltv.cottagemanager

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*

open class CottageCreateController: Initializable {
    @FXML
    lateinit var root: VBox
    @FXML
    lateinit var nameField: TextField
    @FXML
    lateinit var aliasField: TextField
    @FXML
    lateinit var addButton: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.setOnAction {
            addButton.isDisable = nameField.text.length >= 2
        }
    }

    @FXML
    open fun onConfirmButtonClick() {
        transaction {
            Cottage.new(nameField.text) {
                alias = aliasField.text.ifBlank { nameField.text }
            }
        }

        (root.parent as Pane).children.remove(root)
    }

    @FXML
    fun onCancelButtonClick() {
        (root.parent as Pane).children.remove(root)
    }
}
